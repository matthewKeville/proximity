package keville.settings.parser;

import keville.event.Event;
import keville.settings.ScanRoutine;
import keville.settings.Settings;
import keville.compilers.EventCompiler;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SettingsParser {

  static Logger LOG = LoggerFactory.getLogger(Settings.class);

  static final int EVENTBRITE_MAX_PAGES_DEFAULT = 8;

  public static Settings parseSettings(String jsonString) throws Exception {

    JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

    /////////////////////
    // general

    String dbFile  = "app.db";
    if  ( json.has("db_path")) {
      dbFile = ParseUtil.tryParseNonEmptyString(json,"db_path","Invalid settings : dp_path expects a string");
    }

    String eventbriteApiKey = "";
    if ( json.has("eventbrite_api_key")) {
      eventbriteApiKey = ParseUtil.tryParseNonEmptyString(json,"eventbrite_api_key","Invalid settings : eventbrite_api_key expects a string");
    }

    int eventbriteMaxPages = EVENTBRITE_MAX_PAGES_DEFAULT;
    if ( json.has("eventbrite_max_pages")) {
      eventbriteMaxPages = ParseUtil.tryParseInt(json,"eventbrite_max_pages","Invalid settings : eventbrite_max_pages expects an int");
    }

    /////////////////////
    // routines

    Map<String,ScanRoutine> scanRoutines;
    if ( !json.has("routines") ) {
      throw new SettingsParserException("routines is a required field");
    } else {
      JsonArray routinesArray = ParseUtil.tryParseArray(json,"routines","Invalid settings : routines expects an array");
      scanRoutines = RoutineParser.parseScanRoutines(routinesArray,!eventbriteApiKey.isEmpty());
    }

    /////////////////////
    // filters

    Map<String,Predicate<Event>> filters = Collections.emptyMap();
    if ( json.has("filters") ) {
      JsonArray filtersArray = ParseUtil.tryParseArray(json,"filters","Invalid settings : filters expects an array");
      filters = FilterParser.parseEventFilters(filtersArray);
    }
 
    //note the order of ops, compilers might use these filters
    FilterParser.registerCustomFilters(filters);

    /////////////////////
    // compilers

    List<EventCompiler> eventCompilers = Collections.emptyList();
    if ( json.has("compilers") ) {
      JsonArray compilersArray = ParseUtil.tryParseArray(json,"compilers","Invalid settings : comilers expects an array");
      eventCompilers = CompilerParser.parseEventCompilers(compilersArray);
    }

    return new Settings(dbFile,eventbriteApiKey,eventbriteMaxPages,scanRoutines,filters,eventCompilers);

  }

}
