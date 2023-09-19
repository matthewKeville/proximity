package keville.meetup;

import keville.Event;
import keville.EventBuilder;
import keville.SchemaUtil;
import keville.HarUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Arrays;
import java.util.Base64;

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

      String eventJsonRaw = "";

      try {

        //select response content for initial web request
        
        JsonObject response = HarUtil.findResponseFromRequestUrl(harString,targetUrl);
        if ( response == null ) {

          LOG.warn("unable to locate target response data");
          HarUtil.saveHARtoLFS(harString,"meetup-error.har");
          return null;

        }

        // did the inital request get redirected?
        String redirectURL = response.get("redirectURL").getAsString();

        if ( !redirectURL.isEmpty() ) {
          LOG.info("using redirect response");
          response = HarUtil.findResponseFromRequestUrl(harString,redirectURL);

          if ( response == null ) {

            LOG.info("assumption not met, could not find redirect response content aborting...");
            HarUtil.saveHARtoLFS(harString,"meetup-error.har");
            return null;

          }

        }

        // decode (base64) text content from the response 
       
        JsonObject responseContent = response.get("content").getAsJsonObject();
        String webpageData = "";

        if ( responseContent.has("encoding") ) {
          String enc = responseContent.get("encoding").getAsString();

          if ( enc.equals("base64") ) {

            LOG.info("response was encoded with base64, decoding...");

            String base64ResponseText = responseContent.get("text").getAsString();

            // decode base64

            try {
              byte[] decodedBytes = Base64.getDecoder().decode(base64ResponseText);
              webpageData = Arrays.toString(decodedBytes);
            } catch (Exception e) {
              LOG.error("unable to decode initial response entry from " + targetUrl);
              LOG.error(e.getMessage());
              HarUtil.saveHARtoLFS(harString,"allevents-error.har");
              return null;
            }

          } else {

            LOG.error("this response is encoded with " + enc + " which is not currently supported aborting...");
            HarUtil.saveHARtoLFS(harString,"allevents-error.har");
            return null;

          }

        } else {

          LOG.warn("this response is in plain-text, skipping decoding");
          webpageData  = responseContent.get("text").getAsString();
          LOG.info("webpagedata");
          LOG.info(webpageData);
          LOG.info("webpagedata");

        }

        //  select the ld+json data  

        // regex101 : "(?<=www.googletagmanager.com\"/><script type=\"application/ld\+json\">).*?(?=</script>)"gm
        final String regex = "(?<=www.googletagmanager.com\\\"/><script type=\\\"application/ld\\+json\\\">).*?(?=</script>)"; 
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher mat = pattern.matcher(webpageData);

        if (mat.find()) {

          eventJsonRaw=mat.group(0);
          LOG.info("found embedded event data");

        } else {

          LOG.warn("unable to find embedded event data");
          //note web page data , not HAR
          HarUtil.saveHARtoLFS(webpageData,"meetup-error.html");
          return null;

        }
      } catch (Exception e ) {

        LOG.error("unexpected har data");
        HarUtil.saveHARtoLFS(harString,"meetup-error.har");
        return null;

      }
    
      List<Event> newEvents = new ArrayList<Event>();

      JsonArray eventsArray = JsonParser.parseString(eventJsonRaw).getAsJsonArray();

      for (JsonElement jo : eventsArray) {

        JsonObject event = jo.getAsJsonObject();
        newEvents.add(createEventFrom(event));

      }

      return newEvents;

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
