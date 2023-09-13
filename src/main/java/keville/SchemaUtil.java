package keville;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;

// Transform https://schema.org/ entities into Domain entities

public class SchemaUtil {

 private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SchemaUtil.class);

 public static EventBuilder createEventFromSchemaEvent(JsonObject eventJson) {

      EventBuilder eb = new EventBuilder();

      String timestring = eventJson.get("startDate").getAsString(); // DateTime or Date
      if ( timestring.length() == 10 ) {  // ex: 2023‐09‐13
        LOG.warn("Schema event had Date instead of DateTime , faking Time component");
        timestring+="T00:00:00.000Z";
      }       
      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestring));

      eb.setStart(start);
      eb.setName(eventJson.get("name").getAsString());

      if ( eventJson.has("description") ) {
        eb.setDescription(eventJson.get("description").getAsString());
      }

      JsonObject location = eventJson.getAsJsonObject("location");

      if ( location.get("@type").getAsString().equals("Place") ) {

        JsonObject geo = location.getAsJsonObject("geo");
        String latitudeString = geo.get("latitude").getAsString();
        String longitudeString = geo.get("longitude").getAsString();

        eb.setLatitude(Double.parseDouble(latitudeString));
        eb.setLongitude(Double.parseDouble(longitudeString));
        eb.setVirtual(false);

        JsonObject address = location.getAsJsonObject("address");

        if ( address.get("@type").getAsString().equals("PostalAddress") ) {

          eb.setCity(address.get("addressLocality").getAsString());
          eb.setState(address.get("addressRegion").getAsString());

        } else {

          LOG.info("unable to determine addressLocality and addressRegion because unknown Address Type " + address.get("@type").getAsString());

        }
      } else if ( location.get("@type").getAsString().equals("VirtualLocation") ) {

        eb.setVirtual(true);
        LOG.warn("creating an event with a VirtualLocation");

      } else {

        LOG.error("found an event with an unhandled Location type : " + location.get("@type").getAsString());

      }

    eb.setUrl(eventJson.get("url").getAsString()); 

    return eb;

  }

}
