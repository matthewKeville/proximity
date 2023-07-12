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

public class EventLocator {

  private static String eventBaseUri = "https://www.eventbriteapi.com/v3/events/";
  private static String cacheFilePath = "./.eventCache.json";

  private Map<String,JsonObject> events;
  private HttpClient httpClient;
  private String BEARER_TOKEN;

  public EventLocator(Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();
    events = new HashMap<String,JsonObject>();
    loadCacheFromFile();
  }

  public List<JsonObject> getAllKnownEvents() {
    return (List<JsonObject>) new ArrayList<JsonObject>(events.values()); 
  }

  public JsonObject locateEvent(String eventId) {
    /* is this in the cache?  or do we need to retrieve it ? */
    JsonObject eventJson = null;
    if (events.containsKey(eventId)) {
      System.out.println(String.format("local hit on event %s",eventId));
      eventJson = events.get(eventId);
    } else {
      System.out.println(String.format("local miss on event %s",eventId));
      eventJson = getEvent(eventId);
      events.put(eventId,eventJson);
    }
    return eventJson;
  }

  /* load cached events if any */
  private void loadCacheFromFile() {
    System.out.println("loading local cache");
    File cacheFile = new File(cacheFilePath);
    if (!cacheFile.exists() ) {
      System.out.println("no local cache found");
      return;
    }
    System.out.println("found local cache");
    Scanner cacheFileScanner;
    String json = "";
    try {
      cacheFileScanner = new Scanner(cacheFile);
      while (cacheFileScanner.hasNextLine()) {
        json+=cacheFileScanner.nextLine(); 
      }
    } catch (Exception e) {
      System.out.println("Error reading event cache file :" + e.getMessage());
    }
    
    /* populate event map */
    if (!json.equals("")) {
      JsonObject eventJsonList = JsonParser.parseString(json).getAsJsonObject();
      JsonArray eventsArray = eventJsonList.getAsJsonArray("events");
      System.out.println(String.format("found %d events in cache",eventsArray.size()));
      for (JsonElement jo : eventsArray) {
        JsonObject event = jo.getAsJsonObject();
        String eventIdString = event.get("id").getAsString();
        //String eventIdString = event.get("id").toString();
        //System.out.println("found id : " + eventIdString);
        //eventIdString = eventIdString.substring(1,eventIdString.length()-1); //strip literal "", "32432" becomes 32432
        events.put(eventIdString,event);
      }
    }

    return;
  }

  private void saveCacheToFile() {
    System.out.println("saving local cache");
    File cacheFile = new File(cacheFilePath);
    FileWriter fileWriter;
    try {
      cacheFile.createNewFile(); /*creates if not extant*/
      fileWriter = new FileWriter(cacheFile);
      fileWriter.write("{\n\"events\": [\n");
      Iterator<JsonObject> itty = events.values().iterator();
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

  /* Get event data from Event Brite API */
  private JsonObject getEvent(String eventId) {

    HttpRequest getRequest;
    JsonObject eventJson = null;

    try {
    URI uri = new URI(eventBaseUri+eventId+"/"); /*without terminal '/' we get a 301 */
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
      eventJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      System.out.println(String.format("error sending request %s",e.getMessage()));
    }

    return eventJson;

  }

  public void notifyTermination() {
    saveCacheToFile();
  }

}
