package keville.AllEvents;

import keville.Event;
import keville.EventBuilder;
import keville.SchemaUtil;
import keville.HarUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import net.lightbody.bmp.core.har.Har;

import keville.EventTypeEnum;

public class AllEventsHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsHarProcessor.class);

    public static List<Event> process(Har har,String targetUrl) {

      List<Event> events = extractEventsFromStaticPage(HarUtil.harToString(har),targetUrl);
      // TODO : extractEventsFromAjax
      return events;

    }

    public static List<Event> extractEventsFromStaticPage(String harString,String targetUrl) {

      // Find inital web response
        
      JsonObject response = HarUtil.findFirstResponseFromRequestUrl(harString,targetUrl,true);
      if ( response == null ) {

        LOG.warn("unable to find intial web response");
        HarUtil.saveHARtoLFS(harString,"allevents-error.har");
        return null;

      }

      // get the response data

      String webpageData = HarUtil.getDecodedResponseText(response);
      if ( webpageData == null ) {

        LOG.error("initial response data is empty");
        HarUtil.saveHARtoLFS(harString,"allevents-error.har");

      }

      // find the Schema Event Data

      JsonArray schemaEvents = extractJsonSchemaEventArray(webpageData);

      if ( schemaEvents == null ) {

        LOG.warn("no embedded schema events found");
        HarUtil.saveHARtoLFS(harString,"allevents-warn.har");
        return null;

      }

      // Transform Schema Event Data to domain Event

      List<Event> newEvents = new ArrayList<Event>();

      for (JsonElement jo : schemaEvents ) {

        JsonObject event = jo.getAsJsonObject();
        newEvents.add(createEventStubFrom(event));

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


  /* 
     this is a limited Event object that is missing other fields are
     required to really be an 'Event' such as Time (instead of just date) and a formal
     description. 

     For testing purposes, I will use this limited Event as an Event, while I run tests
     to see if full Event collection is feasible or desirable when the Event update protocol
     has yet to be formulated. The main reason why I am reticent to grab the full data is
     it would require a new web request per event which could seem suspicious to allEvents.in
  */
  private static Event createEventStubFrom(JsonObject eventJson) {

    EventBuilder eb = SchemaUtil.createEventFromSchemaEvent(eventJson);
    eb.setEventTypeEnum(EventTypeEnum.ALLEVENTS);
    eb.setEventId(extractIdFromJson(eventJson)); 

    return eb.build();

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
