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
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;


public class ProximalDaemon 
{
    static Properties props;
    //static EventCache eventCache;
    static EventService eventService;

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

        //Load known events into memory
        List<Event> events = eventService.getEvents();
        //List<Event> allEvents = eventService.getEvents(eventFilter);

        //scan events optional
        EventScanner EventbriteScanner = new EventbriteScanner(40.2204,-74.0121,20.0,eventService,props); //asbury
        EventbriteScanner.scan();
        //EventScanner meetupScanner = new MeetupScanner("Belmar","nj",eventCache); //asbury park

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
              System.out.println("request :" + request );
              System.out.println("filterString :" + filterString );

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
          System.out.println("the server encountered an error");
          System.out.println(e.getMessage());
        }

    }

    static void initialize() {

      //load configuration w/ respect to custum.properties
      props = new Properties();
      try {
        File customProperties = new File("./custom.properties");
        if (customProperties.exists()) {
          System.out.println("found custom properties");
          props.load(new FileInputStream("./custom.properties"));
        } else {
          System.out.println("default configuration");
          props.load(new FileInputStream("./default.properties"));
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println("Unable to load app.properties configuration\naborting");
        System.exit(1);
      }

      //minimal configuration met?
      if (props.getProperty("event_brite_api_key").isEmpty()) {
        System.err.println("You must provide an event_brite_api_key");
        System.exit(2);
      }
      System.out.println("using api_key : "+props.getProperty("event_brite_api_key"));

      eventService = new EventService(props);
    }

}
