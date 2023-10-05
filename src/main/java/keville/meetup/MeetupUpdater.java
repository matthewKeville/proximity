package keville.meetup;

import keville.Event;
import keville.EventService;
import keville.EventStatusEnum;
import keville.EventUpdater;

public class MeetupUpdater implements EventUpdater {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupUpdater.class);

    public boolean updateEvent(Event event) {

        //do stuff to get updated event
        event.status = EventStatusEnum.QUARENTINE;

        //reflect changes in db
        return EventService.updateEvent(event);
    }
}
