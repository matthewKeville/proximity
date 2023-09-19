package keville.meetup;

import keville.HarUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Arrays;
import java.util.Base64;

import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;

public class MeetupHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupHarProcessor.class);

    public static String extractEventsJson(String harString,String targetUrl) {

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


      // Unescape json data
      //String eventJson = StringEscapeUtils.unescapeJson(eventJsonRaw);

      //return eventJson;
      return eventJsonRaw;
      

  }

}
