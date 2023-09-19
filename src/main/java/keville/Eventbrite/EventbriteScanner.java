package keville.Eventbrite;

import keville.Settings;
import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.EventService;

import keville.util.GeoUtils;

import java.time.Duration;

import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;

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

public class EventbriteScanner implements EventScanner {

  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventbriteScanner.class);

  public EventbriteScanner(Settings settings) {
  }

  public int scan(double latitude, double longitude, double radius) throws Exception {

      LOG.info(String.format("beginning scan on %f,%f ", latitude, longitude));

      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); // can concurrent instances use the same port?
      LOG.info("scan job started on port "+proxy.getPort());
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

      String targetUrl = eventMapUrl(latitude,longitude,radius);
      LOG.info("target url is " + targetUrl);

      // Hit first search page
      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      // find the total number of result pages
      int pages = 0;
      try {

        String xPageOfKElementXPath = "/html/body/div[2]/div/div[2]/div/div/div/div[1]/div/main/div/div/section[1]/div/section/div/div/footer/div/div/ul/li[2]";
        WebElement xPageOfKElement = driver.findElement(By.xpath(xPageOfKElementXPath));

        if (xPageOfKElementXPath != null) {

          String[] splits = xPageOfKElement.getText().split(" "); 
          if (splits.length != 3) {

            LOG.error("expected 3 splits for xPageOfKElement string but found "+(splits.length));
            LOG.error(xPageOfKElement.getText());
            
          } else {

            pages = Integer.parseInt(splits[2]);

          }

        } 
        LOG.info("found "+pages);

      } catch (Exception e) {

        LOG.error("unable to find the number of result pages");
        LOG.error(e.getMessage());
        LOG.error("defaulting to 1");

      }

      int maxPagesToScrub = 5;
      int pageLoadDelay_ms = 1000;/*1 sec*/

      if (pages == 0) {

        maxPagesToScrub = 1;

      } else {

        LOG.info("multi page scrub");
        maxPagesToScrub = Math.min(maxPagesToScrub,pages);
      }

      //navigate to the remaing maxPagesToScrub-1 pages
      for ( int i = 1; i < maxPagesToScrub; i++ ) {

        targetUrl = eventMapUrl(latitude,longitude,radius,i+1);
        LOG.info(targetUrl);
        driver.get(targetUrl);

        try {

        Thread.sleep(pageLoadDelay_ms);
        
        } catch (Exception e) {

          LOG.error("error sleeping thread");
          LOG.error(e.getMessage());

        }

        // Scroll to the bottom of the page
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0,document.body.scrollHeight)", "");

      }

      Har har = proxy.getHar();

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      List<Event> events = EventbriteHarProcessor.process(har);

      events = events.stream()
        .distinct() 
        .filter ( e -> !EventService.exists(EventTypeEnum.EVENTBRITE,e.eventId) )
        .collect(Collectors.toList());

      EventService.createEvents(events);

      LOG.info(" eventbrite scanner found  " + events.size());

      return events.size();

  }

  static String eventMapUrl(double lat,double lon,double radius,int page) {

    if (page <= 0 ) {
      LOG.error("non positive page indices are undefined");
      return "";
    }
    Map<String,Double> map = GeoUtils.radialBbox(lat,lon,radius);
    String site = "https://www.eventbrite.com/";
    String locationPrefix = "d/united-states/belmar-new/"; /* I don't think /united-states/ matters i.e. what's inside / / */
    String mapPrefix = String.format("?page=%d&bbox=",page);  //"?page=1&bbox=";
                                                              //
    return String.format("%s%16.14f%c2C%16.14f%c2C%16.14f%c2C%16.14f",site+locationPrefix+mapPrefix,map.get("ulon"),'%',map.get("ulat"),'%',map.get("blon"),'%',map.get("blat"));

  }

  static String eventMapUrl(double lat,double lon,double radius) {

    return eventMapUrl(lat,lon,radius,1);

  }

}
