package keville.meetup;

import keville.Event;
import keville.EventBuilder;
import keville.SchemaUtil;
import keville.HarUtil;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import net.lightbody.bmp.core.har.Har;

import keville.EventTypeEnum;

public class MeetupHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupHarProcessor.class);

    public static List<Event> process(Har har,String targetUrl) {
      List<Event> events = extractEventsFromStaticPage(HarUtil.harToString(har),targetUrl);
      //  TODO : extractEventsFromAjax 
      return events;
    }


    public static List<Event> extractEventsFromStaticPage(String harString,String targetUrl) {

      // Find inital web response
        
      JsonObject response = HarUtil.findResponseFromRequestUrl(harString,targetUrl,true);
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

  // finds the JsonSchema data embedded in the webpage markup
  private static JsonArray extractJsonSchemaEventArray(String webPageData) {

    // regex101 : "(?<=www.googletagmanager.com\"/><script type=\"application/ld\+json\">).*?(?=</script>)"gm
    final String regex = "(?<=www.googletagmanager.com\\\"/><script type=\\\"application/ld\\+json\\\">).*?(?=</script>)"; 
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    Matcher mat = pattern.matcher(webPageData);
    String eventJsonSchemaArrayString  = "";

      try {

        if (mat.find()) {

          eventJsonSchemaArrayString = mat.group(0);
          LOG.info("found embedded event data");

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

    return eb.build();
  }


}
