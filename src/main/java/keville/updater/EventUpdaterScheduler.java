package keville.updater;

import keville.event.Event;
import keville.merger.EventMerger;
import keville.event.EventService;
import keville.event.EventStatusEnum;
import keville.providers.Providers;

import java.util.LinkedList;
import java.util.List;
import java.time.Duration;


import keville.settings.Settings;


public class EventUpdaterScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventUpdaterScheduler.class);
  private int timeStepMS = 60_000; /* every minute */
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
    EventMerger eventMerger = Providers.getMerger(event.eventType);

    if ( eventUpdater == null || eventMerger == null ) {
        LOG.error("unable to get updater for type " + event.eventType);
        LOG.error("aborting update for event : " + event.id);
        return;
    }

    Event updatedEvent = eventUpdater.updateEvent(event);
    Event merged = eventMerger.merge(event,updatedEvent);

    if ( merged == null ) {
      LOG.error("error updating event, quarentining : " + event.id);
      return;
    }
    merged.status = EventStatusEnum.HEALTHY;
    EventService.updateEvent(merged);



  }

}
