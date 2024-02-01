package keville.providers.AllEvents;

import java.net.URLEncoder;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import keville.event.Event;
import keville.location.Location;
import keville.location.USStateAndTerritoryCodes;
import keville.scanner.DefaultProxyWebDriver;
import keville.scanner.EventScanner;
import keville.scanner.ProxyWebDriver;
import keville.scanner.ScanReport;
import keville.settings.Settings;
import keville.util.GeoUtils;
import net.lightbody.bmp.core.har.Har;

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
        LOG.error("usnusable target url , aborting scan ");
        LOG.error("location\n" + location.toString());
        return new ScanReport(scanStart,Instant.now(),Instant.now(),new LinkedList<Event>());
      }


      //TODO : replace with DI
      ProxyWebDriver proxyWebDriver = new DefaultProxyWebDriver();
      LOG.info("targetting " + targetUrl);
      proxyWebDriver.getDriver().get(targetUrl);

      int pages = 1;
      proxyWebDriver.getDriver().get(targetUrl);

      // is there another page?
      List<WebElement> nextLinkMatches  = proxyWebDriver.getDriver().findElements(By.xpath("//*[@id='show_more_events']"));

      if ( !nextLinkMatches.isEmpty() ) {
        WebElement nextLink  = nextLinkMatches.get(0);
        while ( nextLink !=  null && pages <= settings.alleventsMaxPages()) {
          String  next  = nextLink.getAttribute("href").toString();
          LOG.info("targetting " + next);
          proxyWebDriver.getDriver().get(next);
          pages++;
          nextLinkMatches  = proxyWebDriver.getDriver().findElements(By.xpath("//*[@id='show_more_events']"));
          nextLink = !nextLinkMatches.isEmpty() ? nextLinkMatches.get(0) : null;
        }
      } 

      LOG.debug("scanning " + pages);

      Har har = proxyWebDriver.getProxy().getHar();
      proxyWebDriver.kill();

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
