package keville;

import keville.util.GeoUtils;
import keville.settings.Settings;
import keville.Eventbrite.EventCache;
import keville.gson.InstantAdapter;

import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static spark.Spark.*;

public class ProximalDaemon 
{
    static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProximalDaemon.class);
    static Thread scheduleThread;
    static Settings settings;

    static {
      initialize();
    }

    public static void main( String[] args )
    {
        Gson gson = new GsonBuilder()
          .registerTypeAdapter(Instant.class, new InstantAdapter())
          .create();

        // this request should be accompanied with radius/lat/lon otherwise auto inferred 
        // always assuming auto for now ...
        double latitude; 
        double longitude; 
        double radius = 5.0; 
        Map<String,Double> coords = GeoUtils.getClientGeolocation();
        latitude = coords.get("latitude");
        longitude = coords.get("longitude");

        port(4567);

        get("/events", (request, response) -> 
            { 
              LOG.info("recieved GET /events");
              return gson.toJson(
                 EventService.getEvents()
                .stream()
                .filter( e -> e.eventType != EventTypeEnum.DEBUG )
                .filter(Events.WithinKMilesOf(latitude,longitude,radius))
                .filter(Events.InTheFuture())
                .map( e -> Events.CreateClientEvent(e,latitude,longitude) )
                .collect(Collectors.toList())
              );
        });

        LOG.info("spark initialized");

    }

    static void initialize() {

      // in the future, we should check ~/.config/proximity/settings.json and adhere to XDG standard
      String settingsFileString = "./settings.json";
      Path settingsPath = FileSystems.getDefault().getPath(settingsFileString);

      try {

        String jsonString = new String(Files.readAllBytes(settingsPath),StandardCharsets.UTF_8);
        settings = Settings.parseSettings(jsonString);
        LOG.info("Configured from settings file :  "  + settingsPath.toString());

      } catch (Exception e) {

        LOG.error("Error parsing settings : " + settingsPath.toString());
        LOG.error(e.getMessage());
        System.exit(1);

      }

      LOG.info("settings loaded ...");
      LOG.info(settings.toString());

      EventCache.applySettings(settings);
      EventService.applySettings(settings);
      EventScannerScheduler scheduler = new EventScannerScheduler(settings);

      scheduleThread = new Thread(scheduler, "EventScannerScheduler");
      scheduleThread.start();

    }

}
