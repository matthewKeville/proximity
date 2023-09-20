package keville.meetup;

import keville.ScanReport;
import keville.Settings;
import keville.util.GeoUtils;
import keville.Location;
import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.EventService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;

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

public class MeetupScanner implements EventScanner {

  
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupScanner.class);

  public MeetupScanner(Settings settings) {
  }

  public ScanReport scan(double latitude, double longitude, double radius) throws Exception {

      Instant scanStart = Instant.now();

      Location location = GeoUtils.getLocationFromGeoCoordinates(latitude,longitude);

      String targetUrl = createTargetUrl(location,radius);
      if ( targetUrl == null ) {
        LOG.error("unusable target url , aborting scan ");
        LOG.error("location\n" + location.toString());
        return new ScanReport(scanStart,Instant.now(),Instant.now(),0,0);
      }
      LOG.info("targetting url \n" + targetUrl);
      
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

      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      /* 
        meetup loads as we scroll, but if we scroll too fast it won't load
        all the data. So we scroll slowly until we notice we can't scroll anymore
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

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      Instant processStart = Instant.now();
      List<Event> events = MeetupHarProcessor.process(har,targetUrl);

      events = events.stream()
        .distinct() 
        .filter ( e -> !EventService.exists(EventTypeEnum.MEETUP,e.eventId) )
        .collect(Collectors.toList());

      int successes = EventService.createEvents(events);

      return new ScanReport(scanStart,processStart,Instant.now(),events.size(),successes);

  }

  private String createTargetUrl(Location location,double radius) {

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

      String distanceString  = "";
      if ( radius <= 2.0 )  {
        distanceString  = "&distance=twoMiles";
      } else if ( radius <= 5.0 ) {
        distanceString  = "&distance=fiveMiles";
      } else if ( radius <=  10.0 ) {
        distanceString  = "&distance=tenMiles";
      } else if ( radius <= 25.0 ) {
        distanceString  = "&distance=twentyFiveMiles";
      } else if ( radius <= 50.0 ) {
        distanceString  = "&distance=fiftyMiles";
      } else if ( radius <= 100.0 ) {
        distanceString  = "&distance=hundredMiles";
      }

      String targetUrl = String.format("https://www.meetup.com/find/?location=%s--%s--%s&source=EVENTS%s",location.country,location.region,location.locality,distanceString);

      return targetUrl;
  }


}
