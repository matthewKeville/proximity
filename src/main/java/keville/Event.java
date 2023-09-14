package keville;

import keville.util.AnsiColors;
import keville.util.GeoUtils;
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
  public Instant start;
  public Location location;
  public String url;
  public String organizer;
  public boolean virtual;

  public Event(
      int id, // pk in db
      String eventId, // from source location
      EventTypeEnum eventType,
      String name,
      String description,
      Instant start,
      Location location,
      String organizer,
      String url,
      boolean virtual) {
    this.id = id;
    this.eventId = eventId;
    this.eventType = eventType;
    this.name = name;
    this.description = description;
    this.start = start;
    this.location = location;
    this.organizer = organizer;
    this.url = url;
    this.virtual = virtual;
  }

  public String toString() {
    return String.format(
        "Name: %s\nDescription: %s\nStart %s\nVirtual %s\nSource : %s\nUrl %s",
        name, description == null ? "" : description.substring(0, Math.min(description.length(), 60)), start.toString(),
        virtual,eventTypeString(eventType),url);
  }

  public String toColorString() {
    return String.format(
        "Name: %s\nDescription: %s\nStart %s\nVirtual %s\nSource : %s\nUrl %s",
        AnsiColors.colorString(name, AnsiColors.RED),
        AnsiColors.colorString(description == null ? "" : description.substring(0, Math.min(description.length(), 60)),
            AnsiColors.GREEN),
        AnsiColors.colorString(start.toString(), AnsiColors.YELLOW),
        AnsiColors.colorString(Boolean.toString(virtual), AnsiColors.PURPLE_BRIGHT),
        AnsiColors.colorString(eventTypeString(eventType), AnsiColors.PURPLE),
        AnsiColors.colorString(url, AnsiColors.WHITE));

  }

  public static String eventTypeString(EventTypeEnum type) {
    switch (type) {
      case EVENTBRITE:
        return "Eventbrite.com";
      case MEETUP:
        return "meetup.com";
      case ALLEVENTS:
        return "allevents.in";
      default:
        return "unknown";
    }
  }

  // location filters
  /*
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

  public static Predicate<Event> WithinKMilesOf(double lat, double lon, double miles) {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        return GeoUtils.isWithinMilesOf(miles, lat, lon, event.latitude, event.longitude);
      }
    };
  }

  // temporal filters

  public static Predicate<Event> DateRangeFilter(ZonedDateTime start, ZonedDateTime end) {
    return new Predicate<Event>() {
      // in the future if event end is Event , then this should use start & end for
      // evaluation
      public boolean test(Event event) {
        return event.start.isBefore(end.toInstant()) && event.start.isAfter(start.toInstant());
      }
    };
  }

  public static Predicate<Event> WithinDaysFromNow(int days) {
    // LocalDateTime now = LocalDateTime.now();
    ZonedDateTime now = ZonedDateTime.now();
    return DateRangeFilter(now, now.plusDays(days));
  }
  */

}
