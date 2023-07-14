package keville.util;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

public class DateTimeUtils {

  //https://stackoverflow.com/questions/32826077/parsing-iso-instant-and-similar-date-time-strings
  public static LocalDateTime ISOInstantToLocalDateTime(String instantString) {
    DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
    Instant instant = Instant.from(dtf.parse(instantString));
    LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
    return localDateTime;
  }

}
