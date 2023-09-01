package keville;

import keville.util.AnsiColors;
import keville.util.GeoUtils;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;


public class Event implements Serializable {
  public EventTypeEnum eventType; 
  public int id;
  public String eventId;
  public String name;
  public String description;
  //public LocalDateTime start;
  public Instant start;
  public double longitude;
  public double latitude;
  //these are not consistent across the world and leaving them empty/null feels
  //lazy. This is temporary.
  public String city;
  public String state;
  public String url;

  public Event() {};
  public Event(
      int id,      //pk in db
      String eventId, //from source location 
      EventTypeEnum eventType,
      String name,
      String description,
      Instant start,
      double longitude,
      double latitude,
      String city,
      String state,
      String url
      ) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.name = name;
        this.description = description;
        this.start = start;
        this.longitude = longitude;
        this.latitude = latitude;
        this.city = city;
        this.state = state;
        this.url = url;
      }

  /* for events not in db yet */
  public Event(
      String eventId, //from source location 
      EventTypeEnum eventType,
      String name,
      String description,
      Instant start,
      double longitude,
      double latitude,
      String city,
      String state,
      String url
      ) {
         this(-1,eventId,eventType,name,description,start,longitude,latitude,city,state,url);
      }

  public String toString() {
    return String.format("Name: %s\nDescription: %s\nStart %s\nLocale : %s,%s\nSource : %s\nLat , Lon: %4.2f,%4.2f\nUrl %s",
        name,description == null ? "" : description.substring(0,Math.min(description.length(),60)),start.toString(),city,state,eventTypeString(eventType),latitude,longitude,url);
  }

  public String toColorString() {
    return String.format("Name: %s\nDescription: %s\nStart %s\nLocale : %s,%s\nSource : %s\nLat , Lon: %s,%s\nUrl %s",
        AnsiColors.colorString(name,AnsiColors.RED),
        AnsiColors.colorString(description == null ? "" : description.substring(0,Math.min(description.length(),60)) ,AnsiColors.GREEN),
        AnsiColors.colorString(start.toString(),AnsiColors.YELLOW),
        AnsiColors.colorString(city,AnsiColors.BLUE),
        AnsiColors.colorString(state,AnsiColors.BLUE),
        AnsiColors.colorString(eventTypeString(eventType),AnsiColors.PURPLE),
        AnsiColors.colorString(((Double) latitude).toString(),AnsiColors.CYAN),
        AnsiColors.colorString(((Double) longitude).toString(),AnsiColors.CYAN),
        AnsiColors.colorString(url,AnsiColors.WHITE));

  }



  public static String eventTypeString(EventTypeEnum type) {
    switch (type) {
      case EVENTBRITE:
            return "Eventbrite.com";
      case MEETUP:
            return "meetup.com";
      default:
            return "unknown";
    }
  }

  // location filters
 
  public static Predicate<Event> CityFilter(String city) {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        return city.equals(event.city);
      }
    };
  }

  public static Predicate<Event> CitiesFilter(List<String> cities) {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        return cities.stream()
          .anyMatch(c -> c.equals(event.city));
      }
    };
  }

  public static Predicate<Event> WithinKMilesOf(double lat,double lon,double miles) {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        return GeoUtils.isWithinMilesOf(miles,lat,lon,event.latitude,event.longitude);
      }
    };
  }

  //temporal filters
  
  public static Predicate<Event> DateRangeFilter(ZonedDateTime start,ZonedDateTime end) {
    return new Predicate<Event>() {
      //in the future if event end is Event , then this should use start & end for evaluation
      public boolean test(Event event) {
        return event.start.isBefore(end.toInstant()) && event.start.isAfter(start.toInstant());
      }
    };
  }

  public static Predicate<Event> WithinDaysFromNow(int days) {
    //LocalDateTime now = LocalDateTime.now();
    ZonedDateTime now = ZonedDateTime.now();
    return DateRangeFilter(now,now.plusDays(days));
  }


}
