package keville.event;

public class ClientEvent extends Event {
  public double distance;
  public int daysFromNow;
  public int hoursFromNow;

  public ClientEvent(Event event,double distance, int daysFromNow, int hoursFromNow) {
    super(
      event.id, // pk in db
      event.eventId, // from source location
      event.eventType,
      event.name,
      event.description,
      event.start,
      event.end,
      event.location,
      event.organizer,
      event.url,
      event.virtual,
      event.lastUpdate,
      event.status
    );
    this.distance = distance;
    this.daysFromNow = daysFromNow;
    this.hoursFromNow = hoursFromNow;

  }

}


