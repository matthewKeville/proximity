package keville.Eventbrite;

import keville.Settings;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EventCache {

  private static String eventBaseUri = "https://www.eventbriteapi.com/v3/events/";

  private static Settings settings;
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventCache.class);

  public static void applySettings(Settings s) {
    settings = s;
  }

  public static JsonObject get(String eventId) {
    JsonObject eventJson = getEventJsonFromDb(eventId);
    if ( (eventJson == null) ) {
      LOG.info(String.format("local miss on event %s",eventId));
      eventJson = getEventFromApi(eventId);
      if (eventJson != null ) {
        createEventJsonInDb(eventId, eventJson);
      } else {
        LOG.error("eventbrite api generated a null eventjson");
      }
    } else {
      LOG.info(String.format("local hit on event %s",eventId));
    }
    return eventJson;
  }

  private static Connection getDbConnection() {
    Connection con  = null;
    String connectionString = "jdbc:sqlite:eventbrite.db"; //pls put in properties (custom & default)
    LOG.debug("connecting to " + connectionString);
    try {
      con = DriverManager.getConnection(connectionString);
      LOG.debug("connected to " + connectionString);
    } catch (SQLException e) {
      LOG.error("Critical error : unable to read events from database : " + connectionString);
      LOG.error(e.getMessage());
      System.exit(5);
    }
    return con;
  }

  private static void closeDbConnection(Connection con) {
    try {
      con.close();
    } catch (Exception e) {
      LOG.error("unable to close db connection");
      LOG.error(e.getMessage());
    }
  }

  private static boolean createEventJsonInDb(String eventId, JsonObject eventJson) {

    Connection con = getDbConnection();
    try {
      String queryTemplate = "INSERT INTO EVENT (EVENT_ID, JSON) "
        + " VALUES (?,?);";
      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1,eventId);
      ps.setString(2,eventJson.toString());
      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;
    } catch (SQLException se)  {
      LOG.error("error adding eventbrite event data to eventbrite.db");
      LOG.error(se.getMessage());
    } finally {
      closeDbConnection(con);
    }
    return false;
  }


  private static JsonObject getEventJsonFromDb(String eventId) {

    String json ="";
    JsonObject jsonEvent = null;
    Connection con = getDbConnection();

    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM EVENT WHERE EVENT_ID=?;");
      ps.setString(1,eventId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        json = rs.getString("json");
        jsonEvent = JsonParser.parseString(json).getAsJsonObject();
      } 
    } catch (SQLException se) {
      LOG.error("error retrieving eventbrite event data from eventbrite.db");
      LOG.error(se.getMessage());
    } finally {
      closeDbConnection(con);
    }

    return jsonEvent;
  }

  private static JsonObject getEventFromApi(String eventId) {

    HttpClient  httpClient = HttpClient.newHttpClient();
    HttpRequest getRequest;
    JsonObject eventJson = null;

    try {

      // EB API has expansions that pull in data from other endpoints (organizer & venue)
      URI uri = new URI(eventBaseUri+eventId+"/"+"?expand=organizer,venue");
      getRequest = HttpRequest.newBuilder()
        .uri(uri) 
        .header("Authorization","Bearer "+settings.eventBriteApiKey)
        .GET()
        .build();

    } catch (URISyntaxException e) {
      LOG.error("error constructing api endpoint url");
      LOG.error(String.format("error building request: %s",e.getMessage()));
      return null;
    }

    try {
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      LOG.info(String.format("request returned %d",getResponse.statusCode()));
      eventJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }

    return eventJson;

  }

}
