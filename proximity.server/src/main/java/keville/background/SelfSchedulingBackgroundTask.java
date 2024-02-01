package keville.background;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.TaskScheduler;

/* a runnable class the schedules itself */
public abstract class SelfSchedulingBackgroundTask implements Runnable {

  protected final String taskName;
  protected final TaskScheduler taskScheduler;
  protected Duration delay; //mutable

  /* Not autowired -> Not Externally Constructible */
  public SelfSchedulingBackgroundTask(
      TaskScheduler taskScheduler,
      Duration startupDelay,
      Duration delay,
      String taskName
      ) {
    this.taskScheduler = taskScheduler;
    this.delay = delay;
    this.taskName = taskName;
    this.taskScheduler.schedule(this,Instant.now().plus(startupDelay));
  }

  public final void run() {
    doTask();
    taskScheduler.schedule(this,Instant.now().plus(delay));
  }

  abstract void doTask();

}
