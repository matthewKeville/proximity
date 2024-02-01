package keville.providers.meetup;

import keville.event.Event;
import keville.scanner.EventScanner;
import keville.location.Location;
import keville.util.GeoUtils;
import keville.scanner.ScanReport;
import keville.settings.Settings;
import keville.scanner.ProxyWebDriver;
import keville.scanner.DefaultProxyWebDriver;

import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.time.Instant;

import java.net.URLEncoder;
import org.openqa.selenium.JavascriptExecutor;
import net.lightbody.bmp.core.har.Har;

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
        return new ScanReport(scanStart,Instant.now(),Instant.now(),new LinkedList<Event>());
      }

      //TODO : replace with DI
      ProxyWebDriver proxyWebDriver = new DefaultProxyWebDriver();
      LOG.info("targetting url \n" + targetUrl);
      proxyWebDriver.getDriver().get(targetUrl);

      /* 
        meetup loads as we scroll, but if we scroll too fast it won't load
        all the data. So we scroll slowly until we notice we can't scroll anymore
      */

      JavascriptExecutor js = (JavascriptExecutor) proxyWebDriver.getDriver();;
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
      
      Har har = proxyWebDriver.getProxy().getHar();
      proxyWebDriver.kill();

      Instant processStart = Instant.now();
      List<Event> events = MeetupHarProcessor.process(har,targetUrl);

      events = events.stream()
        .distinct() 
        .collect(Collectors.toList());

      return new ScanReport(scanStart,processStart,Instant.now(),events);

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

      String targetUrl = null;

      try {

        targetUrl = String.format("https://www.meetup.com/find/?location=%s--%s--%s&source=EVENTS%s",
            URLEncoder.encode(location.country,"UTF-8"),
            URLEncoder.encode(location.region,"UTF-8"),
            URLEncoder.encode(location.locality,"UTF-8"),
            distanceString);

      } catch (Exception e)  {

        LOG.error("unable to create target url : " + targetUrl);
        LOG.error(e.getMessage());

      }

      return targetUrl;
  }


}
