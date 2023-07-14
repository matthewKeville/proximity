package keville;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner; /*why is this convention over FileReader?*/
import java.io.IOException;
import java.util.List;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class EventCache {

  private static String cacheFilePath = "./.genericEventCache.json";

  private Map<String,Event> events;
  private Gson gson;

  public EventCache() {
    events = new HashMap<String,Event>();

    //https://stackoverflow.com/questions/22310143/java-8-localdatetime-deserialized-using-gson
    //https://howtodoinjava.com/gson/gson-typeadapter-for-inaccessibleobjectexception/
    class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

      private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

      @Override
      public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
          final JsonSerializationContext context) {
        return new JsonPrimitive(date.format(formatter));
      }

      @Override
      public LocalDateTime deserialize(final JsonElement json, final Type typeOfT,
          final JsonDeserializationContext context) throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(), formatter);
      }
    }
    gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();

    loadCacheFromFile();

  }

  public List<Event> getAll() {
    return new ArrayList<Event>(events.values());
  }

  public Event get(String eventId) {
    return events.get(eventId);
  }

  public boolean has(String eventId) {
    return events.containsKey(eventId);
  }

  public void add(Event event) {
    events.put(event.id,event);
  }

  public void addAll(List<Event> newEvents) {
    newEvents.stream()
      .forEach(ev -> events.put(ev.id,ev));
  }

  /* load cached events if any */
  private void loadCacheFromFile() {

    File cacheFile = new File(cacheFilePath);
    if (!cacheFile.exists() ) {
      System.out.println("no local cache found");
      return;
    }

    // read event file data
    System.out.println("found local event cache");
    Scanner cacheFileScanner;
    String json = "";
    try {
      cacheFileScanner = new Scanner(cacheFile);
      while (cacheFileScanner.hasNextLine()) {
        json+=cacheFileScanner.nextLine(); 
      }
    } catch (Exception e) {
      System.out.println("error reading event cache file :" + e.getMessage());
    }
    
    // populate event map
    if (!json.equals("")) {

      Type type = new TypeToken<List<Event>>() {}.getType(); 

      List<Event> eventList = gson.fromJson(json,type);

      for (Event event : eventList) {
        events.put(event.id,event);
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

      List<Event> eventList = new ArrayList<Event>(events.values());
      Type type = new TypeToken<List<Event>>() {}.getType(); 

      String jsonString = gson.toJson(eventList,type);
      fileWriter.write(jsonString);

      fileWriter.flush();
      fileWriter.close();

    } catch (IOException e) {
      System.out.println("error writing to file " + e.getMessage());
    }
    return;
  }

  public void notifyTermination() {
    saveCacheToFile();
  }

}
