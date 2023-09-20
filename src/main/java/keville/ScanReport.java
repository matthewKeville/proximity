package keville;
import java.time.Instant;
import java.time.Duration;

public class ScanReport {

  public Instant start; 
  public Instant processStart; 
  public Instant finish; 

  public int totalEventsFound; 
  public int newEventsFound; 

  public ScanReport(Instant start,Instant processStart,Instant finish,int totalEventsFound, int newEventsFound) {
    this.start = start;
    this.processStart = processStart;
    this.finish = finish;
    this.totalEventsFound  = totalEventsFound;
    this.newEventsFound  = newEventsFound;
  }

  public String toString() {
    String result =  "start:\t" + this.start.toString();
    result =  "processingStart:\t" + this.start.toString();
    result +=  "\nfinish:\t" + this.finish.toString();

    Duration stime = Duration.between(this.start,this.processStart);
    Duration ptime = Duration.between(this.processStart,this.finish);
    Duration ttime = Duration.between(this.start,this.finish);

    result +=  "\nscan time:\t" + stime.getSeconds() +  " (s) ";
    result +=  "\nprocess time:\t" + ptime.getSeconds() +  " (s) ";
    result +=  "\ntotal elapsed:\t" + ttime.getSeconds() +  " (s) ";
    result +=  "\nscanned events:\t" + this.totalEventsFound;
    result +=  "\nnew events:\t" + this.newEventsFound;
    return result;
  }

}
