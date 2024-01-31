package keville.scanner;

import keville.event.Event;

import java.util.List;
import java.time.Instant;
import java.time.Duration;

public class ScanReport {

  public Instant start;
  public Instant processStart;
  public Instant finish;

  List<Event> events;

  public ScanReport(Instant start, Instant processStart, Instant finish, List<Event> events) {
    this.start = start;
    this.processStart = processStart;
    this.finish = finish;
    this.events = events;
  }

  public String toString() {
    String result = "\nstart:\t" + this.start.toString();
    result = "\nprocessingStart:\t" + this.start.toString();
    result += "\nfinish:\t" + this.finish.toString();

    Duration stime = Duration.between(this.start, this.processStart);
    Duration ptime = Duration.between(this.processStart, this.finish);
    Duration ttime = Duration.between(this.start, this.finish);

    result += "\nscan time:\t" + stime.getSeconds() + " (s) ";
    result += "\nprocess time:\t" + ptime.getSeconds() + " (s) ";
    result += "\ntotal elapsed:\t" + ttime.getSeconds() + " (s) ";
    result += "\nscanned events:\t" + this.events.size();
    return result;
  }

}
