package keville.event;

import keville.location.Location;

import java.time.LocalDateTime;

public class EventBuilder {

  private EventTypeEnum eventType; 
  private String eventId;
  private String name;
  private String description;
  private LocalDateTime start;
  private LocalDateTime end;
  private Location location;
  private String organizer;
  private String url;
  private boolean virtual;
  private EventStatusEnum status;

  public EventBuilder() {}

  public void setEventTypeEnum(EventTypeEnum eventType) {
    this.eventType = eventType;
  } 

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setStart(LocalDateTime start) {
    this.start = start;
  }

  public void setEnd(LocalDateTime end) {
    this.end = end;
  }

  public void setLocation(Location location) {
    this.location = location;
  }


  public void setUrl(String url){
    this.url = url;
  }

  public void setOrganizer(String organizer){
    this.organizer = organizer;
  }

  public void setVirtual(boolean virtual) {
    this.virtual = virtual;
  }

  public void setStatus(EventStatusEnum status) {
    this.status = status;
  }

  public Event build() {
    Event event = new Event(
      eventId,
      eventType,
      name,
      description,
      start,
      end,
      location,
      organizer,
      url,
      virtual,
      status
    );
    return event;
  }

}


