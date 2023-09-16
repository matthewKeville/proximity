package keville;

import java.time.Instant;
import keville.gson.InstantAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static spark.Spark.*;

public class ProximalDaemon 
{
    static Properties props;
    static EventService eventService;
    static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProximalDaemon.class);
    static Thread scheduleThread;

    static {
      initialize();
    }

    public static void main( String[] args )
    {
        List<Event> events = eventService.getEvents();
        Gson gson = new GsonBuilder()
          .registerTypeAdapter(Instant.class, new InstantAdapter())
          .create();

        //spark defaults port to : 4567 
        get("/events", (req, res) -> gson.toJson(events));
        get("/event", (req, res) -> gson.toJson(events.get(0)));

    }


    static void initialize() {

      //load configuration w/ respect to custom.properties
      props = new Properties();
      try {
        File customProperties = new File("./custom.properties");
        if (customProperties.exists()) {
          LOG.info("custom properties found");
          props.load(new FileInputStream("./custom.properties"));
        } else {
          LOG.info("no custom properties located, using defaults");
          props.load(new FileInputStream("./default.properties"));
        }
      } catch (Exception e) {
        LOG.error("Unable to load app.properties configuration\naborting");
        LOG.error(e.getMessage());
        System.exit(1);
      }

      //minimal configuration met?
      if (props.getProperty("event_brite_api_key").isEmpty()) {
        LOG.info("You must provide an event_brite_api_key");
        LOG.error("no event_brite_api_key found");
        System.exit(2);
      }
      LOG.info("using api_key : "+props.getProperty("event_brite_api_key"));

      eventService = new EventService(props);

      EventScannerScheduler scheduler = new EventScannerScheduler(eventService, props);
      scheduleThread = new Thread(scheduler, "EventScannerScheduler");
      scheduleThread.start();

    }

}
