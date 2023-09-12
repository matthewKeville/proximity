package keville.AllEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

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

    public static List<String> extractEventStubJsonPayloads(String harString) {

      List<String> eventJsonPayloads = new ArrayList<String>();
      
      try {
  
        //jq '.log.entries[] | select(.request.url=="https://www.facebook.com/tr/") | .request.postData.text'
        JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
        JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();

        // find entries communicating with facebook pixel

        for (JsonElement jo : entries) {

          JsonObject entry = jo.getAsJsonObject();
          JsonObject request = entry.get("request").getAsJsonObject();
          String requestUrl = request.get("url").getAsString();

          if ( requestUrl.equals("https://www.facebook.com/tr/")) {

            LOG.debug("found a web request to facebook");

            // extract postData entry

            JsonObject postData = request.get("postData").getAsJsonObject();
            String postDataText = postData.get("text").getAsString();

            // urlDecode

            String unencodedPostDataText = "";
            try {
              unencodedPostDataText = URLDecoder.decode(postDataText, StandardCharsets.UTF_8.toString() );
            } catch (Exception e) {
              LOG.error("unable to decode request postData text");
              LOG.error(e.getMessage());
              continue;
            }

            if ( unencodedPostDataText.equals("") ) {
              continue;
            }

            // select JSON-LD string
            
            final String regex = "(?<=JSON-LD]=\\[).*?(?=,\\{\\\"@context\\\":\\\"https:\\/\\/schema.org\\\",\\\"@type\\\":\\\"BreadcrumbList)";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher mat = pattern.matcher(unencodedPostDataText);

            try {
              if (mat.find()) {
                eventJsonPayloads.add(mat.group(0));
              } 
            } catch ( Exception e ) {
              LOG.error("error extracting JSON-LD from the unencoded post data text");
              LOG.error(e.getMessage());
            }

          }

        }

      } catch (Exception e ) {

        // assumptions not met

        LOG.error("unexpected har data");
        LOG.error(e.getMessage());

        try {
          PrintStream filePrintStream = new PrintStream(new FileOutputStream("logs/AllEvents-failure."+Instant.now().toString()+".har"));
          filePrintStream.print(harString);
          filePrintStream.close();
        } catch (Exception e2) {
          LOG.error("error trying to save HAR file to LFS");
        }

      }

      for ( String json : eventJsonPayloads ) {

        LOG.info("json start");
        LOG.info(json);
        LOG.info("json end");

      }

      return eventJsonPayloads;

  }

}
