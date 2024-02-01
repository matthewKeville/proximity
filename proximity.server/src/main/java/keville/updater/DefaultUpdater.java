package keville.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import keville.event.Event;
import keville.event.EventStatusEnum;

@Component
public class DefaultUpdater implements EventUpdater {

    private static Logger LOG = LoggerFactory.getLogger(DefaultUpdater.class);
    
    public DefaultUpdater() {
        LOG.warn("Default Updater is being used, this for debugging only");
    }

    public Event updateEvent(Event event) {
        event.status = EventStatusEnum.HEALTHY;
        return event;
    }

}
