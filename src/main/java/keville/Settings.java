package keville;

import keville.util.GeoUtils;
import java.util.Map;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class Settings {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Settings.class);

  public String eventBriteApiKey;
  public String applicationConnectionString;
  public double radius;
  public double latitude;
  public double longitude;
  public int delay;
  public boolean auto;
  public boolean runOnRestart;

  public boolean meetup;
  public boolean allevents;
  public boolean eventbrite;

  public int maxEventbritePages;
  public int maxAlleventsPages;

  public static Settings parseSettings(String jsonString) throws Exception {/* populate jobs with data stored on LFS */

    Settings settings = new Settings();
    JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

    settings.applicationConnectionString  = json.get("connection_string").getAsString();
    settings.radius =  json.get("radius").getAsDouble();

    if ( json.get("auto").getAsBoolean() )  {
      Map<String,Double> coords = GeoUtils.getClientGeolocation();
      settings.latitude = coords.get("latitude");
      settings.longitude = coords.get("longitude");
      LOG.info("inferred geocoordinate locataion : " + settings.latitude + " , " + settings.longitude);
    } else {
      settings.latitude = json.get("latitude").getAsDouble();
      settings.longitude = json.get("longitude").getAsDouble();
      LOG.info("using geocoordinate locataion : " + settings.latitude + " , " + settings.longitude);
    }

    settings.delay = json.get("delay").getAsInt();
    settings.meetup  =  json.get("MEETUP").getAsBoolean();
    settings.allevents = json.get("ALLEVENTS").getAsBoolean();
    settings.eventbrite =  json.get("EVENTBRITE").getAsBoolean();

    settings.runOnRestart=  json.get("run_on_restart").getAsBoolean();

    if (settings.eventbrite) {
      if ( json.has("event_brite_api_key") ) {
        settings.eventBriteApiKey  = json.get("event_brite_api_key").getAsString();
      }
      if ( settings.eventBriteApiKey.equals("") || settings.eventBriteApiKey == null ) {
        throw new Exception("You must supply an event_brite_api_key to scan eventbrite events");
      } else {
        LOG.info("using api_key : " + settings.eventBriteApiKey);
      }

      if ( json.has("event_brite_max_pages_scrub")) {
        settings.maxEventbritePages = json.get("event_brite_max_pages_scrub").getAsInt();
        LOG.info("event_brite_max_pages_scrub = " + settings.maxEventbritePages);
      } else {
        settings.maxEventbritePages = 5;
      }
    }

    if (settings.allevents) {

      if ( json.has("allevents_max_pages_scrub")) {
        settings.maxAlleventsPages = json.get("allevents_max_pages_scrub").getAsInt();
        LOG.info("allevents_max_pages_scrub = " + settings.maxAlleventsPages);
      } else {
        settings.maxAlleventsPages = 5;
      }

    }

    return settings;

  }

}
