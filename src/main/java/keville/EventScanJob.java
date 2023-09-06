
package keville;
import java.time.Instant;

public class EventScanJob {

  public EventTypeEnum source;
  public double radius;
  public double latitude;
  public double longitude;

  public Instant lastRun; 
  public long delayInSeconds; /*time in ms between scans*/

  public EventScanJob(
    EventTypeEnum source,
    double radius,
    double latitude,
    double longitude,
    long delayInSeconds
    ) {
    this.source = source;
    this.radius = radius;
    this.latitude = latitude;
    this.longitude = longitude;
    this.delayInSeconds = delayInSeconds;
    lastRun = Instant.now();
  }


  public String toString() {
    return "source : " + source.toString() 
      + " radius " + radius 
      + " lat,lon : "  + latitude + "," + longitude
      + " delay (s) : " + delayInSeconds 
      + " last ran : " + lastRun.toString();
  }

}
