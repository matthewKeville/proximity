package keville.providers.Eventbrite;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name="EVENTBRITE_EVENT")
public class Event {

  @Id
  public Integer id;

  public String eventId;
  public String json;

  public Event(){}
  public Event(String eventId,String json){
    this.eventId = eventId;
    this.json = json;
  }
}
