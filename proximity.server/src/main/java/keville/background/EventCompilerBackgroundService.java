package keville.background;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import keville.compilers.EventCompiler;
import keville.event.EventService;
import keville.settings.Settings;

@Component
public class EventCompilerBackgroundService extends SelfSchedulingBackgroundTask {

  private static Logger LOG = LoggerFactory.getLogger(EventCompilerBackgroundService.class);
  private static final Duration delay = Duration.ofSeconds(60);
  private static final Duration startupDelay = Duration.ofSeconds(90);

  private EventService eventService;
  private Settings settings;

  public EventCompilerBackgroundService(
      @Autowired Settings settings,
      @Autowired EventService eventService,
      @Autowired TaskScheduler taskScheduler
      ) {
    super(taskScheduler,delay,startupDelay,"Event Compiler Background Service");
    this.settings = settings;
    this.eventService = eventService;
  }

  public void doTask() {

    LOG.info("running EventCompilerBackgroundService ");
    LOG.info("compiling " + settings.eventCompilers().size() + " artifacts");

    for (EventCompiler ec : settings.eventCompilers()) {
      LOG.info("compiling " + ec.name);
      ec.compile(eventService.getAllEvents());
    }

  }

}
