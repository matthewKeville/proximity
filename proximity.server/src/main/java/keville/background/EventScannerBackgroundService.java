package keville.background;

import keville.event.EventService;
import keville.event.EventTypeEnum;
import keville.providers.Providers;
import keville.scanner.EventScanner;
import keville.scanner.ScanReport;
import keville.settings.ScanRoutine;
import keville.scanner.ScannedEventsReport;
import keville.settings.Settings;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedList;
import java.time.Duration;
import java.time.Instant;

@Component
public class EventScannerBackgroundService extends SelfSchedulingBackgroundTask {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventScannerBackgroundService.class);
  private static final Duration delay = Duration.ofSeconds(60);
  private static final Duration startupDelay = Duration.ofSeconds(15);

  private Settings settings;
  private EventService eventService;
  private Providers providers;

  private List<EventScanJob> jobs;

  public EventScannerBackgroundService(
      @Autowired Settings settings,
      @Autowired EventService eventService,
      @Autowired TaskScheduler taskScheduler,
      @Autowired Providers providers
      ) {
    super(taskScheduler,delay,startupDelay,"Event Compiler Background Service");
    this.settings = settings;
    this.eventService = eventService;
    this.providers = providers;
    jobs = new LinkedList<EventScanJob>();
  }

  //run all routines that are allowed to run
  public void doTask() {

      LOG.info("Evaluating Scan Routines");

      //translate routines into scan jobs
      Iterator<String> routineKeyIterator = settings.scanRoutines().keySet().iterator();
      while (routineKeyIterator.hasNext()) {

        ScanRoutine routine = settings.scanRoutines().get(routineKeyIterator.next());

        if (routineShouldRun(routine)) {
          jobs.addAll(makeScanJobs(routine));
          //Not the best naming convention here, it was slated to run now. It's in the queue
          routine.lastRan = Instant.now();
        }

      }

      LOG.info(jobs.size() + " Scan Jobs in queue ");

      //run 1 scan job from the queue
      if (jobs.size() != 0) {

        EventScanJob esj = jobs.remove(0);
        LOG.info("Performing scan " + esj.toString());
        ScanReport scanReport = null;

        try {

          EventScanner scanner = providers.getScanner(esj.source);
          if (scanner != null) {
            scanReport = providers.providers.get(esj.source).scanner.scan(esj.latitude, esj.longitude, esj.radius);
          } else {
            LOG.error("Unable to find a scanner for type : " + esj.source);
          }

        } catch (Exception e) {

          LOG.error("scan failed, type : " + esj.source.toString());
          LOG.error(e.getMessage());

        }

        LOG.info("Scan complete");

        if (scanReport != null) {

          //This design seems incoherent (ScannedEventsReport & ScanReport) ?
          LOG.debug("processing scanned events");
          ScannedEventsReport ser = eventService.processFoundEvents(scanReport.events);
          LOG.info(scanReport.toString());
          LOG.info(ser.toString());

        }

      }

  }

  private boolean routineShouldRun(ScanRoutine routine) {

    if (routine.disabled)
      return false;

    Instant nextScanStart = (routine.lastRan).plusSeconds(routine.delay);
    Instant now = Instant.now();
    return nextScanStart.isBefore(now);

  }

  private List<EventScanJob> makeScanJobs(ScanRoutine routine) {

    List<EventScanJob> newJobs = new LinkedList<EventScanJob>();

    Iterator<EventTypeEnum> typeIterator = routine.types.iterator();

    while (typeIterator.hasNext()) {
      newJobs.add(new EventScanJob(typeIterator.next(),
          routine.radius,
          routine.latitude,
          routine.longitude));
    }

    return newJobs;

  }

}
