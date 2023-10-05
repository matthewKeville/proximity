package keville.providers.meetup;

import keville.event.Event;
import keville.event.EventTypeEnum;
import keville.event.EventStatusEnum;
import keville.event.EventBuilder;
import keville.location.LocationBuilder;
import keville.util.SchemaUtil;
import keville.util.HarUtil;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import net.lightbody.bmp.core.har.Har;


public class MeetupHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupHarProcessor.class);

    public static List<Event> process(Har har,String targetUrl) {

      List<Event> eventsEmbedded = extractEventsFromStaticPage(HarUtil.harToString(har),targetUrl);
      List<Event> eventsAjax = extractEventsFromAjax(HarUtil.harToString(har));

      LOG.info("found " + eventsEmbedded.size() + " embedded events");
      LOG.info("found " + eventsAjax.size()     + " ajax events");

      eventsEmbedded.addAll(eventsAjax);
      
      return eventsEmbedded;
    }

    public static List<Event> extractEventsFromStaticPage(String harString,String targetUrl) {

      // Find inital web response
        
      JsonObject response = HarUtil.findFirstResponseFromRequestUrl(harString,targetUrl,true);
      if ( response == null ) {

        LOG.warn("unable to find intial web response");
        HarUtil.saveHARtoLFS(harString,"meetup-error.har");
        return null;

      }


      // get response data

      String webpageData = HarUtil.getDecodedResponseText(response);
      if ( webpageData == null ) {

        LOG.error("initial response data is empty");
        HarUtil.saveHARtoLFS(harString,"meetup-error.har");
        return null;

      }


      // find the Schema Event Data

      JsonArray schemaEvents = extractJsonSchemaEventArray(webpageData);

      if ( schemaEvents == null ) {
        
        LOG.warn("no embedded schema events found");
        HarUtil.saveHARtoLFS(harString,"meetup-error.har");
      
      }

      List<Event> newEvents = new LinkedList<Event>();

      for (JsonElement jo : schemaEvents) {

        JsonObject event = jo.getAsJsonObject();
        newEvents.add(createEventFrom(event));

      }

      return newEvents;

  }  


  // find the Schema Json data embedded in the webpage markup
  private static JsonArray extractJsonSchemaEventArray(String webPageData) {

    // regex101 : "(?<=www.googletagmanager.com\"/><script type=\"application/ld\+json\">).*?(?=</script>)"gm
    final String regex = "(?<=www.googletagmanager.com\\\"/><script type=\\\"application/ld\\+json\\\">).*?(?=</script>)"; 
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    Matcher mat = pattern.matcher(webPageData);
    String eventJsonSchemaArrayString  = "";

      try {

        if (mat.find()) {

          eventJsonSchemaArrayString = mat.group(0);

        } else {

          LOG.warn("unable to find embedded event data");
          return null;

        }

        return JsonParser.parseString(eventJsonSchemaArrayString).getAsJsonArray();

      } catch (Exception e ) {

        LOG.error("unexpected har data");
        return null;

      }

  }


  private static Event createEventFrom(JsonObject eventJson) {

    EventBuilder eb = SchemaUtil.createEventFromSchemaEvent(eventJson);
    eb.setEventTypeEnum(EventTypeEnum.MEETUP);

    //I am assuming this last part is the eventId
    //https://www.meetup.com/monmouth-county-golf-n-sports-fans-social-networking/events/294738939/
    String url = eventJson.get("url").getAsString(); 
    String[] splits = url.split("/");
    String id = splits[splits.length-1];

    eb.setEventId(id); 
    eb.setStatus(EventStatusEnum.HEALTHY);

    return eb.build();
  }

  public static List<Event> extractEventsFromAjax(String harString) {

      final String apiUrl = "https://www.meetup.com/gql";
      List<Event> events = new LinkedList<Event>();

      List<JsonObject> responses = HarUtil.findAllResponsesFromRequestUrl(harString,apiUrl);

      LOG.info("found " + responses.size() + " /gql responses ");

      for (JsonObject resp : responses ) {

        String jsonRaw = HarUtil.getDecodedResponseText(resp);
        if ( jsonRaw == null ) {
          LOG.warn("the response content is null, skipping...");
          continue;
        }

        // sanity check to track assumptions of site api 

        if ( resp.get("content").getAsJsonObject().has("encoding") ) { //assumed to be  base64

          String encoding = resp.get("content").getAsJsonObject().get("encoding").getAsString();

          LOG.warn("unexpected response, found a response encoded with : " + encoding );
          LOG.warn("expected no encoding");

        // process response content of interest 

        } else {

          //gql event to domain Event

          JsonArray edges = extractEdgesFromRawGqLJson(jsonRaw);

          if ( edges == null )  {

            LOG.warn("failed to extract edges from response content");
            continue;

          }

          for ( JsonElement edgeElement : edges ) {

             JsonObject edge = edgeElement.getAsJsonObject(); 
             events.add(createEventFromGqlEventEdge(edge));

          }
          
        }

      }

      return events;

  }


  private static JsonArray extractEdgesFromRawGqLJson(String rawGqlJsonString) {

    JsonObject jsonData = JsonParser.parseString(rawGqlJsonString).getAsJsonObject();
   
    try {
      JsonArray edges = jsonData.get("data").getAsJsonObject().get("rankedEvents").getAsJsonObject().get("edges").getAsJsonArray();
      return edges;
    } catch (Exception e) {
      LOG.error("error trying to extract edges from raw json gql");
      LOG.error(e.getMessage());
      return null;
    }

  }

  private static Event createEventFromGqlEventEdge(JsonObject eventEdge) {

    EventBuilder eb = new EventBuilder();
    LocationBuilder lb = new LocationBuilder();

    JsonObject node = eventEdge.get("node").getAsJsonObject();

    if (node.has("id")) {
      eb.setEventId(node.get("id").getAsString());
    }

    if (node.has("dateTime")) { // start
      String timestring = node.get("dateTime").getAsString();
      //2023-09-27T12:00-04:00 (from gql : iso offset time)
      Instant start  = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestring));
      eb.setStart(start);
    }

    if (node.has("title")) {
      eb.setName(node.get("title").getAsString());
    }

    if (node.has("description")) {
      eb.setName(node.get("description").getAsString());
    }

    if (node.has("venue")) {
      JsonElement venueElement = node.get("venue");

      if ( !venueElement.isJsonNull() ) {

        JsonObject venue= venueElement.getAsJsonObject();

        if ( venue.has("city") ) {
          //Spring Lake
          lb.setLocality(venue.get("city").getAsString());
        }
        if ( venue.has("state") ) {
          lb.setRegion(venue.get("state").getAsString());
          //nj
        }
        if ( venue.has("country") ) {
          lb.setCountry(venue.get("country").getAsString());
        }

        if ( venue.has("lat") && venue.has("lng")) {
          lb.setLatitude(venue.get("lat").getAsDouble());
          lb.setLongitude(venue.get("lng").getAsDouble());
        } 

        if ( venue.has("name") ) {
          lb.setName(venue.get("name").getAsString());
        }

      }
    }

    if ( node.has("eventType") ) {
      boolean virt = !node.get("eventType").getAsString().equals("PHYSICAL");
      eb.setVirtual(virt);
    }

    if ( node.has("eventUrl") ) {
      eb.setUrl(node.get("eventUrl").getAsString());
    }

    eb.setLocation(lb.build()); 
    eb.setEventTypeEnum(EventTypeEnum.MEETUP);
    eb.setStatus(EventStatusEnum.HEALTHY);

    return eb.build();
  }


}
