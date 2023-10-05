package keville.scanner;

import keville.event.EventTypeEnum;

public class EventScanJob {

  public EventTypeEnum source;
  public double radius;
  public double latitude;
  public double longitude;

  public EventScanJob(
    EventTypeEnum source,
    double radius,
    double latitude,
    double longitude
    ) {
    this.source = source;
    this.radius = radius;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String toString() {
    return "source : " + source.toString() 
      + " radius " + radius 
      + " lat,lon : "  + latitude + "," + longitude;
  }

}
