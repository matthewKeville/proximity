package keville.compilers;

import keville.event.EventService;
import keville.settings.Settings;

public class EventCompilerScheduler implements Runnable {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventCompilerScheduler.class);
  private int timeStepMS = 2*60*1000;
  private Settings settings;

  public EventCompilerScheduler(Settings settings) {
    this.settings = settings;
  }

  public void run() {
      
    LOG.debug("starting scheduler ...");

    while ( true ) {

      for ( EventCompiler ec : settings.eventCompilers )  {
        LOG.info("compiling " + ec.name);
        ec.compile(EventService.getAllEvents());
      }

      try {
        Thread.sleep(timeStepMS);
      } catch (Exception e) {
        LOG.error("error trying to sleep thread");
        LOG.error(e.getMessage());
      }

    }

  }

}
