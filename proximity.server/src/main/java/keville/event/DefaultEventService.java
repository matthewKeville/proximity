package keville.event;

import keville.merger.EventMerger;
import keville.providers.Providers;
import keville.scanner.ScannedEventsReport;
import keville.settings.Settings;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class DefaultEventService implements EventService {

  private static Logger LOG = LoggerFactory.getLogger(DefaultEventService.class);
  private Providers providers;
  private Settings settings;
  private EventRepository eventRepository;

  public DefaultEventService(
      @Autowired Settings settings,
      @Autowired EventRepository eventRepository,
      @Autowired Providers providers) {
    this.settings = settings;
    this.eventRepository = eventRepository;
    this.providers = providers;
  }

  public Optional<Event> getByEventIdAndType(String eventId,EventTypeEnum type) {
    return eventRepository.findByEventIdAndType(eventId,type); 
  }

  /* 
   * very inefficient
  */
  public List<Event> getEvents(Predicate<Event> filter) {

    Iterable<Event> dbEvents = eventRepository.findAll();
    List<Event> events = Lists.newArrayList(dbEvents);

    //filter
    List<Event> result = events.stream().
      filter(filter).
      collect(Collectors.toList());
    return result;
  }

  public List<Event> getAllEvents() {
    Iterable<Event> dbEvents = eventRepository.findAll();
    return Lists.newArrayList(dbEvents);
  }

  public List<Event> getOudatedEvents(int batchSize,Duration maxAcceptableAge) {
    Predicate<Event> filter = (e -> e.status == EventStatusEnum.INCOMPLETE);
    filter = filter.or(e ->  e.lastUpdate.plus(maxAcceptableAge).isBefore(LocalDateTime.now(ZoneOffset.UTC)));
    filter = filter.and(e ->  e.status != EventStatusEnum.QUARENTINE);
    return getEvents(filter).stream().limit(batchSize).collect(Collectors.toList());
  }

  // UpdaterBackgroundService depends on this... this should be a different name
  // try to update ..?
  public boolean updateEvent(Event event) {
    eventRepository.save(event);
    return true;
  }

  public ScannedEventsReport processFoundEvents(List<Event> events) {

    List<Event> created  = new  LinkedList<Event>();
    List<Event> updated  = new  LinkedList<Event>();
    List<Event> unchanged  = new  LinkedList<Event>();

    for ( Event e : events ) {

      Optional<Event> dbeOpt = getByEventIdAndType(e.eventId,e.eventType);

      if ( dbeOpt.isEmpty()) {

        created.add(eventRepository.save(e));

      } else {

        Event dbe = dbeOpt.get();
        EventMerger eventMerger = providers.getMerger(dbe.eventType);
        if ( eventMerger == null ) {
            LOG.error("Unable to find merger for type : " + dbe.eventType + " to merge event " + dbe.id);
            continue;
        }

        Event merge = eventMerger.merge(dbe,e);
        if ( merge != null ) {
          LOG.debug("Found an updated version of existing event " + merge.id);
          updated.add(eventRepository.save(dbe));
        } else {
          LOG.debug("Found an existing event in scan " + dbe.id + " but it is not updated");
          unchanged.add(eventRepository.save(dbe));
        }

      }

    }

    return new ScannedEventsReport(created,updated,unchanged);

  }

}
