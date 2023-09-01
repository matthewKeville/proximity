package keville.Eventbrite;

import java.util.Properties;
import java.util.Map;

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
import java.sql.Statement;
import java.sql.ResultSet;

public class EventCache {

  private static String eventBaseUri = "https://www.eventbriteapi.com/v3/events/";

  private HttpClient httpClient;
  private String BEARER_TOKEN;

  private Connection con;
  private String connectionString;

  public EventCache (Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();

    //connectionString = properties.getProperty("connection_string");
    connectionString = "jdbc:sqlite:eventbrite.db"; //pls put in properties (custom & default)
    System.out.println("connecting to " + connectionString);

    try {
      con = DriverManager.getConnection(connectionString);
      System.out.println("connected to " + connectionString);
    } catch (SQLException e) {
      System.out.println("Critical error : unable to read events from database : " + connectionString);
      System.out.println(e.getMessage());
      System.exit(5);
    }

  }

  public JsonObject get(String eventId) {
    JsonObject eventJson = getEventJsonFromDb(eventId);
    if ( (eventJson == null) ) {
      System.out.println(String.format("local miss on event %s",eventId));
      eventJson = getEventFromApi(eventId);
      if (eventJson != null ) {
        createEventJsonInDb(eventId, eventJson);
      } else {
        System.err.println("eventbrite api generated a null eventjson");
      }
    }    
    return eventJson;
  }

  private boolean createEventJsonInDb(String eventId, JsonObject eventJson) {

    try {
      String sql = "INSERT INTO EVENT (EVENT_ID,JSON)" 
        + "VALUES (" + "'" + eventId + "', "    
        + "'" + eventJson.toString() + "'"    
        +");";                                        

      System.out.println(sql);
      Statement stmt = con.createStatement();
      int rowsUpdated = stmt.executeUpdate(sql);
      return rowsUpdated == 1;
    } catch (SQLException se)  {
      System.out.println("error adding eventbrite event data to eventbrite.db");
      System.out.println(se.getMessage());
    }
    return false;
  }


  private JsonObject getEventJsonFromDb(String eventId) {

    String json ="";
    JsonObject jsonEvent = null;

    try {
      String sql = "SELECT * FROM EVENT WHERE EVENT_ID="+eventId+";"; //what is the query?
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        json = rs.getString("json");
        jsonEvent = JsonParser.parseString(json).getAsJsonObject();
      } 
    } catch (SQLException se) {
      System.out.println("error retrieving eventbrite event data from eventbrite.db");
      System.out.println(se.getMessage());
    }

    return jsonEvent;

  }

  /* Get event data from Event Brite API */
  private JsonObject getEventFromApi(String eventId) {

    HttpRequest getRequest;
    JsonObject eventJson = null;

    try {
    URI uri = new URI(eventBaseUri+eventId+"/"); /*without terminal '/' we get a 301 */
    getRequest = HttpRequest.newBuilder()
      .uri(uri) 
      .header("Authorization","Bearer "+BEARER_TOKEN)
      .GET()
      .build();
    } catch (URISyntaxException e) {
      System.out.println(String.format("error building request: %s",e.getMessage()));
      return null;
    }

    try {
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      System.out.println(String.format("request returned %d",getResponse.statusCode()));
      eventJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      System.out.println(String.format("error sending request %s",e.getMessage()));
    }

    return eventJson;

  }

}
