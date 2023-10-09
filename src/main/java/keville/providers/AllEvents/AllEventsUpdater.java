package keville.providers.AllEvents;

import keville.event.Event;
import keville.event.EventBuilder;
import keville.event.EventTypeEnum;
import keville.updater.EventUpdater;
import keville.util.SchemaUtil;

import java.util.List;
import java.time.Duration;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;



public class AllEventsUpdater implements EventUpdater {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsUpdater.class);

    public Event updateEvent(Event event) {

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
      options.addArguments("headless");

      WebDriver driver = new ChromeDriver(options);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      LOG.info("targetting " + event.url + " to update event : " + event.id);
      driver.get(event.url);

      List<WebElement> eventJsonScriptElements = driver.findElements(By.xpath("//*[@id=\"event-container\"]/script[1]"));
      if ( eventJsonScriptElements.size() != 1 ) {
        LOG.warn("unable to update event : " + event.id);
        return null;
      }

      String eventJsonString = eventJsonScriptElements.get(0).getAttribute("text");
      JsonObject jsonData = JsonParser.parseString(eventJsonString).getAsJsonObject();
      EventBuilder eb = SchemaUtil.createEventFromSchemaEvent(jsonData);
      eb.setEventId(event.eventId);
      eb.setEventTypeEnum(EventTypeEnum.ALLEVENTS);
      return eb.build();

    }

}
