package keville.AllEvents;

import keville.Settings;
import keville.USStateAndTerritoryCodes;
import keville.util.GeoUtils;
import keville.Location;
import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.EventService;

import java.util.List;
import java.util.stream.Collectors;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.chrome.ChromeOptions;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

public class AllEventsScanner implements EventScanner {

  
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsScanner.class);

  public AllEventsScanner(Settings settings) {
  }

  public int scan(double latitude, double longitude, double radius) throws Exception {

      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);
      String targetUrl = createTargetUrl(location);
      if ( targetUrl == null ) {
        LOG.error("invalid target url , aborting scan ");
        LOG.error("location\n" + location.toString());
        return 0;
      }
      
      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0);
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
      options.addArguments("headless");

      WebDriver driver = new ChromeDriver(options);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");

      LOG.info("targetting initial url \n" + targetUrl);
      driver.get(targetUrl);

      // TODO expand result set until no more results appear

      Har har = proxy.getHar();

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      List<Event> events  = AllEventsHarProcessor.process(har,targetUrl);

      events = events.stream()
        .distinct() 
        .filter ( e -> !EventService.exists(EventTypeEnum.ALLEVENTS,e.eventId) )
        .collect(Collectors.toList());

      EventService.createEvents(events);

      LOG.info(" allevents scanner found  " + events.size());

      return events.size();

  }



  private String createTargetUrl(Location location) {


      if ( location.locality == null || location.region == null || location.country == null ) {
        return null;
      }

      if ( !USStateAndTerritoryCodes.isANSILStateCode(location.region) )  {
        LOG.warn("AllEvents needs an ANSIL state code " + location.region + " is not one");
        return null;
      }
     
      if ( !location.country.equals("us") ) {
        String warnMsg = "AllEvents scraping has only been tested in the us, searching against"
            .concat("\n\tcountry :  ").concat( location.country)
            .concat("\n\tregion :  ").concat(location.region)
            .concat("\n\tlocality :  ").concat(location.locality)
            .concat("\nis undefined behaviour ");
        LOG.warn(warnMsg);
      }

      //lowercase city , ANSI region code
      // https://allevents.in/Belmar-New Jersey/all   fails
      // https://allevents.in/belmar-nj/all           succeeds
      String locationString = location.locality.toLowerCase() + "-" + location.region.toLowerCase(); 
      String targetUrl = "https://allevents.in/" + locationString + "/all";

      return targetUrl;


  }


}
