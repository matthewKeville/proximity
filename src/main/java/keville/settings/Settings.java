package keville.settings;

import keville.ScanRoutine;

import java.util.List;
import java.util.LinkedList;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class Settings {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Settings.class);


  static final int EVENTBRITE_MAX_PAGES_DEFAULT = 8;
  static final int ALLEVENTS_MAX_PAGES_DEFAULT = 8;

  public String dbConnectionString;
  public String eventbriteDbConnectionString;
  public String eventbriteApiKey;
  public int eventbriteMaxPages;
  public int alleventsMaxPages;
  public List<ScanRoutine> scanRoutines;
  //public List<CompilerSettings> compilerSettings;

  public static Settings parseSettings(String jsonString) throws Exception {/* populate jobs with data stored on LFS */

    Settings settings = new Settings();
    JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

    settings.dbConnectionString  = "jdbc:sqlite:app.db";
    if  ( json.has("db_connection_string")) {
      settings.dbConnectionString  = json.get("db_connection_string").getAsString();
    }

    settings.eventbriteDbConnectionString  = "jdbc:sqlite:eventbrite.db";
    if  ( json.has("eventbrite_db_connection_string")) {
      settings.eventbriteDbConnectionString  = json.get("eventbrite_db_connection_string").getAsString();
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

    return settings;

  }

  public String toString() {

    String result = "\ndbConnectionString : " + dbConnectionString;
    result += "\neventbriteApiKey : " + eventbriteApiKey;
    result += "\neventbriteMaxPages : " + eventbriteMaxPages;
    result += "\nalleventsMaxPages : " + alleventsMaxPages;
    for ( ScanRoutine sr : scanRoutines ) {
      result+= "\n"+sr.toString();
    }

    return result;
  }

}
