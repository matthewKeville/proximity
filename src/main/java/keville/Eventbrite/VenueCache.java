package keville.Eventbrite;

import java.util.Properties;

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

public class VenueCache {

  private static String venueBaseUri = "https://www.eventbriteapi.com/v3/venues/";
  private HttpClient httpClient;
  private String BEARER_TOKEN;
  private Connection con;
  private String connectionString;
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VenueCache.class);

  public VenueCache(Properties properties) {
    BEARER_TOKEN = properties.getProperty("event_brite_api_key");
    httpClient = HttpClient.newHttpClient();
    connectionString = "jdbc:sqlite:eventbrite.db"; //pls put in properties (custom & default)
    LOG.info("connecting to " + connectionString);
    try {
      con = DriverManager.getConnection(connectionString);
      LOG.info("connected to " + connectionString);
    } catch (SQLException e) {
      LOG.error("Critical error : unable to read venues from database : " + connectionString);
      LOG.error(e.getMessage());
      System.exit(5);
    }
  }

  public JsonObject get(String venueId) {
    JsonObject venueJson = getVenueJsonFromDb(venueId);
    if ( (venueJson == null) ) {
      LOG.info(String.format("local miss on venue %s",venueId));
      venueJson = getVenueFromApi(venueId);
      if (venueJson != null ) {
        createVenueJsonInDb(venueId, venueJson);
      } else {
        LOG.error("venuebrite api generated a null venuejson");
      }
    } else {
      LOG.info(String.format("local hit on venue %s",venueId));
    }
    return venueJson;
  }

  private boolean createVenueJsonInDb(String venueId, JsonObject venueJson) {

    try {
      String queryTemplate = "INSERT INTO VENUE (VENUE_ID,JSON) VALUES (?,?);";    
      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1,venueId);
      ps.setString(2,venueJson.toString());
      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;
    } catch (SQLException se)  {
      LOG.error("error adding eventbrite venue data to eventbrite.db");
      LOG.error(se.getMessage());
    }
    return false;
  }

  private JsonObject getVenueJsonFromDb(String venueId) {

    String json ="";
    JsonObject jsonVenue = null;

    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM VENUE WHERE VENUE_ID=?;");
      ps.setString(1,venueId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        json = rs.getString("json");
        jsonVenue = JsonParser.parseString(json).getAsJsonObject();
      } 
    } catch (SQLException se) {
      LOG.error("error retrieving eventbrite venue data from eventbrite.db");
      LOG.error(se.getMessage());
    }
    return jsonVenue;
  }

  private JsonObject getVenueFromApi(String venueId) {

    HttpRequest getRequest;
    JsonObject venueJson = null;

    try {
    URI uri = new URI(venueBaseUri+venueId+"/"); /*without terminal '/' we get a 301 */
    getRequest = HttpRequest.newBuilder()
      .uri(uri) 
      .header("Authorization","Bearer "+BEARER_TOKEN)
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
      venueJson = JsonParser.parseString(getResponse.body()).getAsJsonObject();
    } catch (Exception e) {
      /*Interrupted / IO*/
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }

    return venueJson;

  }

}
