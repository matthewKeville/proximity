package keville.event;

public class EventMerger {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventMerger.class);
  
    //identify if the newEvent is an updated version of the oldEvent,
    //if the events are the same, return null
    public static Event merge(Event oldEvent,Event newEvent) {

        switch (oldEvent.eventType) {
          case DEBUG:
            return newEvent;
          case EVENTBRITE: //fall TODO :
          case ALLEVENTS: //fall TODO :
          case MEETUP: //fall TODO :
          default:
            //equate meta fields (db only)
            newEvent.id = oldEvent.id;
            newEvent.lastUpdate = oldEvent.lastUpdate;
            newEvent.status = oldEvent.status;
            //discriminate
            return ( newEvent.equals(oldEvent) ) ? null : newEvent;
        }

    }

}
