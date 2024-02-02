package keville.providers.Eventbrite;

import keville.settings.Settings;

import java.io.File;
import java.io.FileNotFoundException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

// FIXME : This cache should short lifetime (Currently forever). It's primarily for test.
@Component
public class EventCache {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventCache.class);

  private EventbriteAPI eventbriteAPI;
  private Settings settings;

  public EventCache(@Autowired Settings settings,
      @Autowired EventbriteAPI eventbriteAPI) throws FileNotFoundException {
    this.settings = settings;
    this.eventbriteAPI = eventbriteAPI;
    initTable();
  }

  public JsonObject getEventById(String eventId) throws UnlikelyEventIdException, EventbriteAPIException {

    if (!NumberUtils.isCreatable(eventId)) {
      throw new UnlikelyEventIdException("id string : " + eventId + " is an unlikely event id ");
    }

    JsonObject eventJson = getEventJsonFromDb(eventId);
    if ( eventJson != null ) {
      return eventJson;
    }

    eventJson = eventbriteAPI.getEvent(eventId);
    createEventJsonInDb(eventId, eventJson);
    return eventJson;

  }

  // PLS FIX
  private String getDbConnectionString() {
    return "jdbc:sqlite:" + settings.dbFile();
  }

  private Connection getDbConnection() {
    Connection con = null;
    LOG.debug("connecting to " + getDbConnectionString());
    try {
      con = DriverManager.getConnection(getDbConnectionString());
    } catch (SQLException e) {
      LOG.error("Critical error : unable to read events from database : " + getDbConnectionString());
      LOG.error(e.getMessage());
      System.exit(5);
    }
    return con;
  }

  private void closeDbConnection(Connection con) {
    try {
      con.close();
    } catch (Exception e) {
      LOG.error("unable to close db connection");
      LOG.error(e.getMessage());
    }
  }

  private boolean createEventJsonInDb(String eventId, JsonObject eventJson) {

    Connection con = getDbConnection();
    try {
      String queryTemplate = "INSERT INTO EVENTBRITE_EVENT (EVENT_ID, JSON) "
          + " VALUES (?,?);";
      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1, eventId);
      ps.setString(2, eventJson.toString());
      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;
    } catch (SQLException se) {
      LOG.error("error adding eventbrite event data db");
      LOG.error(se.getMessage());
    } finally {
      closeDbConnection(con);
    }
    return false;
  }

  private JsonObject getEventJsonFromDb(String eventId) {

    String json = "";
    JsonObject jsonEvent = null;
    Connection con = getDbConnection();

    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM EVENTBRITE_EVENT WHERE EVENT_ID=?;");
      ps.setString(1, eventId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        json = rs.getString("json");
        jsonEvent = JsonParser.parseString(json).getAsJsonObject();
      }
    } catch (SQLException se) {
      LOG.error("error retrieving eventbrite event data from db");
      LOG.error(se.getMessage());
    } finally {
      closeDbConnection(con);
    }

    return jsonEvent;
  }

  /* make sure there is an eventbrite event table */
  private void initTable() {

    File dbFile = new File(settings.dbFile());
    if (!dbFile.exists()) {
      LOG.info("no db file found , creating db file @ " + settings.dbFile());
    }

    Connection con = getDbConnection();
    try {
      Statement stmt = con.createStatement();
      String sql = """
            CREATE TABLE IF NOT EXISTS EVENTBRITE_EVENT(
              ID INTEGER PRIMARY KEY AUTOINCREMENT,
              EVENT_ID TEXT NOT NULL,
              JSON STRING NOT NULL
            );
          """;
      stmt.execute(sql);
    } catch (SQLException e) {
      LOG.error("error initalzing eventbrite event table");
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }

  }

}
