package keville.background;

import keville.event.EventService;
import keville.event.EventTypeEnum;
import keville.providers.Providers;
import keville.scanner.EventScanJob;
import keville.scanner.EventScanner;
import keville.scanner.ScanReport;
import keville.scanner.ScanRoutine;
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
  private static final Duration startupDelay = Duration.ofSeconds(30);

  private Settings settings;
  private EventService eventService;

  private List<EventScanJob> jobs;
  public int timeStepMS = 10000;

  public EventScannerBackgroundService(
      @Autowired Settings settings,
      @Autowired EventService eventService,
      @Autowired TaskScheduler taskScheduler
      ) {
    super(taskScheduler,delay,startupDelay,"Event Compiler Background Service");
    this.settings = settings;
    this.eventService = eventService;
    jobs = new LinkedList<EventScanJob>();
  }

  //run all routines that are allowed to run
  public void doTask() {

      Iterator<String> routineKeyIterator = settings.scanRoutines().keySet().iterator();
      while (routineKeyIterator.hasNext()) {

        ScanRoutine routine = settings.scanRoutines().get(routineKeyIterator.next());
        if (routineShouldRun(routine)) {
          LOG.info("It is time to scan " + routine.name);
          List<EventScanJob> newJobs = makeScanJobs(routine);
          LOG.info("adding " + newJobs.size() + " into the scan job queue ");
          jobs.addAll(newJobs);
          routine.lastRan = Instant.now();
        }

      }

      if (jobs.size() != 0) {

        EventScanJob esj = jobs.remove(0);
        LOG.info("executing scan job");
        LOG.info(esj.toString());
        ScanReport scanReport = null;

        try {

          EventScanner scanner = Providers.getScanner(esj.source);
          if (scanner != null) {
            scanReport = Providers.providers.get(esj.source).scanner.scan(esj.latitude, esj.longitude, esj.radius);
          } else {
            LOG.error("unable to find a scanner for type : " + esj.source);
          }

        } catch (Exception e) {

          LOG.error("scan failed, type : " + esj.source.toString());
          LOG.error(e.getMessage());

        }

        LOG.info("scan job complete");

        if (scanReport != null) {

          LOG.info("processing scanned events");

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
