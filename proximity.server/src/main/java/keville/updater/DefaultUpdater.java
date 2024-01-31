package keville.updater;

import keville.event.Event;
import keville.event.EventStatusEnum;

public class DefaultUpdater implements EventUpdater {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultUpdater.class);
    
    public DefaultUpdater() {
        LOG.warn("Default Updater is being used, this for debugging only");
    }

    public Event updateEvent(Event event) {

        event.status = EventStatusEnum.HEALTHY;

        return event;
    }

}
