package keville.updater;

import keville.event.Event;
import keville.event.EventService;
import keville.event.EventStatusEnum;

public class EventUpdaterService {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventUpdaterService.class);

  //  FIXME : when events are outdated, they currently get quarentined, this should only happen
  //  when we fail to update an event. This mechanism is supposed avoid excessive network requests
  //  on third party resources

  //retrieve updated Event object from implementation specific update protocols
  public static Event updateEvent(Event event) {

    LOG.warn("updating event " + event.id);

    //retrieve the updated event object generated from its appropriate protocol.
    switch (event.eventType) {

      case DEBUG:
        event.status  = EventStatusEnum.HEALTHY;
        break;

      case EVENTBRITE:
        //TODO : eventbrite specific update protocol
        event.status  = EventStatusEnum.QUARENTINE;
        LOG.warn("eventbrite update protocol not implemented, quarenting event id : " + event.id);
        break;
      case ALLEVENTS:
        //TODO : allevents specific update protocol
        event.status  = EventStatusEnum.QUARENTINE;
        LOG.warn("allevents update protocol not implemented, quarenting event id : " + event.id);
        break;
      case MEETUP:
        //TODO : meetup specific update protocol
        event.status  = EventStatusEnum.QUARENTINE;
        LOG.warn("meetup update protocol not implemented, quarenting event id : " + event.id);
        break;

    }

    if (!EventService.updateEvent(event) ) {
      LOG.warn("event : " + event.id + " didn't update");
    }

    return event;

  }

}
