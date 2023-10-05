package keville.providers.Eventbrite;

import keville.event.Event;
import keville.event.EventService;
import keville.event.EventStatusEnum;
import keville.updater.EventUpdater;

public class EventbriteUpdater implements EventUpdater {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventbriteUpdater.class);

    public boolean updateEvent(Event event) {

        //do stuff to get updated event
        event.status = EventStatusEnum.QUARENTINE;

        //reflect changes in db
        return EventService.updateEvent(event);
    }
}
