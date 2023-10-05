package keville.settings;

import keville.scanner.ScanRoutine;

import keville.event.Event;
import keville.event.Events;
import keville.compilers.EventCompiler;
import keville.compilers.RSSCompiler;
import keville.compilers.ICalCompiler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.LinkedList;
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
  public List<ScanRoutine> scanRoutines;
  public List<EventCompiler> eventCompilers;

  public static Settings parseSettings(String jsonString) throws Exception {/* populate jobs with data stored on LFS */

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


    if ( !json.has("scans") ) {
      LOG.warn("no scans where found in the configuration file, generating default scan");
      settings.scanRoutines = new LinkedList<ScanRoutine>();
      settings.scanRoutines.add(ScanRoutine.createDefault());
    } else {
      settings.scanRoutines = ScanRoutine.parseScanRoutines(json.get("scans").getAsJsonArray());
    }


    if ( !json.has("compilers") ) {
      LOG.warn("no compilers where found in the configuration file");
      settings.eventCompilers = new LinkedList<EventCompiler>();
    } else {
      settings.eventCompilers = parseEventCompilers(json.get("compilers").getAsJsonArray());
    }

    return settings;

  }





  /*
  eventCompiler  configuration syntax

  {
    name : ...
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

      Predicate<Event> filter = new Predicate<Event>() {  //so cumbersome, why no Predicate.True?
        public boolean test(Event x) { return true; }
      };

      if ( !compilerJson.has("filters") )  {
        LOG.warn(name + " has no filters");
        LOG.warn("this will include everything ...");
      } else {

        for ( JsonElement filterJsonElm : compilerJson.get("filters").getAsJsonArray() ) {
          JsonObject filterJson = filterJsonElm.getAsJsonObject();
          Predicate<Event> subFilter = parseCompilerFilter(filterJson);
          if ( subFilter != null ) {
            filter = (conjunction) ? filter.and(subFilter) : filter.or(subFilter);
          } else {
            LOG.warn("part of the filter chain for the compiler " +  name  + " is misconfigured");
          }
        }

      }

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

/*
    planned filter types

    {
      type  : "and"
      filterA :
      filterB :
    }
    {
      type  : "or"
      filterA : {}
      filterB : {}
    }
    {
      type  : "disk",
      radius : "",
      latitude: "",
      longitude : "",
    }
    
    Having types for connective logic would allow for a more expressive configuration.
    For a draft of compiler parsing these types will be left out.
*/


  private static Predicate<Event> parseCompilerFilter(JsonObject filterJson) {
    
    if ( !filterJson.has("type") ) {
      LOG.error("misconfigured filter, you must provide a \"type\"");
      return null;
    }

    String filterType = filterJson.get("type").getAsString();

    switch (filterType) {

      case "disk":

        if (!filterJson.has("radius")) {
          LOG.error("need value \"radius\", for filter type disk");
          return null;
        }
        Double radius = filterJson.get("radius").getAsDouble(); 

        if (!filterJson.has("latitude")) {
          LOG.error("need value \"latitude\", for filter type disk");
          return null;
        }
        Double latitude = filterJson.get("latitude").getAsDouble(); 

        if (!filterJson.has("longitude")) {
          LOG.error("need value \"longitude\", for filter type disk");
          return null;
        }
        Double longitude = filterJson.get("longitude").getAsDouble(); 

        if ( radius == null || latitude == null || longitude == null ) {
          LOG.error("you have null values in a disk filter");
          return null;
        }

        return Events.WithinKMilesOf(latitude,longitude,radius);

      case "virtual":

        if (!filterJson.has("allowed") || !filterJson.get("allowed").getAsBoolean() ) {
          return Events.NotVirtual();
        } 
        return null;

      default:
        LOG.warn("filter type : " + filterType + " is unknown");
        return null;

    }

  }



  public String toString() {

    String result = "\ndbConnectionString : " + dbConnectionString;

    result += "\neventbriteApiKey : " + eventbriteApiKey;
    result += "\neventbriteMaxPages : " + eventbriteMaxPages;
    result += "\nalleventsMaxPages : " + alleventsMaxPages;

    result += "\n Scan Routines : " +  scanRoutines.size() + "\n";

    for ( ScanRoutine sr : scanRoutines ) {
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
