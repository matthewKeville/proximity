package keville;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;


public class Event implements Serializable {
  public EventTypeEnum eventType; 
  public String eventId;
  public String name;
  public String description;
  public LocalDateTime start;
  public double longitude;
  public double latitude;
  //these are not consistent across the world and leaving them empty/null feels
  //lazy. This is temporary.
  public String city;
  public String state;
  public String url;

  public Event() {};
  public Event(EventTypeEnum eventType,
      String eventId,
      String name,
      String description,
      LocalDateTime start,
      double longitude,
      double latitude,
      String city,
      String state,
      String url
      ) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.start = start;
        this.longitude = longitude;
        this.latitude = latitude;
        this.city = city;
        this.state = state;
        this.url = url;
      }

  public String toString() {
    return String.format("Name: %s\nDescription: %s\nStart %s\nLocale : %s,%s\nLat , Lon: %4.2f,%4.2f\nUrl %s",
        name,description.substring(0,Math.min(description.length(),60)),start.toString(),city,state,latitude,longitude,url);
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

  //temporal filters
  
  public static Predicate<Event> DateRangeFilter(LocalDateTime start,LocalDateTime end) {
    return new Predicate<Event>() {
      //in the future if event end is Event , then this should use start & end for evaluation
      public boolean test(Event event) {
        return event.start.isBefore(end) && event.start.isAfter(start);
      }
    };
  }

  public static Predicate<Event> WithinDaysFromNow(int days) {
    LocalDateTime now = LocalDateTime.now();
    return DateRangeFilter(now,now.plusDays(days));
  }


}
