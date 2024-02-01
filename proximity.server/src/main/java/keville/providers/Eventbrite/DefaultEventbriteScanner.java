package keville.providers.Eventbrite;

import keville.scanner.ScanReport;
import keville.settings.Settings;
import keville.event.Event;
import keville.scanner.ProxyWebDriver;
import keville.scanner.ProxyWebDriverFactory;
import keville.util.GeoUtils;

import java.time.Instant;

import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import net.lightbody.bmp.core.har.Har;

@Component
public class DefaultEventbriteScanner implements EventbriteScanner {

  private static Logger LOG = LoggerFactory.getLogger(DefaultEventbriteScanner.class);
  private Settings settings;
  private ProxyWebDriverFactory proxyWebDriverFactory;

  public DefaultEventbriteScanner(@Autowired Settings settings,
      @Autowired ProxyWebDriverFactory proxyWebDriverFactory) {
    this.settings = settings;
    this.proxyWebDriverFactory = proxyWebDriverFactory;
  }

  public ScanReport scan(double latitude, double longitude, double radius) throws Exception {

    ProxyWebDriver proxyWebDriver = proxyWebDriverFactory.getInstance();

    Instant scanStart = Instant.now();
    String targetUrl = eventMapUrl(latitude, longitude, radius);

    LOG.info("targetting url \n" + targetUrl);
    proxyWebDriver.getDriver().get(targetUrl);

    // find the total number of result pages
    int pages = 0;
    try {
      String xPageOfKElementXPath = "/html/body/div[2]/div/div[2]/div/div/div/div[1]/div/main/div/div/section[1]/div/section/div/div/footer/div/div/ul/li[2]";
      WebElement xPageOfKElement = proxyWebDriver.getDriver().findElement(By.xpath(xPageOfKElementXPath));

      if (xPageOfKElementXPath != null) {
        String[] splits = xPageOfKElement.getText().split(" ");
        if (splits.length != 3) {
          LOG.error("expected 3 splits for xPageOfKElement string but found " + (splits.length));
          LOG.error(xPageOfKElement.getText());
        } else {
          pages = Integer.parseInt(splits[2]);
        }
      }
      LOG.info("found " + pages);
    } catch (Exception e) {
      LOG.error("unable to find the number of result pages");
      LOG.error(e.getMessage());
      LOG.error("defaulting to 1");
    }

    int pageLoadDelay_ms = 1000;/* 1 sec */
    int pagesToScrub = 1;
    if (pages != 0) {
      pagesToScrub = Math.min(settings.eventbriteMaxPages(), pages);
    }

    // scrub pages
    for (int i = 1; i < pagesToScrub; i++) {

      targetUrl = eventMapUrl(latitude, longitude, radius, i + 1);
      LOG.info(targetUrl);
      proxyWebDriver.getDriver().get(targetUrl);

      // This happens common enough that it makes sense to make a utility
      // TryWait that does not throw
      try {
        Thread.sleep(pageLoadDelay_ms);
      } catch (Exception e) {
        LOG.error("error sleeping thread");
        LOG.error(e.getMessage());
      }

      // Scroll to the bottom of the page
      JavascriptExecutor js = (JavascriptExecutor) proxyWebDriver.getDriver();
      js.executeScript("window.scrollBy(0,document.body.scrollHeight)", "");

    }

    Har har = proxyWebDriver.getProxy().getHar();
    proxyWebDriver.kill();

    Instant processStart = Instant.now();
    List<Event> events = EventbriteHarProcessor.process(har);

    events = events.stream()
        .distinct()
        .collect(Collectors.toList());

    return new ScanReport(scanStart, processStart, Instant.now(), events);

  }

  static String eventMapUrl(double lat, double lon, double radius, int page) {

    if (page <= 0) {
      LOG.error("non positive page indices are undefined");
      return "";
    }
    Map<String, Double> map = GeoUtils.radialBbox(lat, lon, radius);
    String site = "https://www.eventbrite.com/";
    String locationPrefix = "d/united-states/belmar-new/";
    String mapPrefix = String.format("?page=%d&bbox=", page); // "?page=1&bbox=";
    return String.format("%s%16.14f%c2C%16.14f%c2C%16.14f%c2C%16.14f", site + locationPrefix + mapPrefix,
        map.get("ulon"), '%', map.get("ulat"), '%', map.get("blon"), '%', map.get("blat"));

  }

  static String eventMapUrl(double lat, double lon, double radius) {
    return eventMapUrl(lat, lon, radius, 1);
  }

}
