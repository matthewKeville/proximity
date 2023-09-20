package keville;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.LinkedList;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.StringWriter;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import net.lightbody.bmp.core.har.Har;


public class HarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HarUtil.class);


  // does not follow redirect
  // does not check content
  public static List<JsonObject> findAllResponsesFromRequestUrl(String harString,String targetUrl) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    List<JsonObject> responses = new LinkedList<JsonObject>();

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if ( requestUrl.equals(targetUrl)) {
        responses.add(entry.get("response").getAsJsonObject());
      }

    }

    if ( responses.size() == 0 ) {
      LOG.warn("unable to find any responses matching a request url : " + targetUrl);
    }
    return responses;

  }

  public static JsonObject findResponseFromRequestUrl(String harString,String targetUrl) {

    return findFirstResponseFromRequestUrl(harString,targetUrl,false);

  }

  public static JsonObject findFirstResponseFromRequestUrl(String harString,String targetUrl,boolean followRedirect) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    JsonObject response = null;

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if ( requestUrl.equals(targetUrl)) {
        response = entry.get("response").getAsJsonObject();
        break;
      }

    }

    if ( response == null ) {
      LOG.warn("unable to find a valid response for the request url : " + targetUrl);
    }

    // was it a redirect?
    String redirectURL = response.get("redirectURL").getAsString();

    if ( !redirectURL.isEmpty() && followRedirect ) {

      LOG.debug("using redirect response");
      return findFirstResponseFromRequestUrl(harString,redirectURL,true);

    }

    return response;

  }

  //given a request url, return that url or the redirect that it responded with
  public static String getRedirectRequestUrlOrOriginal(String harString,String targetUrl) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    JsonObject response = null;

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if ( requestUrl.equals(targetUrl)) {
        response = entry.get("response").getAsJsonObject();
        break;
      }

    }

    if ( response == null ) {
      return null;
    }

    String redirectURL = response.get("redirectURL").getAsString();
    if ( redirectURL == null ) {

      return targetUrl;

    } else {

      return redirectURL;

    }

  }


  public static void saveHARtoLFS(String harString,String fileName) {

      try {

        PrintStream filePrintStream = new PrintStream(new FileOutputStream(fileName));
        filePrintStream.print(harString);
        filePrintStream.close();

      } catch (Exception e) {

        LOG.error("error trying to save HAR file to LFS");

      }

  }

  public static String harToString(Har har) {

      StringWriter harStringWriter = new StringWriter();

      try {
        har.writeTo(harStringWriter);
      } catch (Exception e) {
        LOG.error("unable to extract HAR data as string");
        LOG.error(e.getMessage());
      }

      return harStringWriter.toString();

  }

  public static String getDecodedResponseText(JsonObject response) {

      // extract text content from the response ( may be base64 encoded )
     
      JsonObject responseContent = response.get("content").getAsJsonObject();

      if ( responseContent.has("encoding") ) {

        String enc = responseContent.get("encoding").getAsString();

        if ( enc.equals("base64") ) {

          LOG.debug("response was encoded with base64, decoding...");

          String base64ResponseText = responseContent.get("text").getAsString();

          // decode base64

          try {

            byte[] decodedBytes = Base64.getDecoder().decode(base64ResponseText);
            return Arrays.toString(decodedBytes);

          } catch (Exception e) {

            LOG.error("unable to decode initial response entry from ");
            LOG.error(e.getMessage());
            return null;

          }

        } else {

          LOG.error("this response is encoded with " + enc + " which is not currently supported aborting...");
          return null;

        }

      } else {

        LOG.warn("response was not encoded i.e. plain text");
        return responseContent.get("text").getAsString();

      }

  }

}
