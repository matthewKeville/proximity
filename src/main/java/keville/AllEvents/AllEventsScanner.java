package keville.AllEvents;

import keville.USStateAndTerritoryCodes;
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

public class AllEventsScanner implements EventScanner {

  private keville.EventService eventService;
  private Properties props;
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsScanner.class);

  public AllEventsScanner(keville.EventService eventService, Properties props) {
    this.eventService = eventService;
    this.props = props;
  }

  public int scan(double latitude, double longitude, double radius) {

      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);
      String targetUrl = createTargetUrl(location);
      if ( targetUrl == null ) {
        LOG.error("unusable target url , aborting scan ");
        return 0;
      }
      
      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); /* can concurrent instances use the same port? */
      LOG.info("Scan started on port "+proxy.getPort());
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

      WebDriver driver = new ChromeDriver(options);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");

      LOG.info("targetting initial url \n" + targetUrl);
      driver.get(targetUrl);

      //////////////////////////////////////////////////////////
      // TODO expand result set until no more results appear
      //////////////////////////////////////////////////////////

        //scroll down until we are at the same level as view more button

      // process HAR data from event list page
      
      Har har = proxy.getHar();
      StringWriter harStringWriter = new StringWriter();

      try {
        har.writeTo(harStringWriter);
      } catch (Exception e) {
        LOG.error("unable to extract HAR data as string");
        LOG.error(e.getMessage());
      }
      proxy.endHar();

      // extract the event stub data (incomplete data set for events)

      String eventStubsJson= AllEventsHarUtil.extractEventStubsJson(harStringWriter.toString(),targetUrl);

      List<String> eventUrls = new ArrayList<String>();
      JsonArray eventStubs = JsonParser.parseString(eventStubsJson).getAsJsonArray();

      List<Event> newEvents = new ArrayList<Event>();
      LOG.info("found " + eventStubs.size() + " event stubs ");
      for ( JsonElement eventStub : eventStubs ) {

        //create stub event in place of full event
        JsonObject eventStubJson = eventStub.getAsJsonObject();
        newEvents.add(createEventStubFrom(eventStubJson));

        /*
        String url = eventStub.getAsJsonObject().get("url").getAsString();
        // scrubEventDataFromUrl(Driver driver,Proxy proxy,String url);
        */

      }

      //close web driver

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      // send new events to event service

      newEvents = newEvents.stream()
        .distinct()
        .collect(Collectors.toList());
      eventService.createEvents(newEvents);

      LOG.info(" allEvents scanner generated " + newEvents.size() + " new events " );

      return newEvents.size();
  }

  /*
     This is a stub for a protocol that would visit event stub urls and grab the full event data
  */
  /*
  private Event scrubEventDataFromUrl(Driver driver,Proxy proxy,String url) {
  
  }
  */


  /* 
     this is a limited Event object that is missing other fields are
     required to really be an 'Event' such as Time (instead of just date) and a formal
     description. 

     For testing purposes, I will use this limited Event as an Event, while I run tests
     to see if full Event collection is feasible or desirable when the Event update protocol
     has yet to be formulated. The main reason why I am reticent to grab the full data is
     it would require a new web request per event which could seem suspicious to allEvents.in
  */
  private Event createEventStubFrom(JsonObject eventJson) {

      String url = eventJson.get("url").getAsString(); 
      String eventId = extractIdFromUrl(url);
      String eventName = eventJson.get("name").getAsString();

      String eventDescription = "unsupported for allevents.in";

      // we must convert this partial Date to a ISO_INSTANT (stub only has DATE not DATETIME)
      //stubs have a date : "2023-09-12"
      //Event expects and ISO Instant : "2023-09-12T23:00:00.000Z"
      String startDateString = eventJson.get("startDate").getAsString();
      String fakeTime = "T00:00:00.000Z";
      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startDateString+fakeTime));
  

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
        EventTypeEnum.ALLEVENTS,
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


  /*
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
    return null;
  }
  */


  private String createTargetUrl(Location location) {

      if ( location.state == null ) {
        LOG.warn("allevents scrubbing needs a valid US state, state is null");
        return null;
      }

      String ANSIStateCode = USStateAndTerritoryCodes.getANSILcode(location.state);
      if ( ANSIStateCode == null ) {
        LOG.warn("allevents scrubbing needs a valid ANSI US state code, state " + location.state  + " is not recognized by USStateAndTerritoryCodes");
        return null;
      }

      String locality = "";
      if ( location.town != null ) {
        locality = location.town;
      } else if ( location.village != null ) {
        locality = location.village;
      } else {
        LOG.warn("locality needed to scrape AllEvents.in");
        LOG.warn("town & village are both empty");
      }
  
      // note this url needs lowercase for state and locality otr 404 returned
      // note this url needs 2 letter state code
        // https://allevents.in/Belmar-New Jersey/all  fails
        // https://allevents.in/belmar-nj/all  succeeds
      String locationString = locality.toLowerCase() + "-" + ANSIStateCode.toLowerCase(); //toms river-nj //jackson-nj //jackson-ms
      String targetUrl = "https://allevents.in/" + locationString + "/all";

      return targetUrl;
  }

  //https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097
  //assuming the last bit is the eventId
  private String extractIdFromUrl(String url) {
    String [] splits = url.split("/");
    if ( splits.length == 0 ) {
      LOG.error("could not extract event id from url");
    } 
    return splits[splits.length-1];
  }

}
