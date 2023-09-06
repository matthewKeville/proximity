package keville;

import keville.Eventbrite.EventbriteScanner;
import keville.meetup.MeetupScanner;
import keville.util.GeoUtils;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.List;


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

        Map<String,Double> currentLocation = GeoUtils.getClientGeolocation(); 
        
        //Assemble a test filter
        Predicate<Event> eventFilter;
        eventFilter = Event.WithinDaysFromNow(2);
        eventFilter = eventFilter.and(
            Event.WithinKMilesOf(
              currentLocation.get("latitude"),
              currentLocation.get("longitude"),
            10.0));

        int port = 9876;
        boolean run = true;
        try {

          ServerSocket server = new ServerSocket(port);
          while (run) {

            //process one socket request at a time

            Socket socket = server.accept();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            //what is the client requesting?
            String request = (String) ois.readObject();

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            if (request.equals("List")) {

              String filterString = (String) ois.readObject();
              LOG.info("Recieved List Request with filter string : " + filterString);

              //Load known events into memory
              List<Event> events = eventService.getEvents();

              oos.writeObject("Okay");
              oos.writeObject(events);

            } else {
              oos.writeObject("Unknown");
            }

            ois.close();
            oos.close();
            socket.close();

          }
          server.close();

        } catch (IOException | ClassNotFoundException e) {
          LOG.error("Server error encountered");
          LOG.error(e.getMessage());
        }

    }

    static void initialize() {

      //load configuration w/ respect to custum.properties
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
