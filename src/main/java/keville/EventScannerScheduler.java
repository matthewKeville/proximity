package keville;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.time.Instant;

import keville.Eventbrite.EventbriteScanner;
import keville.meetup.MeetupScanner;
import keville.AllEvents.AllEventsScanner;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class EventScannerScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventScannerScheduler.class);
  private List<EventScanJob> jobs;
  private int timeStepMS = 10000; //30
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

    boolean firstRun = true; //run all jobs on startup

    while ( true ) {

      LOG.info("evaluating job list");

      for ( EventScanJob esj : jobs ) {
        if ( firstRun || shouldRunNow(esj)) {
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
              break;
            default:
              LOG.warn("This EventType case has not been programmed explicitly and is not evaluated");
          }
          esj.lastRun = Instant.now();
          LOG.info("scan job complete");
          LOG.info(esj.toString());
        }
      }

      if ( firstRun ) {
        firstRun = false;
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

    Path jobFilePath = FileSystems.getDefault().getPath("scan-jobs.csv");

    try {

      for ( String jobRow : Files.readAllLines(jobFilePath) ) {

        //EventTypeEnum,radius,lat,lon,delay(s)
        String[] fields = jobRow.split(",");
        if ( fields.length != 5 ) {
          LOG.warn("invalid scan job at \n\t" + jobRow);
          continue;
        }

        EventTypeEnum type = EventTypeEnum.valueOf(fields[0]);
        double radius = Double.parseDouble(fields[1]);
        double latitude = Double.parseDouble(fields[2]);
        double longitude = Double.parseDouble(fields[3]);
        int delay = Integer.parseInt(fields[4]);

        EventScanJob esj = new EventScanJob(type,radius,latitude,longitude,delay);
        jobs.add(esj);

      }
    } catch (Exception e) {
      LOG.error("error reading scan jobs from scan-jobs.csv");
      LOG.error(e.getMessage());
    }

  }

}
