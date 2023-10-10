package keville.settings;

import keville.event.Event;
import keville.event.EventTypeEnum;
import keville.util.GeoUtils;
import keville.scanner.ScanRoutine;
import keville.compilers.EventCompiler;
import keville.compilers.RSSCompiler;
import keville.compilers.ICalCompiler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.time.Instant;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class Settings {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Settings.class);

  static final int EVENTBRITE_MAX_PAGES_DEFAULT = 8;
  static final int ALLEVENTS_MAX_PAGES_DEFAULT = 8;

  public String dbConnectionString;
  public String eventbriteApiKey;
  public int eventbriteMaxPages;
  public int alleventsMaxPages;
  public Map<String,ScanRoutine> scanRoutines;
  public Map<String,Predicate<Event>> filters;
  public List<EventCompiler> eventCompilers;

  public static Settings parseSettings(String jsonString) throws Exception {

    Settings settings = new Settings();
    JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

    settings.dbConnectionString  = "jdbc:sqlite:app.db";
    if  ( json.has("db_connection_string")) {
      settings.dbConnectionString  = json.get("db_connection_string").getAsString();
    }

    if ( json.has("eventbrite_api_key") ) {
      settings.eventbriteApiKey  = json.get("eventbrite_api_key").getAsString();
    }

    settings.eventbriteMaxPages = json.has("evenbrite_max_pages")  ? json.get("eventbrite_max_pages").getAsInt() : EVENTBRITE_MAX_PAGES_DEFAULT;
    settings.alleventsMaxPages = json.has("allevents_max_pages")  ? json.get("allevents_max_pages").getAsInt() : ALLEVENTS_MAX_PAGES_DEFAULT;

    // routines

    if ( !json.has("routines") ) {
      LOG.warn("no routines where found in the configuration file, generating default scan");
      settings.scanRoutines = new HashMap<String,ScanRoutine>();
      settings.scanRoutines.put("Default",ScanRoutine.createDefault());
    } else {
      settings.scanRoutines = parseScanRoutines(json.get("routines").getAsJsonArray());
    }

    // custom filters

    if ( !json.has("filters") ) {
      LOG.warn("no filters where found in the configuration file");
      settings.filters = new HashMap<String,Predicate<Event>>();
    } else {
      settings.filters = parseEventFilters(json.get("filters").getAsJsonArray());
    }
    Filters.registerCustomFilters(settings.filters);

    // compilers

    if ( !json.has("compilers") ) {
      LOG.warn("no compilers where found in the configuration file");
      settings.eventCompilers = new LinkedList<EventCompiler>();
    } else {
      settings.eventCompilers = parseEventCompilers(json.get("compilers").getAsJsonArray());
    }

    return settings;

  }

  public static Map<String,ScanRoutine> parseScanRoutines(JsonArray scans) throws Exception {

    Map<String,ScanRoutine> scanRoutineMap = new HashMap<String,ScanRoutine>();

    for ( JsonElement scan : scans ) {
      ScanRoutine routine  = parseScanRoutine(scan.getAsJsonObject());
      if ( scanRoutineMap.containsKey(routine.name) ) {
          LOG.warn("Scan routine names must be unique! But found " + routine.name + " more than once");
      }
      scanRoutineMap.put(routine.name,routine);
    }

    return scanRoutineMap;

  }

  public static ScanRoutine parseScanRoutine(JsonObject scanJson) throws Exception {

    ScanRoutine scanRoutine = new ScanRoutine();
    scanRoutine.types = new HashSet<EventTypeEnum>();

    if ( !scanJson.has("radius") ) {
      throw new Exception("Invalid scan scanRoutine , you must set a \"radius\"");
    }
    scanRoutine.radius = scanJson.get("radius").getAsDouble();

    if ( scanJson.has("auto") && scanJson.get("auto").getAsBoolean() ) {

      Map<String,Double> coords = GeoUtils.getClientGeolocation();
      scanRoutine.latitude = coords.get("latitude");
      scanRoutine.longitude = coords.get("longitude");

    } else {

      if ( !scanJson.has("latitude")  || !scanJson.has("longitude") ) {
        throw new Exception("Invalid scan scanRoutine , must set \"auto\" or \"latitude\" and \"longitude\"");
      }

      scanRoutine.latitude = scanJson.get("latitude").getAsDouble();
      scanRoutine.longitude = scanJson.get("longitude").getAsDouble();

    }

    scanRoutine.delay = scanJson.get("delay").getAsInt();

    if ( scanJson.has("meetup")     && scanJson.get("meetup").getAsBoolean() ) {
        scanRoutine.types.add(EventTypeEnum.MEETUP);
    }
    if ( scanJson.has("allevents")     && scanJson.get("allevents").getAsBoolean() ) {
        scanRoutine.types.add(EventTypeEnum.ALLEVENTS);
    }
    if ( scanJson.has("eventbrite")     && scanJson.get("eventbrite").getAsBoolean() ) {
        scanRoutine.types.add(EventTypeEnum.EVENTBRITE);
    }

    scanRoutine.runOnRestart =  scanJson.has("run_on_restart") && scanJson.get("run_on_restart").getAsBoolean();
    scanRoutine.lastRan = scanRoutine.runOnRestart ? Instant.EPOCH : Instant.now();

    scanRoutine.name =  scanJson.has("name") ? scanJson.get("name").getAsString()  : "";

    scanRoutine.disabled =  scanJson.has("disabled") && scanJson.get("disabled").getAsBoolean();

    return scanRoutine;

  }

  /*

    {
        type : "rss" | "ical",
        path : "/path/to/the/result",
        conjunctive : true,
        filters : [{}]
    }

  */

  private static List<EventCompiler> parseEventCompilers(JsonArray compilersJson) {

    List<EventCompiler> eventCompilers = new LinkedList<EventCompiler>();

    for ( JsonElement jsonElm : compilersJson ) {

      JsonObject compilerJson = jsonElm.getAsJsonObject();

      //parse top fields
      if ( !compilerJson.has("name") ) {
        LOG.warn("you have a misconfigured compiler in your settings.json");
        LOG.warn("you must provide a \"name\" for  the compiler to build to");
        continue;
      }
      String name = compilerJson.get("name").getAsString();
      
      if ( !compilerJson.has("path") ) {
        LOG.warn(name + " compiler is misconfigured");
        LOG.warn("you must provide a \"path\" for  the compiler to build to");
        continue;
      }

      String pathString = compilerJson.get("path").getAsString();
      try {
        Files.createDirectories(Paths.get(pathString).getParent()); //mkdir -p
      }  catch (Exception e ) {
        LOG.error("unable to create directory path for file " + pathString);
      }
      File file = new File(pathString);

      if ( !compilerJson.has("type") ) {
        LOG.warn(name + " compiler is misconfigured");
        LOG.warn("you must supply a \"type\"");
      }

      String typeString = compilerJson.get("type").getAsString();

      if ( !compilerJson.has("conjunction") ) {
        LOG.warn("the compiler " + name + " does not specify a conjunction value, assuming true");
      }
      boolean conjunction = !compilerJson.has("conjunction") || compilerJson.get("conjunction").getAsBoolean();

      // parse filter chain

      if ( !compilerJson.has("filter") ) {
        LOG.error("the compiler " + name + " does not specify any filters ");
        continue;
      }

      JsonArray jsonFilterChain = compilerJson.get("filters").getAsJsonArray();
      Predicate<Event> filter = Filters.parseFilterChain(jsonFilterChain, conjunction);

      //  construct compiler

      if (typeString.equals("rss")) {

        eventCompilers.add(new RSSCompiler(name,filter,file));

      } else if (typeString.equals("ical")) {

        eventCompilers.add(new ICalCompiler(name,filter,file));

      } else {

        LOG.warn(name + " compiler is misconfigured");
        LOG.error("unknown compiler type : " + typeString);
        
      }

    }

    return eventCompilers;

  }

  private static Map<String,Predicate<Event>> parseEventFilters(JsonArray filtersJson) {

      Map<String,Predicate<Event>> eventFilters = new HashMap<String,Predicate<Event>>();

      for ( JsonElement jsonElm : filtersJson ) {

        JsonObject filterJson = jsonElm.getAsJsonObject();

        if ( !filterJson.has("name") ) {
          LOG.warn("you have a misconfigured filter in your settings.json");
          LOG.warn("you must provide a name for  the filter to build to");
          continue;
        }
        String name = filterJson.get("name").getAsString();
        
        if ( !filterJson.has("conjunction") ) {
          LOG.warn("the filter " + name + " does not specify a conjunction value, assuming true");
        }
        boolean conjunction = !filterJson.has("conjunction") || filterJson.get("conjunction").getAsBoolean();

        if ( !filterJson.has("filters") )  {
          LOG.error(name + " has no filters");
          continue;
        } 

        JsonArray jsonFilterChain = filterJson.get("filters").getAsJsonArray();
        Predicate<Event> filter = Filters.parseFilterChain(jsonFilterChain, conjunction);

        eventFilters.put(name,filter);

      }

      return eventFilters;

    }

  public String toString() {

    String result = "\ndbConnectionString : " + dbConnectionString;

    result += "\neventbriteApiKey : " + eventbriteApiKey;
    result += "\neventbriteMaxPages : " + eventbriteMaxPages;
    result += "\nalleventsMaxPages : " + alleventsMaxPages;

    result += "\n Scan Routines : " +  scanRoutines.size() + "\n";

    Iterator<String> routineKeyIterator = scanRoutines.keySet().iterator();
    while ( routineKeyIterator.hasNext() ) {
      ScanRoutine sr = scanRoutines.get(routineKeyIterator.next());
      result+= "\n"+sr.toString();
    }

    result += "\n Event Compilers : " +  eventCompilers.size() + "\n";

    for ( EventCompiler ec : eventCompilers ) {
      result+= "\n"+ec.toString();
    }

    result += "\n";

    return result;
  }

}
