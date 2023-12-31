package keville.event;

import keville.location.Location;
import java.time.Instant;
import java.io.Serializable;

public class Event implements Serializable {

  public EventTypeEnum eventType;
  public int id;
  public String eventId;
  public String name;
  public String description;
  public Instant start;
  public Instant end;
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
      Instant end,
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
    this.end = end;
    this.location = location;
    this.organizer = organizer;
    this.url = url;
    this.virtual = virtual;
    this.lastUpdate = lastUpdate;
    this.status = status;
  }

  public String toString() {
    return String.format(
        "Name: %s\nDescription: %s\nStart %s\nEnd %s\nVirtual %s\nSource : %s\nUrl %s",
        name, description == null ? "" : description.substring(0, Math.min(description.length(), 60)), 
        start.toString(), end.toString(),
        virtual, eventType.toString(), url);
  }

  public boolean equals(Event e) {

    boolean sameType = true;
    if ( eventType != null && e.eventType != null ) {
      sameType = eventType.equals(e.eventType);
    } else {
      sameType = eventType == null && e.eventType == null;
    }

    boolean sameDescription = true;
    if ( description != null && e.description != null ) {
      sameDescription = description.equals(e.description);
    } else {
      sameDescription = description == null && e.description == null;
    }

    boolean sameName = true;
    if ( name != null && e.name != null ) {
      sameName = name.equals(e.name);
    } else {
      sameName = name == null && e.name == null;
    }

    boolean sameOrganizer = true;
    if ( organizer != null && e.organizer != null ) {
      sameOrganizer = organizer.equals(e.organizer);
    } else {
      sameOrganizer = organizer == null && e.organizer == null;
    }

    boolean sameStart = true;
    if ( start != null && e.start != null ) {
      sameStart = start.equals(e.start);
    } else {
      sameStart = start == null && e.start == null;
    }

    boolean sameEnd = true;
    if ( end != null && e.end != null ) {
      sameEnd = end.equals(e.end);
    } else {
      sameEnd = end == null && e.end == null;
    }

    boolean sameUrl = true;
    if ( url != null && e.url != null ) {
      sameUrl = url.equals(e.url);
    } else {
      sameUrl = url == null && e.url == null;
    }

    // TODO : Location
    
    return sameName 
      && sameType 
      && sameDescription 
      && sameName 
      && sameOrganizer 
      && sameStart
      && sameEnd
      && sameUrl;

  }

}
