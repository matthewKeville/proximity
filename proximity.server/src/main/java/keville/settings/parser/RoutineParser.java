package keville.settings.parser;

import keville.event.EventTypeEnum;
import keville.settings.ScanRoutine;
import keville.util.GeoUtils;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.time.Instant;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//Parse the settings.json routines field into ScanRoutines
public class RoutineParser {

  static Logger LOG = LoggerFactory.getLogger(RoutineParser.class);

  public static Map<String,ScanRoutine> parseScanRoutines(JsonArray scans,boolean eventbriteKeyFound) throws SettingsParserException {

    Map<String,ScanRoutine> scanRoutineMap = new HashMap<String,ScanRoutine>();

    for ( JsonElement scan : scans ) {

      try {
        ScanRoutine routine  = parseScanRoutine(scan.getAsJsonObject(),eventbriteKeyFound);
        if ( scanRoutineMap.containsKey(routine.name) ) {
            throw new SettingsParserException("Invalid routine : scan routine names must be unique! But found " + routine.name + " more than once");
        }
        scanRoutineMap.put(routine.name,routine);
      } catch ( IllegalStateException ise ) { //if scan is not an JsonObject
        throw new SettingsParserException("Invalid routine : scan routine is an object");
      }
    }

    return scanRoutineMap;

  }

  private static ScanRoutine parseScanRoutine(JsonObject scanJson,boolean eventbriteKeyFound) throws SettingsParserException {

    ScanRoutine scanRoutine = new ScanRoutine();
    scanRoutine.types = new HashSet<EventTypeEnum>();

    ///////////////////////////
    //name

    if ( !scanJson.has("name") ) {
      throw new SettingsParserException("Invalid routine : name is a required field");
    }
    scanRoutine.name = ParseUtil.tryParseNonEmptyString(scanJson,"name","Invalid routine : name expects a string");

    ///////////////////////////
    //radius

    if ( !scanJson.has("radius") ) {
      throw new SettingsParserException("Invalid routine : radius is a required field");
    }
    scanRoutine.radius = ParseUtil.tryParseDouble(scanJson,"radius","Invalid routine : radius expects a double");

    ///////////////////////////
    //latitude and longitude

    if ( scanJson.has("auto") && scanJson.get("auto").getAsBoolean() ) {

      Map<String,Double> coords = GeoUtils.getClientGeolocation();
      scanRoutine.latitude = coords.get("latitude");
      scanRoutine.longitude = coords.get("longitude");

    } else {

      if ( !scanJson.has("latitude")  || !scanJson.has("longitude") ) {
        throw new SettingsParserException("Invalid routine : auto or longitude and latitude are required");
      }
  
      scanRoutine.latitude = ParseUtil.tryParseDouble(scanJson,"latitude","Invalid routine : latitude expects a double");
      scanRoutine.longitude = ParseUtil.tryParseDouble(scanJson,"longitude","Invalid routine : longitude expects a double");

    }

    ///////////////////////////
    //delay

    if ( !scanJson.has("delay") ) {
      throw new SettingsParserException("Invalid routine : delay is a required field");
    }
    scanRoutine.delay = ParseUtil.tryParseInt(scanJson,"delay","Invalid routine : delay expects an int");

    ///////////////////////////
    //event sources

    if (  scanJson.has("meetup") && 
        ParseUtil.tryParseBoolean(scanJson,"meetup","Invalid routine : meetup expects a boolean")) {
      scanRoutine.types.add(EventTypeEnum.MEETUP);
    }

    if (  scanJson.has("eventbrite") && 
        ParseUtil.tryParseBoolean(scanJson,"eventbrite","Invalid routine : eventbrite expects a boolean")) {
      if ( !eventbriteKeyFound ) {
        throw new SettingsParserException("Invalid routine : eventbrite_api_key is required when eventbrite = true");
      }
      scanRoutine.types.add(EventTypeEnum.EVENTBRITE);
    }

    ///////////////////////////
    //disabled

    if ( scanJson.has("disabled") && 
        ParseUtil.tryParseBoolean(scanJson,"disabled","Invalid routine : disabled expects a boolean")) {
      scanRoutine.disabled = true;
    }

    ///////////////////////////
    //run on restart
    if ( scanJson.has("run_on_restart") &&
        ParseUtil.tryParseBoolean(scanJson,"run_on_restart","Invalid routine : run_on_restart expects a boolean")) {
      scanRoutine.runOnRestart =  true;
    }

    //does this belong here?
    scanRoutine.lastRan = scanRoutine.runOnRestart ? Instant.EPOCH : Instant.now();

    return scanRoutine;

  }

}
