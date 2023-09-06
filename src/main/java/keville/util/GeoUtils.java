package keville.util;

import keville.Location;
import java.util.Map;
import java.util.HashMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class GeoUtils {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GeoUtils.class);

  /* return a bounding box  that is circumsribed by the circle defined by (lon,lat,radius) */
  public static Map<String,Double> radialBbox(double lat,double lon,double radius /* miles */ ) {
    double km = radius * 1.60934;
    double deg = km * 90.0 /*deg*/ / 10_000.0 /*km*/;
    Map<String,Double> map = new HashMap<String,Double>();
    map.put("ulat",(lat-deg));
    map.put("ulon",(lon-deg));
    map.put("blat",(lat+deg));
    map.put("blon",(lon+deg));
    return map;
  }

  public static boolean isWithinMilesOf(double miles,double latCenter,double lonCenter,double lat,double lon) {
    double distGeo = Math.sqrt( Math.pow(latCenter - lat,2.0) + Math.pow(lonCenter - lon,2.0) );
    double radiusKm = miles * 1.60934;
    double radiusDeg = radiusKm * 90.0 /*deg*/ / 10_000.0 /*km*/;
    return distGeo < radiusDeg;
  }

  //https://github.com/dcarrillo/whatismyip
  public static Map<String,Double> getClientGeolocation() {

    Map<String,Double> geoLocation = new HashMap<String,Double>();
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest getRequest;
    String response = "";

    try {
    URI uri = new URI("https://ifconfig.es/geo"); /*without terminal '/' we get a 301 */
    getRequest = HttpRequest.newBuilder()
      .uri(uri) 
      .GET()
      .build();
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      LOG.warn("ajsdkfjasd");
      LOG.debug("kajsdfa");
      LOG.info(String.format("request returned %d",getResponse.statusCode()));
      response = getResponse.body();
    } catch (Exception e) {
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }

    /*
    City: Belmar
    Country: United States
    Country Code: US
    Latitude: 40.171200
    Longitude: -74.071700
    Postal Code: 07719
    Time Zone: America/New_York
    */
    String[] fields = response.split("\n");
    geoLocation.put("latitude",Double.parseDouble(fields[3].split(" ")[1]));
    geoLocation.put("longitude",Double.parseDouble(fields[4].split(" ")[1]));
    return geoLocation;

  }

  public static Location getLocationFromGeoCoordinates(double latitude, double longitude) {
    Location result = null;
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest getRequest;
    String response = "";
    
    LOG.info(" attempting reverse geocode on lat = " + latitude + " and lon = " + longitude);

    try {
    URI uri = new URI("https://geocode.maps.co/reverse?lat=" + latitude + "&lon=" + longitude ); /*without terminal '/' we get a 301 */
    getRequest = HttpRequest.newBuilder()
      .uri(uri) 
      .GET()
      .build();
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      LOG.info(String.format("request returned %d",getResponse.statusCode()));
      response = getResponse.body();
    } catch (Exception e) {
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }

    String displayString = "";

    try {
      JsonObject json = JsonParser.parseString(response).getAsJsonObject();
      JsonObject address = json.getAsJsonObject("address");
      displayString = json.get("display_name").getAsString();
      String state = address.get("state").getAsString();
      String city = address.get("town").getAsString();
      result = new Location(latitude, longitude, state, city);
    } catch (Exception e) {
      LOG.error(" unable to reverse geocode lat= " + latitude + " lon= " + latitude + " into a Location ");
      LOG.error(" location display name : " + displayString );
      LOG.error(e.getMessage());
    }

    return result;
    
  }

}
