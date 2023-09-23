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

    static double DEFAULT_LAT; 
    static double DEFAULT_LON; 
    static final double DEFAULT_RAD = 5.0; 

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
        Map<String,Double> coords = GeoUtils.getClientGeolocation();

        port(4567);

        get("/events", (request, response) -> 
            { 

              LOG.info("recieved GET /events");

              //  this query parameter extraction feels awkward at best
              //  It is the way it is because java lambdas require final or effectively final
              //  variables (not class fields)

              Double radius;
              if (request.queryMap().hasKey("radius")) {
                Double queryRadius = request.queryMap().get("radius").doubleValue();
                if ( queryRadius != null ) {
                  radius = queryRadius;
                } else {
                  radius = DEFAULT_RAD; 
                }
              } else {
                radius = DEFAULT_RAD; 
              }

              Double latitude;
              if (request.queryMap().hasKey("latitude")) {
                Double queryLatitude = request.queryMap().get("latitude").doubleValue();
                if ( queryLatitude != null ) {
                  latitude = queryLatitude;
                } else {
                  latitude = DEFAULT_LAT; 
                }
              } else {
                latitude = DEFAULT_LAT; 
              }

              Double longitude;
              if (request.queryMap().hasKey("longitude")) {
                Double queryLongitude = request.queryMap().get("longitude").doubleValue();
                if ( queryLongitude != null ) {
                  longitude = queryLongitude;
                } else {
                  longitude = DEFAULT_LON; 
                }
              } else {
                longitude = DEFAULT_LON; 
              }

              Integer daysBefore;
              if (request.queryMap().hasKey("daysBefore")) {
                Integer queryDaysBefore = request.queryMap().get("daysBefore").integerValue();
                if ( queryDaysBefore != null ) {
                  daysBefore = queryDaysBefore;
                } else {
                  daysBefore = Integer.MAX_VALUE;
                }
              } else {
                daysBefore = Integer.MAX_VALUE;
              }

              boolean showVirtual = request.queryMap().hasKey("virtual") && request.queryMap().get("virtual").booleanValue();

              LOG.info("processing request with parameters : radius = " + radius + " latitude  = " + latitude +  " longitude = " + longitude + " dayBefore " + daysBefore + " showVirtual " + showVirtual);

              return gson.toJson(
                 EventService.getEvents()
                .stream()
                .filter( e -> e.eventType != EventTypeEnum.DEBUG )
                .filter(e -> showVirtual || !e.virtual)
                .filter(Events.WithinKMilesOf(latitude,longitude,radius))
                .filter(Events.InTheFuture())
                .map( e -> Events.CreateClientEvent(e,latitude,longitude) )
                .filter(Events.BeforeDays(daysBefore))
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

      Map<String,Double> coords = GeoUtils.getClientGeolocation();
      DEFAULT_LAT = coords.get("latitude"); 
      DEFAULT_LON = coords.get("longitude");

      LOG.info("settings loaded ...");
      LOG.info(settings.toString());

      EventCache.applySettings(settings);
      EventService.applySettings(settings);
      EventScannerScheduler scheduler = new EventScannerScheduler(settings);

      scheduleThread = new Thread(scheduler, "EventScannerScheduler");
      scheduleThread.start();

    }

}
