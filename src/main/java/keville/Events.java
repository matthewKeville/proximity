package keville;

import java.time.Instant;
import java.time.Duration;
import keville.util.GeoUtils;
import java.util.function.Predicate;

public class Events {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Events.class);

  public static Predicate<Event> WithinKMilesOf(double lat, double lon, double miles) {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        if ( event.location.latitude == null | event.location.longitude == null ) {
          return event.virtual;
        }
        return GeoUtils.isWithinMilesOf(miles, lat, lon, event.location.latitude, event.location.longitude);
      }
    };
  }

  public static Predicate<Event> InTheFuture() {
    return new Predicate<Event>() {
      public boolean test(Event event) {
        return event.start.isAfter(Instant.now());
      }
    };
  }

  public static ClientEvent CreateClientEvent(Event event,double latitude,double longitude) {

    double distance = 0.0;
    if ( event.virtual != true ) {
      distance = GeoUtils.distanceInMiles(event.location.latitude,event.location.longitude,latitude,longitude);
    } else {
      LOG.warn("spoofing distance for virtual event");
    }

    Duration duration = Duration.between(Instant.now(),event.start);
    return new ClientEvent(event,distance,(int) duration.toDaysPart(), duration.toHoursPart());
  }


}
