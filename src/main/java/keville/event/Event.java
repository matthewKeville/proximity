package keville.event;

import keville.location.Location;
import keville.util.AnsiColors;
import java.time.Instant;
import java.io.Serializable;

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
  public Instant lastUpdate;
  public EventStatusEnum status;

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
      boolean virtual,
      Instant lastUpdate,
      EventStatusEnum status) {
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
    this.lastUpdate = lastUpdate;
    this.status = status;
  }

  public String toString() {
    return String.format(
        "Name: %s\nDescription: %s\nStart %s\nVirtual %s\nSource : %s\nUrl %s",
        name, description == null ? "" : description.substring(0, Math.min(description.length(), 60)), start.toString(),
        virtual, eventTypeString(eventType), url);
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

  public boolean equals(Event e) {

    return e.eventId.equals(eventId) 
        && e.eventType.equals(eventType)
        && e.name.equals(name)
        && e.description.equals(description)
        && e.start.equals(start)
        //&& e.organizer ( can be null? in eventbrite )
        //&& e.location TODO :  (need to implement equality for Location ..)
        && e.url.equals(url);
  }

}
