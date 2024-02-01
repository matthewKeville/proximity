package keville.event;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import keville.scanner.ScannedEventsReport;

public interface EventService {

  public Event getEvent(int id);

  /* return a list of Events from the DB */
  public List<Event> getEvents(Predicate<Event> filter);

  /* get all events */
  public List<Event> getAllEvents();

  public List<Event> getOudatedEvents(int batchSize,Duration maxAcceptableAge);

  /* 
   * determine if this event exists in the db 
   * Existence is determined by a unique combination of eventId (domain)
   * and eventType (source). [New events from scanners do not have id]
   */
  public boolean exists(EventTypeEnum type, String eventId);

  /**
   * update an event row in the database, modifiying it's LAST_UPDATE timestamp.
   * @return : update success
  */
  public boolean updateEvent(Event event);

  /**
   * @return : true if event creation succeeds
   */
  public boolean createEvent(Event event);

  /**
   * @return : newly created events
   */
  public ScannedEventsReport processFoundEvents(List<Event> events);

}
