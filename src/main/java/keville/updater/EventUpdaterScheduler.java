package keville.updater;

import keville.event.Event;
import keville.event.EventService;
import keville.providers.Providers;

import java.util.LinkedList;
import java.util.List;
import java.time.Duration;


import keville.settings.Settings;


public class EventUpdaterScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventUpdaterScheduler.class);
  private int timeStepMS = 10000; /* every ten seconds */
  private List<Event> outdatedEvents;
  private final int updateBatchSize = 5;
  private final Duration maxAge = Duration.ofDays(10); // This will need to
                                                       // change to a dynamic
                                                       // system.

  /**
  * @param settings
  */
  public EventUpdaterScheduler(Settings settings) {
      this.outdatedEvents = new LinkedList<Event>();
  }

  public void run() {
      
    LOG.debug("starting event update scheduler ...");

    while ( true ) {

      //query for outdated events

      if ( outdatedEvents.size() == 0 ) {
        getOutdatedEventBatch();
      }

      //process one event

      if ( outdatedEvents.size() != 0 ) {
        updateEvent(outdatedEvents.remove(0));
      } else {
        LOG.info("no events to update");
      }

      try {
        Thread.sleep(timeStepMS);
      } catch (Exception e) {
        LOG.error("error trying to sleep thread");
        LOG.error(e.getMessage());
      }

    }

  }

  private void getOutdatedEventBatch() {
    LOG.info("retrieving a batch of outdated events from the database");
    List<Event> batch = EventService.getOudatedEvents(updateBatchSize,maxAge);
    LOG.info("found " + batch.size());
    outdatedEvents.addAll(batch);
  }

  private void updateEvent(Event event) {

    LOG.warn("updating event " + event.id);

    EventUpdater eventUpdater = Providers.getUpdater(event.eventType);
    if ( eventUpdater == null ) {
        LOG.error("unable to get updater for type " + event.eventType);
        LOG.error("aborting update for event : " + event.id);
        return;
    }

    eventUpdater.updateEvent(event);

  }

}
