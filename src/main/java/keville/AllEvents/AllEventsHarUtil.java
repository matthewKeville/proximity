package keville.AllEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Base64;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.net.URLDecoder;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;


public class AllEventsHarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsHarUtil.class);

    public static String extractEventStubsJson(String harString,String targetUrl) {

      String eventStubsJson = null;
      boolean error = false;
      
      try {
 
        //select response content for initial web request
        
        JsonObject response = findResponseFromRequestUrl(harString,targetUrl);
        if ( response == null ) {
          LOG.warn("assumption not met, could not find  initial repsonse content aborting...");
          return null;
        }

        // did the inital request get redirected?
        String redirectURL = response.get("redirectURL").getAsString();
        LOG.info("redirectURL : " + redirectURL );
        if ( !redirectURL.isEmpty() ) {
          LOG.info("using redirect response");
          response = findResponseFromRequestUrl(harString,redirectURL);

          if ( response == null ) {
            LOG.info("assumption not met, could not find redirect response content aborting...");
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
              LOG.info("coded webpage");
              LOG.info(base64ResponseText);
              LOG.info("coded webpage");
              byte[] decodedBytes = Base64.getDecoder().decode(base64ResponseText);
              webpageData = Arrays.toString(decodedBytes);
            } catch (Exception e) {
              LOG.error("unable to decode initial response entry from " + targetUrl);
              LOG.error(e.getMessage());
            }

          } else {
            LOG.error("this response is encoded with " + enc + " which is not currently supported aborting...");
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
          }
        } catch ( Exception e ) {
          LOG.error("error extracting JSON-LD from the unencoded post data text");
          LOG.error(e.getMessage());
        }


      } catch (Exception e ) {

        // assumptions not met
        error = true;

        LOG.error("unexpected har data");
        LOG.error(e.getMessage());
        saveHARtoLFS("error",harString);

      }

      if ( !error && eventStubsJson == null ) {
        LOG.warn("this is unexpected behaviour, could not identify event stub location");
        saveHARtoLFS("warning",harString);
      }

      return eventStubsJson;

  }

  private static JsonObject findResponseFromRequestUrl(String harString,String targetUrl) {

    //jq '.log.entries[] | select(.request.url == "https://allevents.in/brooklyn/all")  | .response.content.text'
    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    JsonObject response = null;

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      LOG.info("url : " + requestUrl);
      if ( requestUrl.equals(targetUrl)) {
        response = entry.get("response").getAsJsonObject();
        LOG.info("found the response matching the request url : " + targetUrl);
        break;
      }

    }

    if ( response == null ) {
      LOG.warn("unable to find a valid response for the request url : " + targetUrl);
    }

    return response;

  }

  private static void saveHARtoLFS(String reason,String harString) {

      try {
        PrintStream filePrintStream = new PrintStream(new FileOutputStream("logs/AllEvents-"+reason+"-"+Instant.now().toString()+".har"));
        filePrintStream.print(harString);
        filePrintStream.close();
      } catch (Exception e2) {
        LOG.error("error trying to save HAR file to LFS");
      }

  }

}
