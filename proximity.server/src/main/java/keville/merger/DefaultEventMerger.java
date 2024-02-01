package keville.merger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import keville.event.Event;

@Component
public class DefaultEventMerger implements EventMerger {

    private static Logger LOG = LoggerFactory.getLogger(DefaultEventMerger.class);
  
    public Event merge(Event oldEvent,Event newEvent) {
      newEvent.id = oldEvent.id;
      newEvent.lastUpdate = oldEvent.lastUpdate;
      newEvent.status = oldEvent.status;
      return ( newEvent.equals(oldEvent) ) ? null : newEvent;
    }

}
