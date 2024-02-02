package keville.event;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import keville.scanner.ScannedEventsReport;

public interface EventService {

  public List<Event> getEvents(Predicate<Event> filter);
  public List<Event> getAllEvents();
  public List<Event> getOudatedEvents(int batchSize,Duration maxAcceptableAge);

  /** @return : upate sucess */
  public boolean updateEvent(Event event);

  /** @return : newly created events */
  public ScannedEventsReport processFoundEvents(List<Event> events);

}
