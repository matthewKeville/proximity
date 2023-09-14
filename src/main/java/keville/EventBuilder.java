package keville;

import java.time.Instant;

public class EventBuilder {

  private int id;

  private EventTypeEnum eventType; 
  private String eventId;
  private String name;
  private String description;
  private Instant start;
  private Location location;
  private boolean virtual;
  private String url;

  public EventBuilder() {
    id = -1;
  }
  
  public void setId(int id) {
    this.id = id;
  }

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

  public void setStart(Instant start) {
    this.start = start;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setVirtual(boolean virtual) {
    this.virtual = virtual;
  }

  public void setUrl(String url){
    this.url = url;
  }

  public Event build() {
    Event event = new Event(
      id,
      eventId,
      eventType,
      name,
      description,
      start,
      location,
      url,
      virtual
    );
    return event;
  }

}


