package keville.Eventbrite;

import keville.HarUtil;
import keville.Event;
import keville.EventStatusEnum;
import keville.EventBuilder;
import keville.LocationBuilder;
import keville.EventTypeEnum;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import net.lightbody.bmp.core.har.Har;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class EventbriteHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventbriteHarProcessor.class);


  public static List<Event> process(Har har) {
    List<String> eventIds = extractEventIds(HarUtil.harToString(har));
    List<Event> events = eventIds
      .stream()
      .distinct()
      .map(ei -> createEventFrom(ei))
      .filter(e -> e != null)
      .collect(Collectors.toList());
    return events;
  }


  /* transform local event format to Event object */
  private static Event createEventFrom(String eventId) {

    EventBuilder eb = new EventBuilder();
    eb.setEventId(eventId);

    LocationBuilder lb = new LocationBuilder();

    JsonObject eventJson = keville.Eventbrite.EventCache.get(eventId);
    if (eventJson == null) {
      LOG.error("error creating Event from eventbrite id : " + eventId + "\n\t unable to find eventJson in eventcache");
      return null;
    }

    if ( eventJson.has("name") ) {
      eb.setName(eventJson.getAsJsonObject("name").get("text").getAsString());
    }

    // description is deprecated , but preferred over nothing
    if (eventJson.has("summary")) {
      eb.setDescription(eventJson.get("summary").toString());
    } else if (eventJson.has("description")) {  
      JsonElement eventDescriptionJson = eventJson.getAsJsonObject("description").get("text");
      eb.setDescription(eventDescriptionJson.toString());
    }
    
    if ( eventJson.has("start") ) {
      JsonObject eventStartJson = eventJson.getAsJsonObject("start");
      String timestring = eventStartJson.get("utc").getAsString();
      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestring));
      eb.setStart(start);
    }

    if ( eventJson.has("venue") && !eventJson.get("venue").isJsonNull() ) {

      JsonObject venueJson = eventJson.get("venue").getAsJsonObject();

      if ( venueJson.has("name") ) {
        lb.setName(venueJson.get("name").getAsString());
      }

      if ( venueJson.has("latitude") && venueJson.has("longitude") ) {
        Double latitude  = Double.parseDouble(venueJson.get("latitude").getAsString());
        Double longitude = Double.parseDouble(venueJson.get("longitude").getAsString());
        lb.setLatitude(latitude);
        lb.setLongitude(longitude);
      }

      if ( venueJson.has("address") ) {

        JsonObject address = venueJson.getAsJsonObject("address");

        if ( address.has("country") ) {
            lb.setCountry(address.get("country").getAsString());
        }

        if ( address.has("region") ) {
            lb.setRegion(address.get("region").getAsString());
        }

        if ( address.has("city") ) {
            lb.setLocality(address.get("city").getAsString());
        }

      }

    } else {
      LOG.warn("unable to find venue data for this event json ... ");
      LOG.warn(eventJson.toString());
    }


    if (eventJson.has("organizer")) {
      JsonObject organizer = eventJson.get("organizer").getAsJsonObject();
      if ( organizer.has("name")) {
        JsonElement organizerNameElement = organizer.get("name");
        if ( !organizerNameElement.isJsonNull() ) {
          eb.setOrganizer(organizerNameElement.getAsString());
        }
      }
    }

    //intentional short circuit
    boolean virtual = eventJson.has("online_event") && eventJson.get("online_event").getAsString().equals("true");
    eb.setVirtual(virtual);

    if ( eventJson.has("url") ) {
      eb.setUrl(eventJson.get("url").getAsString());
    }

    eb.setLocation(lb.build());
    eb.setEventTypeEnum(EventTypeEnum.EVENTBRITE);
    eb.setStatus(EventStatusEnum.HEALTHY);

    return eb.build();

  }

  //this method is a bit inefficient, but does extract all event ids in the archive, including asynchronous ones
  public static List<String> extractEventIds(String harString) {

    List<String> eventIds = new ArrayList<String>();

      Pattern pat = Pattern.compile("(?<=eventbrite_event_id\\\\\":\\\\\").*?(?=\\\\\",\\\\\"start)"); //what an ungodly creation
      Matcher mat = pat.matcher(harString);
      while (mat.find()) {
        eventIds.add(mat.group());
      }

    return eventIds;

  }

}
