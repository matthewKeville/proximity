package keville.util;

import keville.location.Location;
import keville.location.LocationBuilder;
import keville.location.USStateAndTerritoryCodes;

import java.util.Map;
import java.util.HashMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * This class has low cohesion. Location utilities isWithinMiles, radialBbox, distanceInMiles
 * are of a different nature than getClientGeolocation. FIXME : break this into two classes 
 * */
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

    double radiusKm = miles * 1.60934;
    double distGeo = Math.sqrt( Math.pow(latCenter - lat,2.0) + Math.pow(lonCenter - lon,2.0) );
    double radiusGeo = radiusKm * 90.0 /*deg*/ / 10_000.0 /*km*/;

    return distGeo < radiusGeo;
  }

  public static double distanceInMiles(double lat0,double lon0,double latf,double lonf) {
    double distGeo = Math.sqrt( Math.pow(latf - lat0,2.0) + Math.pow(lonf - lon0,2.0) );
    double distKm = distGeo / 90.0 * 10_000.0;
    double distMiles = distKm / 1.60934;
    return distMiles;
  }

  public static Map<String,Double> getClientGeolocation() {

    Map<String,Double> geoLocation = new HashMap<String,Double>();
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest getRequest;
    String response = "";

    try {
      //without terminal '/' we get a 301
      URI uri = new URI("https://ifconfig.es/geo"); 
      getRequest = HttpRequest.newBuilder()
        .uri(uri) 
        .GET()
        .build();
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      response = getResponse.body();
    } catch (Exception e) {
      LOG.error(String.format("Error requesting reverse Geocode information",e.getMessage()));
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
    
    LOG.info("Reverse Geocoding : " + latitude + " , " + longitude);

    try {

      URI uri = new URI("https://geocode.maps.co/reverse?lat=" + latitude + "&lon=" + longitude ); /*without terminal '/' we get a 301 */
      getRequest = HttpRequest.newBuilder()
        .uri(uri) 
        .GET()
        .build();
      HttpResponse<String> getResponse = httpClient.send(getRequest, BodyHandlers.ofString());
      response = getResponse.body();

    } catch (Exception e) {
      LOG.error(String.format("error sending request %s",e.getMessage()));
    }


    try {

      JsonObject json = JsonParser.parseString(response).getAsJsonObject();

      if ( !json.has("address"))  {
        LOG.warn("No address returned");
        return result;
      }

      JsonObject address = json.getAsJsonObject("address");
      
      String state = null;
      if ( address.has("state") ) {
        state = address.get("state").getAsString();
        state = USStateAndTerritoryCodes.getANSILcode(state); //we want "nj" not "New Jersey"
      }

      String locality = null; 
      if ( address.has("village") )  {
        locality = address.get("village").getAsString();
      } else if ( address.has("town") ) {
        locality = address.get("town").getAsString();
      } else if ( address.has("city") ) {
        locality = address.get("city").getAsString();
      } else {
        LOG.warn("Failed to find a town , city or village for the geocoordinate : " + latitude + " , " + longitude);
      }

      LocationBuilder lb = new LocationBuilder();
      lb.setLatitude(latitude);
      lb.setLongitude(longitude);

      LOG.warn("Spoofing country field, api returns country in name format instead of ANSIL standard");
      lb.setCountry("us");

      lb.setRegion(state);
      lb.setLocality(locality);
      result =  lb.build();

    } catch (Exception e) {

      LOG.error("Failed to reverse Geocode " + latitude + " , " + longitude);
      LOG.error(e.getMessage());

    }

    return result;
    
  }

}
