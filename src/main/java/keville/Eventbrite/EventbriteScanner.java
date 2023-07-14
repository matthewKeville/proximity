package keville.Eventbrite;

import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;

import keville.Eventbrite.VenueCache;
import keville.Eventbrite.EventCache;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.io.StringWriter;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

import java.util.stream.Collectors;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.chrome.ChromeOptions;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EventbriteScanner implements EventScanner {


  /* return a bounding box  that is circumsribed by the circle defined by (lon,lat,radius) */
  static Map<String,Double> radialBbox(double lat,double lon,double radius /* miles */ ) {
    double km = radius * 1.60934;
    double deg = km * 90.0 /*deg*/ / 10_000.0 /*km*/;

    Map<String,Double> map = new HashMap<String,Double>();
    map.put("ulat",(lat-deg));
    map.put("ulon",(lon-deg));
    map.put("blat",(lat+deg));
    map.put("blon",(lon+deg));
    return map;
  }

  static String eventMapUrl(double lat,double lon,double radius,int page) {
    if (page <= 0 ) {
      System.err.println("non positive page indices are undefined");
      return "";
    }
    Map<String,Double> map = radialBbox(lat,lon,radius);
    String site = "https://www.eventbrite.com/";
    String locationPrefix = "d/united-states/belmar-new/"; /* I don't think /united-states/ matters i.e. what's inside / / */
    String mapPrefix = String.format("?page=%d&bbox=",page);  //"?page=1&bbox=";
    return String.format("%s%16.14f%c2C%16.14f%c2C%16.14f%c2C%16.14f",site+locationPrefix+mapPrefix,map.get("ulon"),'%',map.get("ulat"),'%',map.get("blon"),'%',map.get("blat"));
  }

  static String eventMapUrl(double lat,double lon,double radius) {
    return eventMapUrl(lat,lon,radius,1);
  }

  private double latitude;
  private double longitude;
  private double radius; /*in miles*/

  private keville.EventCache masterEventCache;
  private EventCache eventCache;
  private VenueCache venueCache;

  public EventbriteScanner(double latitude,double longitude,double radius,Properties props) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.radius = radius; /*in miles*/
    venueCache = new VenueCache(props);
    eventCache = new EventCache(props);
  }

  public int scan() {

      int page  = 0;
      int pages = 0;

      List<String> eventIds = new ArrayList<String>();
      System.out.println(String.format("beginning scan on %f,%f ", latitude, longitude));
      
      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); /* can concurrent instances use the same port? */
      System.out.println("scan job started on port "+proxy.getPort());
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

      WebDriver driver = new ChromeDriver(options);
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");

      String targetUrl = eventMapUrl(latitude,longitude,radius);
      System.out.println(targetUrl);

      // Hit first search page
      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      // find the total number of result pages
      String xPageOfKElementXPath = "/html/body/div[2]/div/div[2]/div/div/div/div[1]/div/main/div/div/section[1]/div/section/div/div/footer/div/div/ul/li[2]";
      WebElement xPageOfKElement = driver.findElement(By.xpath(xPageOfKElementXPath));
      if (xPageOfKElementXPath != null) {
        //System.out.println(xPageOfKElement.getText());
        String[] splits = xPageOfKElement.getText().split(" "); 
        if (splits.length != 3) {
          System.out.println("expected 3 splits for xPageOfKElement string but found "+(splits.length));
          System.out.println(xPageOfKElement.getText());
        } else {
          pages = Integer.parseInt(splits[2]);
        }
      } else {
        System.out.println("unable to determine how many pages are available");
      }
      System.out.println("found "+pages);


      int maxPagesToScrub = 10;
      int pageLoadDelay_ms = 1000;/*1 sec*/
      if (pages == 0) {
        maxPagesToScrub = 1;
      } else {
        System.out.println("multi page scrub");
        maxPagesToScrub = Math.min(maxPagesToScrub,pages);
      }

      //navigate to the remaing maxPagesToScrub-1 pages
      for ( int i = 1; i < maxPagesToScrub; i++ ) {
        targetUrl = eventMapUrl(latitude,longitude,radius,i+1);
        System.out.println(targetUrl);
        driver.get(targetUrl);
        try {
        Thread.sleep(pageLoadDelay_ms);
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }

        // Scroll to the bottom of the page
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)", "");

      }


      
      // Cleanup


      // get the HAR data
      Har har = proxy.getHar();

      Writer stringWriter = new StringWriter();
      try {
        har.writeTo(stringWriter);
        String rawContent = stringWriter.toString();
        //   \":\"456523472997\",\"
        //Pattern pat = Pattern.compile("(?<=eventbrite_event_id).*?(?=start)");
        Pattern pat = Pattern.compile("(?<=eventbrite_event_id\\\\\":\\\\\").*?(?=\\\\\",\\\\\"start)"); //what an ungodly creation
        Matcher mat = pat.matcher(rawContent);
        while (mat.find()) {
          //System.out.println(mat.group());
          eventIds.add(mat.group());
        }
        //filter event_id from rawContent
      } catch (Exception e ) {

      }

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      // Only process new event ids
      List<Event> events = eventIds
        .stream()
        .filter(ei -> !masterEventCache.has(ei))
        .map(ei -> createEventFrom(ei))
        .filter(e -> e != null)
        .collect(Collectors.toList());

      masterEventCache.addAll(events);

      //Package into Domain Event
      return events.size();

  }


  /* transform local event format to Event object */
  private Event createEventFrom(String eventId) {

    JsonObject eventJson = eventCache.get(eventId);
    if (eventJson == null) {
      System.err.println("error generating domain event for eventbrite id : " + eventId);
      return null;
    }

    String eventName = eventJson.getAsJsonObject("name").get("text").getAsString();

    String eventDescription = "";
    JsonElement eventDescriptionJson = eventJson.getAsJsonObject("description").get("text");
    if (!eventDescriptionJson.isJsonNull()) {
      eventDescription = eventDescriptionJson.getAsString();
    } 
     
    JsonObject eventStartJson = eventJson.getAsJsonObject("start");
    LocalDateTime start = ISOInstantToLocalDateTime(eventStartJson.get("utc").getAsString());

    String venueId = "";
    JsonElement venueIdElement = eventJson.get("venue_id");
    if (!venueIdElement.isJsonNull()) {
      venueId = venueIdElement.getAsString();
    }

    double latitude = 0;
    double longitude = 0;
    String city = "";
    String state = "";
    if (!venueId.isEmpty()) {
      JsonObject venue = venueCache.get(venueId);
      latitude  = Double.parseDouble(venue.get("latitude").getAsString());
      longitude = Double.parseDouble(venue.get("longitude").getAsString());
      JsonObject address = venue.getAsJsonObject("address");
      JsonElement cityJson = address.get("city");
      if (cityJson  != null && !cityJson.isJsonNull() )  {
        city = cityJson.getAsString();
      }
      JsonElement stateJson = address.get("region");
      if (stateJson != null && !stateJson.isJsonNull() )  {
        state = stateJson.getAsString();
      }
    } else {
      System.out.println("Venue: no venue information");
    }

    String url = eventJson.get("url").getAsString();

    return new Event(EventTypeEnum.EVENTBRITE,
        eventId,
        eventName,
        eventDescription,
        start,
        longitude,
        latitude,
        city,
        state,
        url
        );
  }

  /* this doesn't really belong in this class should belong in keville.util */
  //https://stackoverflow.com/questions/32826077/parsing-iso-instant-and-similar-date-time-strings
  public static LocalDateTime ISOInstantToLocalDateTime(String instantString) {
    DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
    Instant instant = Instant.from(dtf.parse(instantString));
    LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
    return localDateTime;
  }


}
