package keville;

import java.util.LinkedList;
import java.util.List;
import java.time.Duration;

import keville.meetup.MeetupUpdater;
import keville.AllEvents.AllEventsUpdater;
import keville.Eventbrite.EventbriteUpdater;

import keville.settings.Settings;


public class EventUpdaterScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventUpdaterScheduler.class);
  private int timeStepMS = 10000; /* every ten seconds */
  private List<Event> outdatedEvents;
  private MeetupUpdater meetupUpdater;
  private EventbriteUpdater eventbriteUpdater;
  private AllEventsUpdater allEventsUpdater;
  private final int updateBatchSize = 5;
  private final Duration maxAge = /* dev value */ Duration.ofMinutes(5); //Duration.ofDays(1); 
  //must be careful here as maxAge directly affects request frequency. 100 events invalidated at 5 minutes would
  //be 28800 requests per day.

  /**
 * @param settings
 */
public EventUpdaterScheduler(Settings settings) {

    this.outdatedEvents = new LinkedList<Event>();
    this.meetupUpdater = new MeetupUpdater();
    this.eventbriteUpdater = new EventbriteUpdater();
    this.allEventsUpdater = new AllEventsUpdater();

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

    switch (event.eventType) {
      case DEBUG:
        break;
      case EVENTBRITE:
        eventbriteUpdater.updateEvent(event);
        break;
      case ALLEVENTS:
        allEventsUpdater.updateEvent(event);
        break;
      case MEETUP:
        meetupUpdater.updateEvent(event);
        break;
    }

  }

}
