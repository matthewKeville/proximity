package keville.AllEvents;

import keville.Event;
import keville.EventService;
import keville.EventStatusEnum;
import keville.EventUpdater;

public class AllEventsUpdater implements EventUpdater {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsUpdater.class);

    public boolean updateEvent(Event event) {

        //do stuff to get updated event
        event.status = EventStatusEnum.QUARENTINE;

        return  EventService.updateEvent(event);

    }
}
