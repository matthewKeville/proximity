package keville;

import keville.Eventbrite.EventCache;
import keville.gson.InstantAdapter;

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

        //spark defaults port to : 4567 
        
        get("/events", (request, response) -> 
            { 
              LOG.info("recieved GET /events");
              return gson.toJson(
                 EventService.getEvents()
                .stream()
                .filter( e -> e.eventType != EventTypeEnum.DEBUG )
                .filter(Events.WithinKMilesOf(settings.latitude,settings.longitude,settings.radius))
                .filter(Events.InTheFuture())
                .map( e -> Events.CreateClientEvent(e,settings.latitude,settings.longitude) )
                .collect(Collectors.toList())
              );
            });

    }

    static void initialize() {

      String settingsFileString = "./custom.json";
      Path jobFilePath = FileSystems.getDefault().getPath(settingsFileString);

      try {
        String jsonString = new String(Files.readAllBytes(jobFilePath),StandardCharsets.UTF_8);
        settings = Settings.parseSettings(jsonString);
      } catch (Exception e) {
        LOG.error("unable to parse settings : " + jobFilePath.toString());
        LOG.error(e.getMessage());
        System.exit(1);
      }

      EventCache.applySettings(settings);
      EventService.applySettings(settings);

      EventScannerScheduler scheduler = new EventScannerScheduler(settings);

      scheduleThread = new Thread(scheduler, "EventScannerScheduler");
      scheduleThread.start();

    }

}
