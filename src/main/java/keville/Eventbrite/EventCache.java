package keville.Eventbrite;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

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

public class EventCache {

  private static String eventBaseUri = "https://www.eventbriteapi.com/v3/events/";
  private static String cacheFilePath = "./.eventCache.json";

  private Map<String,JsonObject> events;
  private HttpClient httpClient;
  private String BEARER_TOKEN;

  public EventCache (Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();
    events = new HashMap<String,JsonObject>();
    loadCacheFromFile();
  }

  public JsonObject get(String eventId) {
    JsonObject eventJson = null;
    if (events.containsKey(eventId)) {
      eventJson = events.get(eventId);
    } else {
      System.out.println(String.format("local miss on event %s",eventId));
      eventJson = getEventFromApi(eventId);
      if (eventJson != null ) {
        events.put(eventId,eventJson);
      } else {
        System.err.println("eventbrite api generated a null eventjson");
      }
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
  private JsonObject getEventFromApi(String eventId) {

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
