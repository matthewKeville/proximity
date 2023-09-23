package keville.AllEvents;

import keville.ScanReport;
import keville.settings.Settings;
import keville.USStateAndTerritoryCodes;
import keville.util.GeoUtils;
import keville.Location;
import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.EventService;

import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

import java.time.Duration;
import java.time.Instant;
import java.net.URLEncoder;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

public class AllEventsScanner implements EventScanner {

  
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsScanner.class);
  private Settings settings; 

  public AllEventsScanner(Settings settings) {
    this.settings = settings;
  }

  public ScanReport scan(double latitude, double longitude, double radius) throws Exception {

      Instant scanStart= Instant.now();

      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);
      String targetUrl = createTargetUrl(location);
      if ( targetUrl == null ) {
        LOG.error("invalid target url , aborting scan ");
        LOG.error("location\n" + location.toString());
        return new ScanReport(scanStart,Instant.now(),Instant.now(),new LinkedList<Event>());
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

      LOG.info("targetting " + targetUrl);

      int pages = 1;
      driver.get(targetUrl);

      // is there another page?

      List<WebElement> nextLinkMatches  = driver.findElements(By.xpath("//*[@id='show_more_events']"));

      if ( !nextLinkMatches.isEmpty() ) {

        WebElement nextLink  = nextLinkMatches.get(0);

        while ( nextLink !=  null && pages <= settings.alleventsMaxPages) {

          String  next  = nextLink.getAttribute("href").toString();
          LOG.info("targetting " + next);
          driver.get(next);
          pages++;

          nextLinkMatches  = driver.findElements(By.xpath("//*[@id='show_more_events']"));
          nextLink = !nextLinkMatches.isEmpty() ? nextLinkMatches.get(0) : null;

        }

      } else {
        LOG.warn("this search only had one page");
      }


      Har har = proxy.getHar();

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      Instant processStart = Instant.now();
      List<Event> events  = AllEventsHarProcessor.process(har,targetUrl,pages);

      events = events.stream()
        .distinct() 
        .collect(Collectors.toList());

      return new ScanReport(scanStart,processStart,Instant.now(),events);

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

      // https://allevents.in/asbury park-nj/all           succeeds but gets redirected to 
      // https://allevents.in/asbury%20park-nj/all           succeeds but gets redirected to 
      String locationString = location.locality.toLowerCase() + "-" + location.region.toLowerCase(); 
      try {
        locationString = URLEncoder.encode(locationString,"UTF-8"); // spaces need to be encoded
      } catch (Exception e) {
      }
      String targetUrl = "https://allevents.in/" + locationString + "/all";

      return targetUrl;


  }


}
