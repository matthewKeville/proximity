package keville;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.time.Instant;

import keville.Eventbrite.EventbriteScanner;
import keville.meetup.MeetupScanner;
import keville.AllEvents.AllEventsScanner;

public class EventScannerScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventScannerScheduler.class);
  private List<EventScanJob> jobs;
  private int timeStepMS = 10000; //10 sec //60000;
  private Properties props;
  private EventService eventService;

  private EventbriteScanner eventbriteScanner;
  private MeetupScanner meetupScanner;
  private AllEventsScanner allEventsScanner;


  public EventScannerScheduler(EventService eventService, Properties props) {
    this.eventService = eventService;
    this.props = props;

    eventbriteScanner = new EventbriteScanner(eventService, props);
    meetupScanner = new MeetupScanner(eventService, props);
    allEventsScanner = new AllEventsScanner(eventService, props);

    jobs = new ArrayList<EventScanJob>();
    loadScanJobs();
    LOG.info(" found : " + jobs.size() + " jobs ");
  }

  //this will run one scan at a time 
  public void run() {

    while ( true ) {

      LOG.info("evaluating job list");

      for ( EventScanJob esj : jobs ) {
        if ( shouldRunNow(esj)) {
          LOG.info("new scan job started");
          LOG.info(esj.toString());
          //do the scan
          switch ( esj.source ) {
            case ALLEVENTS: 
              allEventsScanner.scan(esj.latitude,esj.longitude,esj.radius);
              break;
            case EVENTBRITE:
              eventbriteScanner.scan(esj.latitude,esj.longitude,esj.radius);
              break;
            case MEETUP: 
              meetupScanner.scan(esj.latitude,esj.longitude,esj.radius);
              break;
            case DEBUG:
              //pass
              break;
            default:
              LOG.warn("This EventType case has not been programmed explicitly and is not evaluated");
          }
          esj.lastRun = Instant.now();
          LOG.info("scan job complete");
          LOG.info(esj.toString());
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

  /* populate jobs with data stored on LFS */
  private void loadScanJobs() {

    jobs.add(new EventScanJob(
          EventTypeEnum.EVENTBRITE,
          5.0,
          40.1784,-74.0218,
          30   // every 3 minutes 
    ));

    /*
    jobs.add(new EventScanJob(
          EventTypeEnum.MEETUP,
          3.0,
          40.1784,-74.0218,
          30   // every 2 minutes 
    ));
    */

    /*
    jobs.add(new EventScanJob(
          EventTypeEnum.ALLEVENTS,
          3.0,
          40.1784,-74.0218,
          30   
    ));
    */

  }

}
