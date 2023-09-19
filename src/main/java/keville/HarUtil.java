package keville;

import java.io.PrintStream;
import java.io.FileOutputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;


public class HarUtil {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HarUtil.class);

  public static JsonObject findResponseFromRequestUrl(String harString,String targetUrl) {

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

    return response;

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

}
