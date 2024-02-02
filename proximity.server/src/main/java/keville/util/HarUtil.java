package keville.util;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.LinkedList;

import java.io.PrintStream;
import java.io.File;
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
  public static List<JsonObject> findAllResponsesFromRequestUrl(String harString, String targetUrl) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    List<JsonObject> responses = new LinkedList<JsonObject>();

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if (requestUrl.equals(targetUrl)) {
        responses.add(entry.get("response").getAsJsonObject());
      }

    }

    if (responses.size() == 0) {
      LOG.warn("Unable to find any responses matching a request url : " + targetUrl);
    }
    return responses;

  }

  public static JsonObject findResponseFromRequestUrl(String harString, String targetUrl) {

    return findFirstResponseFromRequestUrl(harString, targetUrl, false);

  }

  public static JsonObject findFirstResponseFromRequestUrl(String harString, String targetUrl, boolean followRedirect) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    JsonObject response = null;

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if (requestUrl.equals(targetUrl)) {
        response = entry.get("response").getAsJsonObject();
        break;
      }

    }

    if (response == null) {
      LOG.warn("Unable to find a valid response for the request url : " + targetUrl);
      return null;
    }

    // was it a redirect?
    String redirectURL = response.get("redirectURL").getAsString();

    if (!redirectURL.isEmpty() && followRedirect) {

      LOG.debug("using redirect response");
      return findFirstResponseFromRequestUrl(harString, redirectURL, true);

    }

    return response;

  }

  // Unwrap a redirect url, or return the original if not redirected
  public static String getRedirectRequestUrlOrOriginal(String harString, String targetUrl) {

    JsonObject harJson = JsonParser.parseString(harString).getAsJsonObject();
    JsonArray entries = harJson.get("log").getAsJsonObject().get("entries").getAsJsonArray();
    JsonObject response = null;

    for (JsonElement jo : entries) {

      JsonObject entry = jo.getAsJsonObject();
      JsonObject request = entry.get("request").getAsJsonObject();
      String requestUrl = request.get("url").getAsString();

      if (requestUrl.equals(targetUrl)) {
        response = entry.get("response").getAsJsonObject();
        break;
      }

    }

    if (response == null) {
      return null;
    }

    String redirectURL = response.get("redirectURL").getAsString();
    if (redirectURL == null) {

      return targetUrl;

    } else {

      return redirectURL;

    }

  }

  // Write a HAR file to disk
  public static void saveHARtoLFS(Har har, String fileName) {

    try {

      File file = new File(fileName);
      file.getParentFile().mkdirs(); //create path to containing dir if not exists

      PrintStream filePrintStream = new PrintStream(new FileOutputStream(fileName));
      filePrintStream.print(harToString(har));
      filePrintStream.close();

    } catch (Exception e) {

      LOG.error("error trying to save HAR file to LFS");
      LOG.error(e.getMessage());

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

    // plain text
    if (!responseContent.has("encoding")) {
      LOG.debug("response was not encoded i.e. plain text");
      try {
        return responseContent.get("text").getAsString();
      } catch (Exception e) {
        LOG.error("unable to extract response text");
        LOG.error(e.getMessage());
        return null;
      }
    }

    String enc = responseContent.get("encoding").getAsString();
    if (enc.equals("base64")) {

      LOG.debug("Response was encoded with base64");
      String base64ResponseText = responseContent.get("text").getAsString();
      try {
        byte[] decodedBytes = Base64.getDecoder().decode(base64ResponseText);
        return Arrays.toString(decodedBytes);
      } catch (Exception e) {
        LOG.error("Unable to decode base64 response");
        LOG.error(e.getMessage());
        return null;
      }

    } else {

      LOG.error("This response is encoded with " + enc + " which is not currently");
      return null;

    }

  }

}
