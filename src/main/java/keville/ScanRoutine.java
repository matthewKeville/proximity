package keville;

import keville.util.GeoUtils;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ScanRoutine {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ScanRoutine.class);

  public double radius;
  public double latitude;
  public double longitude;
  public int delay;
  public Instant lastRan;
  public boolean runOnRestart;
  public String name;

  public boolean meetup;
  public boolean allevents;
  public boolean eventbrite;

  public boolean disabled;

  public static List<ScanRoutine> parseScanRoutines(JsonArray scans) throws Exception {

    List<ScanRoutine> scanRoutineList = new LinkedList<ScanRoutine>();

    for ( JsonElement scan : scans ) {
      ScanRoutine routine  = parseScanRoutine(scan.getAsJsonObject());
      scanRoutineList.add(routine);
    }

    return scanRoutineList;

  }

  public static ScanRoutine parseScanRoutine(JsonObject scanJson) throws Exception {

    ScanRoutine scanRoutine = new ScanRoutine();

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

    scanRoutine.meetup     =  scanJson.has("meetup")     && scanJson.get("meetup").getAsBoolean();
    scanRoutine.allevents  =  scanJson.has("allevents")  && scanJson.get("allevents").getAsBoolean();
    scanRoutine.eventbrite =  scanJson.has("eventbrite") && scanJson.get("eventbrite").getAsBoolean();

    scanRoutine.runOnRestart =  scanJson.has("run_on_restart") && scanJson.get("run_on_restart").getAsBoolean();
    scanRoutine.lastRan = scanRoutine.runOnRestart ? Instant.EPOCH : Instant.now();

    scanRoutine.name =  scanJson.has("name") ? scanJson.get("name").getAsString()  : "";

    scanRoutine.disabled =  scanJson.has("disabled") && scanJson.get("disabled").getAsBoolean();

    return scanRoutine;

  }

  public static ScanRoutine createDefault() {

    ScanRoutine routine = new ScanRoutine();
    routine.radius = 5.0;
    routine.delay = 7200;
    routine.meetup = true;
    routine.allevents = true;

    return routine;
  }

  public String toString() {

    String result = "";
    result += "\n\tname : " + name;
    result += "\n\tenabled : " + !disabled;
    result += "\n\tradius : " + radius;
    result += "\n\tlatitude : " + latitude;
    result += "\n\tlongitude : " + longitude;
    result += "\n\tmeetup : " + meetup;
    result += "\n\tallevents : " + allevents;
    result += "\n\teventbrite : " + eventbrite;
    result += "\n\tdelay : " + delay;
    result += "\n\tlastRan : " + lastRan;
    result += "\n\trunOnRestart : " + runOnRestart;
    return result;

  }

}
