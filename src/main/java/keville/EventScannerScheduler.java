package keville;

import keville.compilers.EventCompiler;
import keville.settings.Settings;
import keville.compilers.RSSCompiler;
import keville.AllEvents.AllEventsScanner;
import keville.Eventbrite.EventbriteScanner;
import keville.meetup.MeetupScanner;

import java.util.function.Predicate;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.time.Instant;


public class EventScannerScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventScannerScheduler.class);
  private List<EventScanJob> jobs;
  private List<EventCompiler> compilers;
  private int timeStepMS = 10000;

  private EventbriteScanner eventbriteScanner;
  private MeetupScanner meetupScanner;
  private AllEventsScanner allEventsScanner;
  private Settings settings;

  public EventScannerScheduler(Settings settings) {

    this.settings = settings;

    eventbriteScanner = new EventbriteScanner(settings);
    meetupScanner = new MeetupScanner(settings);
    allEventsScanner = new AllEventsScanner(settings);

    jobs = new LinkedList<EventScanJob>();
    loadCompilers();

    LOG.info(" found : " + settings.scanRoutines.size() + " scan routines ");
    LOG.info(" found : " + compilers.size() + " compilers ");

    settings.scanRoutines.stream()
      .filter(e -> e.runOnRestart)
      .forEach(e -> {
        LOG.info(e.name + " is scheduled to run on restart");
      });

  }

  public void run() {
      
    LOG.debug("starting scheduler ...");

    while ( true ) {

      LOG.debug("evaluating scan routines");

      for ( ScanRoutine routine : settings.scanRoutines ) {
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

        switch ( esj.source ) {
          case ALLEVENTS: 
            try {
              scanReport = allEventsScanner.scan(esj.latitude,esj.longitude,esj.radius);
            } catch (Exception e) {
              LOG.error("scan failed : ALLEVENTS");
              LOG.error(e.getMessage());
            }
            break;
          case EVENTBRITE:
            try {
              scanReport = eventbriteScanner.scan(esj.latitude,esj.longitude,esj.radius);
            } catch (Exception e) {
              LOG.error("scan failed : EVENTBRITE");
              LOG.error(e.getMessage());
            }
            break;
          case MEETUP: 
            try {
              scanReport  = meetupScanner.scan(esj.latitude,esj.longitude,esj.radius);
            } catch (Exception e) {
              LOG.error("scan failed : MEETUP");
              LOG.error(e.getMessage());
            }
            break;
          case DEBUG:
            break;
          default:
            LOG.warn("This EventType case has not been programmed explicitly and will not be evaluated");
        }

        LOG.info("scan job complete");

        if ( scanReport != null ) {

          LOG.info(scanReport.toString());

          LOG.info("compiling new events into output formats");

          List<Event> discoveries  =  EventService.createEvents(scanReport.events);
          for ( EventCompiler ec : compilers )  {
            ec.compile(discoveries);
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
    if ( routine.eventbrite && ( settings.eventbriteApiKey == null || settings.eventbriteApiKey.equals("") )) {
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

      if ( routine.eventbrite ) {
        newJobs.add(new EventScanJob(EventTypeEnum.EVENTBRITE,routine.radius,routine.latitude,routine.longitude));
      }

      if ( routine.meetup ) {
        newJobs.add(new EventScanJob(EventTypeEnum.MEETUP,routine.radius,routine.latitude,routine.longitude));
      }

      if ( routine.allevents) {
        newJobs.add(new EventScanJob(EventTypeEnum.ALLEVENTS,routine.radius,routine.latitude,routine.longitude));
      }

      return newJobs;

  }

  private void loadCompilers() {

    compilers = new LinkedList<EventCompiler>();

    //File file = new File("within3miles.rss");
    /*
    Predicate<Event> filter = Events.InTheFuture().
      and(Events.WithinKMilesOf(settings.latitude,settings.longitude,settings.radius));
      */
    //compilers.add(new RSSCompiler(filter,file));
    
  }

}
