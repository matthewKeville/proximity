package keville;

import java.util.List;

public interface EventLocator {
  public List<Event> getAllKnownEvents();
  public Event locateEvent(String eventId);
}


