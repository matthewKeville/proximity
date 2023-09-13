package keville.meetup;

import keville.util.GeoUtils;
import keville.Location;
import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;

import java.io.StringWriter;

import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class MeetupScanner implements EventScanner {

  private keville.EventService eventService;
  private Properties props;
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupScanner.class);

  /* 
   * this is very wrong city,state or lat / long in Eventbrite
   * cleary I need some Location interace 
   */
  public MeetupScanner(keville.EventService eventService, Properties props) {
    this.eventService = eventService;
    this.props = props;
  }

  public int scan(double latitude, double longitude, double radius) {

      //My meetup scrubbing protocol requires huamn readable address formats ( city & state )
      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);
      LOG.info("location constructed from latitude and longitude");
      LOG.info(location.toString());
      
      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); /* can concurrent instances use the same port? */
      LOG.info("Scan started on port "+proxy.getPort());
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.addArguments("headless");
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

      WebDriver driver = new ChromeDriver(options);
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");

      String targetUrl = createTargetUrl(location); //TBD pass in radius
      LOG.info("targetting url \n" + targetUrl);

      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      // Scroll to the bottom of the page
      JavascriptExecutor js = (JavascriptExecutor) driver;

      /* this is probably a usefl utility i.e. definition doesn't belong here*/ 
      // meetup loads as we scroll, but if we scroll to fast it won't load
      // all the data. So we scroll slowly until we notice we can't scroll anymore
      long lastScrollY = (long)js.executeScript("return window.scrollY");
      long scrollY = -1;
      while ( scrollY != lastScrollY ) {
        lastScrollY = scrollY;
        js.executeScript("window.scrollBy(0,1000)");
        scrollY = (long) js.executeScript("return window.scrollY");
        try {
          Thread.sleep(750/*ms*/); //potentially too fast?
        } catch (Exception e) {
          LOG.error("error encountered trying to sleep thread");
          LOG.error(e.getMessage());
        }
      }
      
      Har har = proxy.getHar();
      StringWriter harStringWriter = new StringWriter();

      try {
        har.writeTo(harStringWriter);
      } catch (Exception e) {
        LOG.error("unable to extract HAR data as string");
        LOG.error(e.getMessage());
      }

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      String jsonData = MeetupHarUtil.extractEventsJson(harStringWriter.toString());

      // package json event data into application Event
      List<Event> newEvents = new ArrayList<Event>();
      if (!jsonData.equals("")) {
        JsonArray eventsArray = JsonParser.parseString(jsonData).getAsJsonArray();
        for (JsonElement jo : eventsArray) {
          JsonObject event = jo.getAsJsonObject();
          String id = eventBriteJsonId(event);
          // only process new event ids
          if (!eventService.exists(EventTypeEnum.MEETUP,id)) {
            newEvents.add(createEventFrom(event));
          }
        }
      } else {
        LOG.info("json data is empty!");
      }

      newEvents = newEvents.stream()
        .distinct() //not sure what the behaviour is here
        .collect(Collectors.toList());
      eventService.createEvents(newEvents);

      LOG.info(" meetup scanner generated " + newEvents.size() + " potential new events " );

      //Package into Domain Event
      return newEvents.size();
  }

  private String createTargetUrl(Location location) {

      String countryString = "us";          // 2 letter lowercase code | Ex. "us"       TODO: add country to Location
      String stateString = location.state;  // 2 letter lowercase code | Ex. "nj"
      String localeString = "";             // This is the name of what I call a 'city' but the api I use maps this to town / village / city ...
      if ( location.town != null ) {
        localeString = location.town;    // First Cap and Lowercase | Ex. "Belmar" 
      } else if ( location.village != null ) {
        localeString = location.village;    // First Cap and Lowercase | Ex. "Belmar" 
      } else {
        LOG.warn("location : lat=" + location.latitude + " lon=" + location.longitude + " does not map to a town or village as expected for meetup.com ");
      }
      String distanceString = "fiveMiles";  // english distance string | Ex. "fiveMiles" TODO: make programmatic
      String targetUrl = String.format("https://www.meetup.com/find/?location=%s--%s--%s&source=EVENTS&distance=%s",countryString,stateString,localeString,distanceString);

      return targetUrl;
  }


  //  transform local event format to Event object
  //  TODO : this should be tryToCreateEventFrom and possibly return an Event or nothing
  //    if the data passed does not qualitfy for an event
  private Event createEventFrom(JsonObject eventJson) {

      String eventId = eventBriteJsonId(eventJson);
      String url = eventJson.get("url").getAsString(); 
      String eventName = eventJson.get("name").getAsString();
      String eventDescription = eventJson.get("description").getAsString();

      String timestring = eventJson.get("startDate").getAsString();
      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestring));

      JsonObject location = eventJson.getAsJsonObject("location");
      String locationType = location.get("@type").getAsString();

      double latitude = 0;
      double longitude = 0;
      String city = null;
      String state = null;
      if ( locationType.equals("Place") ) {
        JsonObject geo = location.getAsJsonObject("geo");
        String latitudeString = geo.get("latitude").getAsString();
        String longitudeString = geo.get("longitude").getAsString();
        latitude = Double.parseDouble(latitudeString);
        longitude = Double.parseDouble(longitudeString);

        // -> Location -> Address (Type==PostalAddress) -> addressLocality (city)
        // -> Location -> Address (Type==PostalAddress) -> addressRegion   (state)
        JsonObject address = location.getAsJsonObject("address");
        if ( address.get("@type").getAsString().equals("PostalAddress") ) {
          city = address.get("addressLocality").getAsString();
          state = address.get("addressRegion").getAsString();
        } else {
          LOG.info("unable to determine addressLocality and addressRegion because unknown Address Type " + address.get("@type").getAsString());
        }
      } else if ( locationType.equals("VirtualLocation") ) {
        LOG.warn("found an event with a VirtualLocation which is currently unsupported");
        LOG.error("created an event with fake geo location data");
      } else {
        LOG.error("found an event with an unhandled Location type : " + locationType);
      }

    return new Event(
        eventId,
        EventTypeEnum.MEETUP,
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

  //I am assuming this last part is the eventId
  //https://www.meetup.com/monmouth-county-golf-n-sports-fans-social-networking/events/294738939/
  private String eventBriteJsonId(JsonObject eventJson) {
      String url = eventJson.get("url").getAsString(); 
      String[] splits = url.split("/");
      return splits[splits.length-1];
  }

}
