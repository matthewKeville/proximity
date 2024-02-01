package keville.background;

import keville.event.Event;
import keville.merger.EventMerger;
import keville.providers.Providers;
import keville.event.EventService;
import keville.event.EventStatusEnum;
import keville.updater.EventUpdater;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.time.Duration;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class EventUpdaterBackgroundService extends SelfSchedulingBackgroundTask  {


  private static org.slf4j.Logger LOG = LoggerFactory.getLogger(EventUpdaterBackgroundService.class);
  private static final Duration delay = Duration.ofSeconds(60);
  private static final Duration startupDelay = Duration.ofSeconds(60);

  private EventService eventService;
  private Providers providers;

  private List<Event> outdatedEvents;
  private final int updateBatchSize = 5;
  private final Duration maxAge = Duration.ofDays(10);// FIXME : This is a bad placeholder

  public EventUpdaterBackgroundService(
      @Autowired EventService eventService,
      @Autowired Providers providers,
      @Autowired TaskScheduler taskScheduler) {
    super(taskScheduler, delay,startupDelay,"Event Updater Background Service");
    this.eventService = eventService;
    this.providers = providers;
    this.outdatedEvents = new LinkedList<Event>();
  }

  public void doTask() {

    LOG.info("running EventUpdaterBackgroundService ");

    if (outdatedEvents.size() == 0) {
      getOutdatedEventBatch();
    }

    if (outdatedEvents.size() != 0) {
      LOG.info("updating " + outdatedEvents.size());
      ListIterator<Event> eventIterator = outdatedEvents.listIterator();
      while (eventIterator.hasNext()) {
        Event outdated = eventIterator.next();
        updateEvent(outdated);
        eventIterator.remove();
      }
    } else {
      LOG.info("all events up to date");
    }

  }

  private void getOutdatedEventBatch() {
    LOG.info("retrieving a batch of outdated events from the database");
    List<Event> batch = eventService.getOudatedEvents(updateBatchSize, maxAge);
    LOG.info("found " + batch.size());
    outdatedEvents.addAll(batch);
  }

  private void updateEvent(Event event) {

    LOG.warn("updating event " + event.id);

    EventUpdater eventUpdater = providers.getUpdater(event.eventType);
    EventMerger eventMerger = providers.getMerger(event.eventType);

    if (eventUpdater == null || eventMerger == null) {
      LOG.error("unable to get updater for type " + event.eventType);
      LOG.error("aborting update for event : " + event.id);
      return;
    }

    Event updatedEvent = event;
    try {
      updatedEvent = eventUpdater.updateEvent(event);
    } catch (Exception e) {
      LOG.error("critical error trying to update event : " + event.id);
      LOG.error(e.getMessage());
      return;
    }

    if (updatedEvent == null) {
      LOG.error("unable to update event : " + event.id);
      LOG.error("quarentining : " + event.id);
      event.status = EventStatusEnum.QUARENTINE;
      eventService.updateEvent(event);
      return;
    }

    Event merged = eventMerger.merge(event, updatedEvent);

    if (merged == null) {
      LOG.error("unable to merge updated event : " + event.id);
      LOG.error("quarentining : " + event.id);
      event.status = EventStatusEnum.QUARENTINE;
      eventService.updateEvent(event);
      return;
    }

    merged.status = EventStatusEnum.HEALTHY;
    eventService.updateEvent(merged);

  }

}
