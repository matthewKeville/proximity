package keville;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import java.util.Iterator;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner; /*why is this convention over FileReader?*/
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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

public class VenueLocator {

  private static String venueBaseUri = "https://www.eventbriteapi.com/v3/venues/";
  private static String cacheFilePath = "./.venueCache.json";

  private Map<String,JsonObject> venues;
  private HttpClient httpClient;
  private String BEARER_TOKEN;

  public VenueLocator(Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();
    venues = new HashMap<String,JsonObject>();
    loadCacheFromFile();
  }

  public List<JsonObject> getAllKnownVenues() {
    return (List<JsonObject>) new ArrayList<JsonObject>(venues.values()); 
  }

  public JsonObject locateVenue(String venueId) {
    /* is this in the cache?  or do we need to retrieve it ? */
    JsonObject venueJson = null;
    if (venues.containsKey(venueId)) {
      System.out.println(String.format("local hit on venue %s",venueId));
      venueJson = venues.get(venueId);
    } else {
      System.out.println(String.format("local miss on venue %s",venueId));
      venueJson = getVenue(venueId);
      venues.put(venueId,venueJson);
    }
    return venueJson;
  }

  /* load cached venues if any */
  private void loadCacheFromFile() {
    System.out.println("loading local venue cache");
    File cacheFile = new File(cacheFilePath);
    if (!cacheFile.exists() ) {
      System.out.println("no local venue cache found");
      return;
    }
    System.out.println("found local venue cache");
    Scanner cacheFileScanner;
    String json = "";
    try {
      cacheFileScanner = new Scanner(cacheFile);
      while (cacheFileScanner.hasNextLine()) {
        json+=cacheFileScanner.nextLine(); 
      }
    } catch (Exception e) {
      System.out.println("Error reading venue cache file :" + e.getMessage());
    }
    
    /* populate venue map */
    if (!json.equals("")) {
      JsonObject venueJsonList = JsonParser.parseString(json).getAsJsonObject();
      JsonArray venuesArray = venueJsonList.getAsJsonArray("venues");
      System.out.println(String.format("found %d venues in cache",venuesArray.size()));
      for (JsonElement jo : venuesArray) {
        JsonObject venue = jo.getAsJsonObject();
        String venueIdString = venue.get("id").getAsString();
        venues.put(venueIdString,venue);
      }
    }

    return;
  }

  private void saveCacheToFile() {
    System.out.println("saving local venue cache");
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
      System.out.println("error writing to file " + e.getMessage());
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
      System.out.println(String.format("error building request: %s",e.getMessage()));
      return null;
    }

    try {
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      System.out.println(String.format("request returned %d",getResponse.statusCode()));
      venueJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      System.out.println(String.format("error sending request %s",e.getMessage()));
    }

    return venueJson;

  }

  public void notifyTermination() {
    saveCacheToFile();
  }

}
