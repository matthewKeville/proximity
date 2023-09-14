package keville.meetup;

import keville.util.GeoUtils;
import keville.Location;
import keville.Event;
import keville.SchemaUtil;
import keville.EventBuilder;
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

  public MeetupScanner(keville.EventService eventService, Properties props) {
    this.eventService = eventService;
    this.props = props;
  }

  public int scan(double latitude, double longitude, double radius) {

      //My meetup scrubbing protocol requires huamn readable address formats ( city & state )
      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);
      
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
      if ( targetUrl == null ) {
        LOG.error("unusable target url , aborting scan ");
        LOG.error("location\n" + location.toString());
        return 0;
      }
      LOG.info("targetting url \n" + targetUrl);

      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));


      /* 
        meetup loads as we scroll, but if we scroll too fast it won't load
        all the data. So we scroll slowly until we notice we can't scroll anymore
        this is probably a useful utility, so definition doesn't belong here 
      */

      JavascriptExecutor js = (JavascriptExecutor) driver;
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
          String id = meetupJsonId(event);

          // only process new event ids
          if (!eventService.exists(EventTypeEnum.MEETUP,id)) {
            newEvents.add(createEventFrom(event));
          }
        }

      } else {
        LOG.info("json data is empty!");
      }

      newEvents = newEvents.stream()
        .distinct() 
        .collect(Collectors.toList());
      eventService.createEvents(newEvents);

      LOG.info(" meetup scanner generated " + newEvents.size() + " potential new events " );

      return newEvents.size();
  }

  private String createTargetUrl(Location location) {

      //         "belmar"                       "nj"                    "us"
      if ( location.locality == null || location.region == null || location.country == null ) {
        return null;
      }
     
      if ( !location.country.equals("us") ) {
        String warnMsg = "Meetup scraping has only been tested in the us, searching against"
            .concat("\n\tcountry :  ").concat( location.country)
            .concat("\n\tregion :  ").concat(location.region)
            .concat("\n\tlocality :  ").concat(location.locality)
            .concat("\nis undefined behaviour ");
        LOG.warn(warnMsg);
      }

      String distanceString = "fiveMiles";  // english distance string | Ex. "fiveMiles" TODO: make programmatic
      String targetUrl = String.format("https://www.meetup.com/find/?location=%s--%s--%s&source=EVENTS&distance=%s",location.country,location.region,location.locality,distanceString);

      return targetUrl;
  }


  private Event createEventFrom(JsonObject eventJson) {

    EventBuilder eb = SchemaUtil.createEventFromSchemaEvent(eventJson);
    eb.setEventTypeEnum(EventTypeEnum.MEETUP);
    eb.setEventId(meetupJsonId(eventJson)); 

    return eb.build();
  }


  //I am assuming this last part is the eventId
  //https://www.meetup.com/monmouth-county-golf-n-sports-fans-social-networking/events/294738939/
  private String meetupJsonId(JsonObject eventJson) {
      String url = eventJson.get("url").getAsString(); 
      String[] splits = url.split("/");
      return splits[splits.length-1];
  }

}
