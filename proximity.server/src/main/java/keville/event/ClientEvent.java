package keville.event;

import keville.location.Location;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ClientEvent {

  public double distance;
  public int daysFromNow;
  public int hoursFromNow;

  public String eventId;
  public EventTypeEnum eventType;
  public String name;
  public String description;
  public Instant start;
  public Instant end;
  public Location location;
  public String organizer;
  public String url;
  public boolean virtual;
  public EventStatusEnum status;

  public ClientEvent(Event event,double distance, int daysFromNow, int hoursFromNow) {
    this.eventId = event.eventId;
    this.eventType = event.eventType;
    this.name = event.name;
    this.description = event.description;
    if ( event.start != null ) {
      this.start = ZonedDateTime.of(event.start, ZoneOffset.UTC).toInstant();
    }
    if ( event.end != null ) {
      this.end = ZonedDateTime.of(event.end, ZoneOffset.UTC).toInstant();
    }
    this.location = event.location;
    this.organizer = event.organizer;
    this.url = event.url;
    this.virtual = event.virtual;
    this.status = event.status;
    this.distance = distance;
    this.daysFromNow = daysFromNow;
    this.hoursFromNow = hoursFromNow;
  }

}


