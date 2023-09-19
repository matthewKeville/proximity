package keville.AllEvents;

import keville.HarUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Base64;

import com.google.gson.JsonObject;


public class AllEventsHarProcessor {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsHarProcessor.class);

    public static String extractEventStubsJson(String harString,String targetUrl) {

      String eventStubsJson = null;
      
      try {
 
        //select response content for initial web request
        
        JsonObject response = HarUtil.findResponseFromRequestUrl(harString,targetUrl);
        if ( response == null ) {

          LOG.warn("assumption not met, could not find  initial repsonse content aborting...");
          HarUtil.saveHARtoLFS(harString,"allevents-error.har");
          return null;
        }

        // did the inital request get redirected?
        String redirectURL = response.get("redirectURL").getAsString();

        if ( !redirectURL.isEmpty() ) {
          LOG.info("using redirect response");
          response = HarUtil.findResponseFromRequestUrl(harString,redirectURL);

          if ( response == null ) {
            LOG.info("assumption not met, could not find redirect response content aborting...");
            HarUtil.saveHARtoLFS(harString,"allevents-error.har");
            return null;
          }

        }

        // extract text content from the response ( may be base64 encoded )
       
        JsonObject responseContent = response.get("content").getAsJsonObject();
        String webpageData = "";

        // decode if necessary ( not sure when information is decoded, but I have seenn this content in plaintext and encoded )
  
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
          LOG.info("response was not encoded i.e. plain text");
          webpageData = responseContent.get("text").getAsString();
        }

        // select JSON-LD string (exists between script tags in the markup)

        //(?<=application\/ld\+json\">\n).*?(?=<\/script>)/gm
        final String regex = "(?<=application\\/ld\\+json\\\">\n).*?(?=<\\/script>)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher mat = pattern.matcher(webpageData);

        try {
          if (mat.find()) {

            eventStubsJson = mat.group(0);
            LOG.info("found event stub data in initial web response");

          } else {

            LOG.info("unable to locate stub data in JSON-LD with regex");
            HarUtil.saveHARtoLFS(harString,"allevents-error.har");
            return null;

          }
        } catch ( Exception e ) {

          LOG.error("error extracting JSON-LD from the unencoded post data text");
          LOG.error(e.getMessage());
          HarUtil.saveHARtoLFS(harString,"allevents-error.har");
          return null;

        }


      } catch (Exception e ) {

        LOG.error("unexpected har data");
        LOG.error(e.getMessage());
        HarUtil.saveHARtoLFS(harString,"allevents-error.har");
        return null;

      }

      if ( eventStubsJson == null ) {
        LOG.warn("no event stubs found");
        HarUtil.saveHARtoLFS(harString,"allevents-warn.har");
        return null;
      }

      return eventStubsJson;

  }


}
