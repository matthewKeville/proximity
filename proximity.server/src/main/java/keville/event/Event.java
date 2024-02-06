package keville.event;

import keville.location.Location;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Table
public class Event implements Serializable {

  @Id
  public Integer id;
  public String eventId;
  @Column(value = "SOURCE")
  public EventTypeEnum eventType;
  public String name;
  public String description;
  @Column(value = "START_TIME")
  public LocalDateTime start;
  @Column(value = "END_TIME")
  public LocalDateTime end;
  // allow Location columns to have null values
  @Embedded.Empty
  public Location location;
  public String organizer;
  public String url;
  public boolean virtual;
  @Column(value = "LAST_UPDATE")
  @LastModifiedDate
  public LocalDateTime lastUpdate;
  public EventStatusEnum status;

  public Event(){}

  public Event(
      String eventId, // from source location
      EventTypeEnum eventType,
      String name,
      String description,
      LocalDateTime start,
      LocalDateTime end,
      Location location,
      String organizer,
      String url,
      boolean virtual,
      EventStatusEnum status) {
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

    // TODO : Factor Location into event equivalence
    
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
