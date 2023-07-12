package keville;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class App 
{
    static Properties props;
    static VenueLocator venueLocator;
    static EventLocator eventLocator;

    static {
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
      venueLocator = new VenueLocator(props);
      eventLocator = new EventLocator(props);
    }
    
    
    public static String ExtractVenueId(JsonObject event) {
        JsonElement venueIdElement= event.get("venue_id");
        if (venueIdElement.isJsonNull()) {
          return "";
        }
        return venueIdElement.getAsString();
    }
    
    public static LocalDateTime ISOInstantToLocalDateTime(String instantString) {
      //https://stackoverflow.com/questions/32826077/parsing-iso-instant-and-similar-date-time-strings
      DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
      Instant instant = Instant.from(dtf.parse(instantString));
      //2011-12-03T10:15:30Z
      LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
      return localDateTime;
    }

    public static Predicate<JsonObject> CityFilter(String city) {
      return new Predicate<JsonObject>() {
        public boolean test(JsonObject event) {
          String venueId = ExtractVenueId(event);
          if (venueId.equals("")) {
            return false;
          } else {
            JsonObject venue = venueLocator.locateVenue(venueId);
            JsonObject address = venue.getAsJsonObject("address");
            return city.equals(address.get("city").getAsString());
          }
        }
      };
    }

    public static Predicate<JsonObject> CitiesFilter(List<String> cities) {
      return new Predicate<JsonObject>() {
        public boolean test(JsonObject event) {
          String venueId = ExtractVenueId(event);
          if (venueId.equals("")) {
            return false;
          } else {
            JsonObject venue = venueLocator.locateVenue(venueId);
            JsonObject address = venue.getAsJsonObject("address");
            return cities.stream()
              .anyMatch(c -> c.equals(address.get("city").getAsString()));
          }
        }
      };
    }

    public static Predicate<JsonObject> DateRangeFilter(LocalDateTime start,LocalDateTime end) {
      return new Predicate<JsonObject>() {
        public boolean test(JsonObject event) {
          JsonObject eventStart = event.getAsJsonObject("start");
          LocalDateTime localDateTime = ISOInstantToLocalDateTime(eventStart.get("utc").getAsString());
          return localDateTime.isBefore(end) && localDateTime.isAfter(start);
        }
      };
    }

    public static Predicate<JsonObject> WithinDaysFromNow(int days) {
      LocalDateTime now = LocalDateTime.now();
      return DateRangeFilter(now,now.plusDays(days));
    }

    public static void printEventInfo(List<JsonObject> events) {
      for ( JsonObject jo : events ) { 

        JsonObject eventName = jo.getAsJsonObject("name");
        JsonObject eventStart = jo.getAsJsonObject("start");
        JsonObject eventDescription = jo.getAsJsonObject("description");

        //String venueId = ExtractVenueId(jo,venueLocator);
        String venueId = ExtractVenueId(jo);

        LocalDateTime localDateTime = ISOInstantToLocalDateTime(eventStart.get("utc").getAsString());
        String dateInfo = String.format("%s/%s",localDateTime.getMonth(),localDateTime.getDayOfMonth());

        System.out.println(String.format("Start: %s",dateInfo));
        System.out.println(String.format("Name: %s",eventName.get("text").getAsString()));

        if (!venueId.isEmpty()) {
          JsonObject venue = venueLocator.locateVenue(venueId);
          JsonObject address = venue.getAsJsonObject("address");
          System.out.println(String.format("City: %s",address.get("city")));
          System.out.println(String.format("Region: %s",address.get("region")));
          System.out.println(String.format("Venue Name: %s",venue.get("name").getAsString()));
        } else {
          System.out.println("Venue: no venue information");
        }

        System.out.println(String.format("%s",eventDescription.get("text").getAsString()));
        System.out.println(String.format("%s",jo.get("url").getAsString()));
        System.out.println();
      }
    }

    public static void main( String[] args )
    {
        //Scan for new events

        //When I google city lat/lon pairs google show somethings like 40.2204° N, 74.0121° W
        //but this translates to (40.2204,-74.0121,5.0) , i.e. W means negative
        //EventScanner eventScanner = new EventScanner(39.9710,-75.1285,5.0); //fishtown
        //EventScanner eventScanner = new EventScanner(40.2204,-74.0121,20.0); //asbury park
        //List<String> newEventIds = eventScanner.scan(15);
        //newEventIds.stream()
        //  .forEach(e -> eventLocator.locateEvent(e));
        


        List<JsonObject> allEvents = eventLocator.getAllKnownEvents();

        Predicate<JsonObject> philly = CitiesFilter(Arrays.asList("Philadelphia"));
        Predicate<JsonObject> cityFilters = CitiesFilter(Arrays.asList("Asbury Park","Philadelphia"));
        Predicate<JsonObject> localCityFilters = CitiesFilter(Arrays.asList("Asbury Park","Belmar","Ocean Grove","Neptune","Lake Como"));
        Predicate<JsonObject> in10Days = WithinDaysFromNow(10);
        
        //Filter events
        List<JsonObject> events = allEvents.stream().
          //filter(cityFilters).
          //filter(localCityFilters).
          filter(philly).
          //filter(in10Days).
          filter(WithinDaysFromNow(2)).
          collect(Collectors.toList());
        

        //Display events
        printEventInfo(events);

        // save venues & events to cold storage
        eventLocator.notifyTermination();
        venueLocator.notifyTermination();

    }

}
