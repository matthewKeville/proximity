package keville.util;

import keville.location.Location;
import keville.location.LocationBuilder;
import keville.event.Event;
import keville.event.EventBuilder;
import keville.event.EventStatusEnum;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

// Transform https://schema.org/ entities into Domain entities

public class SchemaUtil {

 private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SchemaUtil.class);

 public static EventBuilder createEventFromSchemaEvent(JsonObject eventJson) {

      try {

        EventBuilder eb = new EventBuilder();
        LocationBuilder lb = new LocationBuilder();

        String startTimestring = eventJson.get("startDate").getAsString();
        if ( startTimestring.length() == 10 ) {  // ex: 2023‐09‐13

          LOG.debug("Schema event had start Date instead of DateTime , faking Time component setting INCOMPLETE");
          startTimestring+="T00:00:00.000Z";
          eb.setStatus(EventStatusEnum.INCOMPLETE);

        }       
        Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTimestring));
        eb.setStart(start);


        String endTimestring = eventJson.get("endDate").getAsString(); // DateTime or Date
        if ( endTimestring.length() == 10) {  // ex: 2023‐09‐13
                                               //
          LOG.debug("Schema event had end Date instead of DateTime , faking Time component, setting INCOMPLETE");
          endTimestring+="T00:00:00.000Z";
          eb.setStatus(EventStatusEnum.INCOMPLETE);

        } 
        if ( endTimestring.length() != 0 ) { //meetup has sent schema with empty endDate string 
                                             //
          Instant end  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(endTimestring));
          eb.setEnd(end);

        } else {

          LOG.debug("Schema event had empty endDate string , setting end to start");
          eb.setEnd(start);

        }

        eb.setName(eventJson.get("name").getAsString());

        if ( eventJson.has("description") ) {
          eb.setDescription(eventJson.get("description").getAsString());
        }

        JsonObject location = eventJson.getAsJsonObject("location");

        if ( location.get("@type").getAsString().equals("Place") ) {

          if ( location.has("name") ) {
            lb.setName(location.get("name").getAsString());
          }

          JsonObject geo = location.getAsJsonObject("geo");
          String latitudeString = geo.get("latitude").getAsString();
          String longitudeString = geo.get("longitude").getAsString();

          lb.setLatitude(Double.parseDouble(latitudeString));
          lb.setLongitude(Double.parseDouble(longitudeString));

          eb.setVirtual(false);

          JsonObject address = location.getAsJsonObject("address");

          if ( address.get("@type").getAsString().equals("PostalAddress") ) {
      
            if ( address.has("addressCountry") ) {
              lb.setCountry(address.get("addressCountry").getAsString());
            }

            if ( address.has("addressRegion") ) {
              lb.setRegion(address.get("addressRegion").getAsString());
            }

            if ( address.has("addressLocality") ) {
              lb.setLocality(address.get("addressLocality").getAsString());
            }

          } else {

            LOG.info("unable to determine addressLocality and addressRegion because unknown Address Type " + address.get("@type").getAsString());

          }

        } else if ( location.get("@type").getAsString().equals("VirtualLocation") ) {

          eb.setVirtual(true);

        } else {

          LOG.error("found an event with an unhandled Location type : " + location.get("@type").getAsString());

        }

      if ( eventJson.has("organizer") ) {

        JsonElement organizerElement = eventJson.get("organizer");
        JsonObject organizer = null;

        if ( organizerElement.isJsonArray() ) {

          JsonArray organizers = organizerElement.getAsJsonArray();

          if ( organizers.size() == 1 ) {

            organizer = organizers.get(0).getAsJsonObject();
          } else if ( organizers.isEmpty() ) {

            LOG.warn("this json schema has a list for organizers that is empty ... ");
          } else {

            organizer = organizers.get(0).getAsJsonObject();
            LOG.warn("this json schema has a list for organization containing " + organizers.size());
            LOG.warn("using the first organizer in the list");
          }

        } else {

          organizer  = organizerElement.getAsJsonObject();

        }

        if ( organizer != null && organizer.has("name") ) {

          eb.setOrganizer(organizer.get("name").getAsString());

        }

      }

      eb.setUrl(eventJson.get("url").getAsString()); 
      eb.setLocation(lb.build());

      return eb;

    } catch (Exception e) {
  
      LOG.error("error creating event from schema event");
      LOG.error(eventJson.toString());
      LOG.error(e.getMessage());
      return null;

    }

  }

}
