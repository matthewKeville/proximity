package keville;

import keville.compilers.EventCompiler;
import keville.compilers.RSSCompiler;
import keville.AllEvents.AllEventsScanner;
import keville.Eventbrite.EventbriteScanner;
import keville.meetup.MeetupScanner;

import java.io.File;
import java.util.function.Predicate;
import java.util.List;
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

  public EventScannerScheduler(Settings settings) {

    eventbriteScanner = new EventbriteScanner(settings);
    meetupScanner = new MeetupScanner(settings);
    allEventsScanner = new AllEventsScanner(settings);

    jobs = new ArrayList<EventScanJob>();
    compilers = new ArrayList<EventCompiler>();

    loadScanJobs(settings);
    loadCompilers(settings);

    LOG.info(" found : " + jobs.size() + " jobs ");
    LOG.info(" found : " + compilers.size() + " compilers ");

  }

  public void run() {

    while ( true ) {

      LOG.debug("evaluating job list");

      for ( EventScanJob esj : jobs ) {
        if ( shouldRunNow(esj) ) {

          LOG.info("scan job started");
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

          esj.lastRun = Instant.now();
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
      }

      try {
        Thread.sleep(timeStepMS);
      } catch (Exception e) {
        LOG.error("error trying to sleep thread");
        LOG.error(e.getMessage());
      }

    }
  }

  private boolean shouldRunNow(EventScanJob job) {
    Instant nextScanStart = (job.lastRun).plusSeconds(job.delayInSeconds);
    Instant now = Instant.now();
    return  nextScanStart.isBefore(now);
  }

  private void loadScanJobs(Settings settings) {

    if ( settings.eventbrite ) {
      jobs.add(new EventScanJob(EventTypeEnum.EVENTBRITE,settings.radius,settings.latitude,settings.longitude,settings.delay,settings.runOnRestart));
    }
    if ( settings.meetup ) {
      jobs.add(new EventScanJob(EventTypeEnum.MEETUP,settings.radius,settings.latitude,settings.longitude,settings.delay,settings.runOnRestart));
    }
    if ( settings.allevents) {
      jobs.add(new EventScanJob(EventTypeEnum.ALLEVENTS,settings.radius,settings.latitude,settings.longitude,settings.delay,settings.runOnRestart));
    }

  }

  private void loadCompilers(Settings settings) {
    //debug placeholder data
    File file = new File("within3miles.rss");
    Predicate<Event> filter = Events.InTheFuture().
      and(Events.WithinKMilesOf(settings.latitude,settings.longitude,settings.radius));
    compilers.add(new RSSCompiler(filter,file));
  }

}
