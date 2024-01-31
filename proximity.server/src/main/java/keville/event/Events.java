package keville.event;

import java.util.List;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.Instant;
import java.time.Duration;
import java.util.function.Predicate;
import keville.util.GeoUtils;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Events {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Events.class);

  public static Predicate<Event> WithinKMilesOf(double lat, double lon, double miles) {

    return new Predicate<Event>() {
      public boolean test(Event event) {
        if ( event.location.latitude == null || event.location.longitude == null ) {
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

  // return events that are not online ( match online only if invert )
  public static Predicate<Event> InPerson(boolean invert) {

    return new Predicate<Event>() {
      public boolean test(Event event) {
        return invert ? event.virtual : !event.virtual;
      }
    };

  }

  // return events that match ( do not match if invert ) the list of keywords
  // compare against event.name and event.description
  public static Predicate<Event> Keywords(List<String> keywords,boolean caseInensitive,boolean invert) {

    return new Predicate<Event>() {

      public boolean test(Event event) {

          for ( String key : keywords ) {

              Pattern pattern = Pattern.compile(Pattern.quote(key), ((caseInensitive) ? Pattern.CASE_INSENSITIVE : Pattern.LITERAL ));

              if ( event.description != null ) {
                Matcher descriptionMatcher = pattern.matcher(event.description);
                if (descriptionMatcher.find()) {
                  return !invert;
                }
              }

              if ( event.name != null ) {
                Matcher nameMatcher = pattern.matcher(event.name);
                if (nameMatcher.find()) {
                  return !invert;
                }
              }

          }

          return invert; //no match
      }
    };

  }

  // return events that match ( do not match if invert ) the list of week days
  public static Predicate<Event> Weekdays(List<DayOfWeek> days,boolean invert) {

    return new Predicate<Event>() {
      public boolean test(Event event) {

          ZonedDateTime date = event.start.atZone(ZoneId.of("UTC"));

          for ( DayOfWeek day : days ) {
            if ( date.getDayOfWeek().equals(day) )  {
              return invert ? false : true;
            }
          }

          return invert; //filter not matched
      }
    };

  }

  // return events that match ( do not match if invert ) the list of months
  public static Predicate<Event> Months(List<Month> months,boolean invert) {

    return new Predicate<Event>() {
      public boolean test(Event event) {

          ZonedDateTime date = event.start.atZone(ZoneId.of("UTC"));

          for ( Month month : months ) {
            if ( date.getMonth().equals(month) )  {
              return invert ? false : true;
            }
          }

          return false;
      }
    };

  }

  //return events between the DaysAwayRange
  public static Predicate<Event> DaysAwayRange(int minDays,int maxDays) {

    return new Predicate<Event>() {
      public boolean test(Event event) {
        Duration duration = Duration.between(Instant.now(),event.start);
        int daysFromNow = (int) duration.toDaysPart();
        return daysFromNow < maxDays && daysFromNow > minDays;
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
