package keville;

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


}
