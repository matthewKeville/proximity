package keville.Eventbrite;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Iterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VenueCache {

  private static String venueBaseUri = "https://www.eventbriteapi.com/v3/venues/";
  private static String cacheFilePath = "./.venueCache.json";

  private Map<String,JsonObject> venues;
  private HttpClient httpClient;
  private String BEARER_TOKEN;

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VenueCache.class);

  public VenueCache(Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();
    venues = new HashMap<String,JsonObject>();
    loadCacheFromFile();
  }

  public JsonObject get(String venueId) {
    /* is this in the cache?  or do we need to retrieve it ? */
    JsonObject venueJson = null;
    if (venues.containsKey(venueId)) {
      LOG.info(String.format("local hit on venue %s",venueId));
      venueJson = venues.get(venueId);
    } else {
      LOG.info(String.format("local miss on venue %s",venueId));
      venueJson = getVenue(venueId);
      venues.put(venueId,venueJson);
    }
    return venueJson;
  }

  /* load cached venues if any */
  private void loadCacheFromFile() {
    LOG.info("loading local venue cache");
    File cacheFile = new File(cacheFilePath);
    if (!cacheFile.exists() ) {
      LOG.info("no local venue cache found");
      return;
    }
    LOG.info("found local venue cache");
    Scanner cacheFileScanner;
    String json = "";
    try {
      cacheFileScanner = new Scanner(cacheFile);
      while (cacheFileScanner.hasNextLine()) {
        json+=cacheFileScanner.nextLine(); 
      }
    } catch (Exception e) {
      LOG.error("Error reading venue cache file :" + e.getMessage());
    }
    
    /* populate venue map */
    if (!json.equals("")) {
      JsonObject venueJsonList = JsonParser.parseString(json).getAsJsonObject();
      JsonArray venuesArray = venueJsonList.getAsJsonArray("venues");
      LOG.info(String.format("found %d venues in cache",venuesArray.size()));
      for (JsonElement jo : venuesArray) {
        JsonObject venue = jo.getAsJsonObject();
        String venueIdString = venue.get("id").getAsString();
        venues.put(venueIdString,venue);
      }
    }

    return;
  }

  private void saveCacheToFile() {
    LOG.info("saving local venue cache");
    File cacheFile = new File(cacheFilePath);
    FileWriter fileWriter;
    try {
      cacheFile.createNewFile(); /*creates if not extant*/
      fileWriter = new FileWriter(cacheFile);
      fileWriter.write("{\n\"venues\": [\n");
      Iterator<JsonObject> itty = venues.values().iterator();
      while ( itty.hasNext() ) {
        JsonObject jo = itty.next();
        fileWriter.write(jo.toString());
        if (itty.hasNext()) {
          fileWriter.write(",");
        }
        fileWriter.write(System.lineSeparator());
      }
      fileWriter.write("]");
      fileWriter.write("\n}");
      fileWriter.flush();
      fileWriter.close();
    } catch (IOException e) {
      LOG.error("error writing to file " + e.getMessage());
    }
    return;
  }

  /* Get venue data from venue Brite API */
  private JsonObject getVenue(String venueId) {

    HttpRequest getRequest;
    JsonObject venueJson = null;

    try {
    URI uri = new URI(venueBaseUri+venueId+"/"); /*without terminal '/' we get a 301 */
    getRequest = HttpRequest.newBuilder()
      .uri(uri) 
      .header("Authorization","Bearer "+BEARER_TOKEN)
      .GET()
      .build();
    } catch (URISyntaxException e) {
      LOG.error(String.format("error building request: %s",e.getMessage()));
      return null;
    }

    try {
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      LOG.info(String.format("request returned %d",getResponse.statusCode()));
      venueJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }

    return venueJson;

  }

  public void notifyTermination() {
    saveCacheToFile();
  }

}
