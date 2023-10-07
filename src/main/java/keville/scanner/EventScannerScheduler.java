package keville.scanner;

import keville.event.EventService;
import keville.event.EventTypeEnum;
import keville.compilers.EventCompiler;
import keville.providers.Providers;
import keville.settings.Settings;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.time.Instant;


public class EventScannerScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventScannerScheduler.class);
  private List<EventScanJob> jobs;
  private int timeStepMS = 10000;
  private Settings settings;

  public EventScannerScheduler(Settings settings) {

    this.settings = settings;
    jobs = new LinkedList<EventScanJob>();

  }

  public void run() {
      
    LOG.debug("starting scheduler ...");

    while ( true ) {

      LOG.debug("evaluating scan routines");

      Iterator<String> routineKeyIterator = settings.scanRoutines.keySet().iterator();

      while (routineKeyIterator.hasNext() ) {
        ScanRoutine routine = settings.scanRoutines.get(routineKeyIterator.next());
        if ( shouldRunNow(routine) ) {
          LOG.info("It is time to scan " + routine.name);
          List<EventScanJob> newJobs = makeScanJobs(routine);
          LOG.info("adding " + newJobs.size() + " into the scan job queue ");
          jobs.addAll(newJobs);
          routine.lastRan = Instant.now();
        }
      }

      if ( jobs.size() != 0 ) {

        EventScanJob esj = jobs.remove(0);

        LOG.info("executing scan job");
        LOG.info(esj.toString());
        ScanReport scanReport = null;
        try {

            EventScanner scanner = Providers.getScanner(esj.source); 
            if ( scanner != null ) {
                scanReport = Providers.providers.get(esj.source)
                    .scanner.scan(esj.latitude,esj.longitude,esj.radius);
            } else {
                LOG.error("unable to find a scanner for type : " + esj.source);
            }

        } catch (Exception e) {

            LOG.error("scan failed, type : " + esj.source.toString());
            LOG.error(e.getMessage());

        }


        LOG.info("scan job complete");

        if ( scanReport != null ) {


          LOG.info("processing scanned events");

          ScannedEventsReport ser =  EventService.processFoundEvents(scanReport.events);

          LOG.info(scanReport.toString());
          LOG.info(ser.toString());

          for ( EventCompiler ec : settings.eventCompilers )  {
            ec.compile(ser.getAll());
          }

        }

      }

      // wait

      try {
        Thread.sleep(timeStepMS);
      } catch (Exception e) {
        LOG.error("error trying to sleep thread");
        LOG.error(e.getMessage());
      }

    }

  }

  private boolean shouldRunNow(ScanRoutine routine) {
    if ( routine.disabled ) return false;
    //this failsafe is in the wrong place, we want to abstract away the
    //scan routine source... 
    //TODO : move to settings where we are aware of specific types.
    if ( routine.types.contains(EventTypeEnum.EVENTBRITE) && 
        ( settings.eventbriteApiKey == null || 
          settings.eventbriteApiKey.equals("") 
        )
    ) {
      LOG.warn("the configuration for " + routine.name + " is invalid");
      LOG.warn("to use eventbrite scanning you must supply an eventbrite api key");
      return false;
    } 
    Instant nextScanStart = (routine.lastRan).plusSeconds(routine.delay);
    Instant now = Instant.now();
    return  nextScanStart.isBefore(now);
  }

  private List<EventScanJob> makeScanJobs(ScanRoutine routine) {

    List<EventScanJob> newJobs = new LinkedList<EventScanJob>();

    Iterator<EventTypeEnum> typeIterator = routine.types.iterator();

    while ( typeIterator.hasNext() ) {
        newJobs.add(new EventScanJob(typeIterator.next(),
            routine.radius,
            routine.latitude,
            routine.longitude
        ));
    }

    return newJobs;

  }



}
