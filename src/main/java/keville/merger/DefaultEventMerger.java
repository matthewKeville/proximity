package keville.merger;

import keville.event.Event;

public class DefaultEventMerger implements EventMerger {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultEventMerger.class);
  
    public Event merge(Event oldEvent,Event newEvent) {

        newEvent.id = oldEvent.id;
        newEvent.lastUpdate = oldEvent.lastUpdate;
        newEvent.status = oldEvent.status;
        return ( newEvent.equals(oldEvent) ) ? null : newEvent;

    }

}
