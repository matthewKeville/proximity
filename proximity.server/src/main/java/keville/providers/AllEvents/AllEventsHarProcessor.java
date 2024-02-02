package keville.providers.AllEvents;

import keville.event.Event;
import keville.event.EventTypeEnum;
import keville.event.EventStatusEnum;
import keville.event.EventBuilder;
import keville.util.SchemaUtil;
import keville.util.HarUtil;
import keville.util.SchemaParseException;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import net.lightbody.bmp.core.har.Har;


public class AllEventsHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsHarProcessor.class);

    public static List<Event> process(Har har,String targetUrl,int pages) {

      List<Event> events = extractEventsFromStaticPages(HarUtil.harToString(har),targetUrl,pages);
      return events;

    }

    public static List<Event> extractEventsFromStaticPages(String harString,String targetUrl,int pages) {

      // find the documents containing event data

      String basePageRequest = HarUtil.getRedirectRequestUrlOrOriginal(harString,targetUrl);
      if ( basePageRequest == null ) {
        LOG.error("unable to retrieve the reponse information for the base page url with initial target " + targetUrl);
        return new LinkedList<Event>();
      }

      List<String> staticPageUrls = new LinkedList<String>();
      staticPageUrls.add(basePageRequest);

      for ( int i = 2; i <= pages; i++ ) {
        String pageUrl = basePageRequest+"?page="+i;
        staticPageUrls.add(pageUrl);
      }

      // process the documents into Event data

      List<Event> newEvents = new LinkedList<Event>();

      for ( String url : staticPageUrls ) {

        LOG.info("processing : " + url);

        // Find web response
          
        JsonObject response = HarUtil.findFirstResponseFromRequestUrl(harString,url,true);
        if ( response == null ) {
          LOG.warn("unable to find intial web response");
          return null;
        }

        // get the response data

        String webpageData = HarUtil.getDecodedResponseText(response);
        if ( webpageData == null ) {
          LOG.error("initial response data is empty");
        }

        // find the Schema Event Data

        JsonArray schemaEvents = extractJsonSchemaEventArray(webpageData);

        if ( schemaEvents == null ) {
          LOG.warn("no embedded schema events found");
        }

        // Transform Schema Event Data to domain Event

        for (JsonElement jo : schemaEvents ) {

          JsonObject jsonEvent = jo.getAsJsonObject();
          Event event = createEventStubFrom(jsonEvent);
          if ( event != null ) {
            newEvents.add(event);
          }

        }

      }

      return newEvents;

  }

  // finds the JsonSchema data embedded in the webpage markkup
  private static JsonArray extractJsonSchemaEventArray(String webPageData) {

        // select JSON-LD string (exists between script tags in the markup)
        //(?<=application\/ld\+json\">\n).*?(?=<\/script>)/gm
        final String regex = "(?<=application\\/ld\\+json\\\">\n).*?(?=<\\/script>)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher mat = pattern.matcher(webPageData);
        String eventJsonSchemaArrayString = "";

        try {

          if (mat.find()) {

            eventJsonSchemaArrayString = mat.group(0);
            LOG.debug("found event stub data in initial web response");

          } else {

            LOG.warn("unable to locate stub data in JSON-LD with regex");
            return null;

          }

          return JsonParser.parseString(eventJsonSchemaArrayString).getAsJsonArray();

        } catch ( Exception e ) {

          LOG.error("error extracting JSON-LD from the unencoded post data text");
          LOG.error(e.getMessage());
          return null;

        }

  }


  private static Event createEventStubFrom(JsonObject eventJson) {

    try {
      EventBuilder eb = SchemaUtil.createEventFromSchemaEvent(eventJson);
      eb.setEventTypeEnum(EventTypeEnum.ALLEVENTS);
      eb.setEventId(extractIdFromJson(eventJson)); 
      return eb.build();
    } catch (SchemaParseException ex) {
      LOG.error("caught SchemaParseException");
      LOG.error(ex.getMessage());
      return null;
    }

  }

  //https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097
  //assuming the last bit is the eventId
  private static String extractIdFromJson(JsonObject eventJson) {

    String url = eventJson.get("url").getAsString(); 

    String [] splits = url.split("/");
    if ( splits.length == 0 ) {
      LOG.error("could not extract event id from url");
    } 
    return splits[splits.length-1];
  }

}
