package keville.meetup;

import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.util.DateTimeUtils;

import java.io.Writer;
import java.io.StringWriter;

import java.time.LocalDateTime;
import java.time.Duration;

import java.util.stream.Collectors;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private String city;
  private String state;

  /* 
   * this is very wrong city,state or lat / long in Eventbrite
   * cleary I need some Location interace 
   */
  public MeetupScanner(String city,String state, keville.EventService eventService) {
    this.eventService = eventService;
    this.city = city;
    this.state = state;
  }

  public int scan() {
      
      BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); /* can concurrent instances use the same port? */
      System.out.println("scan job started on port "+proxy.getPort());
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

      WebDriver driver = new ChromeDriver(options);
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");

      String countryString = "us"; //lower country code
      String cityString = city; //"Belmar"; // First Cap and lowercase
      String stateString = state; //"nj"; //lower
      String distanceString = "fiveMiles"; //need a distance otr infintite scrolling
      String targetUrl = String.format("https://www.meetup.com/find/?location=%s--%s--%s&source=EVENTS&distance=%s",countryString,stateString,cityString,distanceString);
      System.out.println(targetUrl);

      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      // Scroll to the bottom of the page
      JavascriptExecutor js = (JavascriptExecutor) driver;

      /* this is probably a usefl utility i.e. definition doesn't belong here*/ 
      // meetup loads as we scroll, but if we scroll to fast it won't load
      // all the data. So we scroll slowly until we notice we can't scroll anymore
      long lastScrollY = (long)js.executeScript("return window.scrollY");
      long scrollY = -1;
      while ( scrollY != lastScrollY ) {
        lastScrollY = scrollY;
        js.executeScript("window.scrollBy(0,1000)");
        scrollY = (long) js.executeScript("return window.scrollY");
        try {
          Thread.sleep(750/*ms*/); //potentially too fast?
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
      

      // Parse Har Data
      Har har = proxy.getHar();

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }


      Writer stringWriter = new StringWriter();
      String eventJsonListRaw = "";
      try {
        har.writeTo(stringWriter);
        String rawContent = stringWriter.toString();
        // event json data exists within two specific script tags ...
        //"(?<=<script type=\\\\\"application\\/ld\\+json\\\\\">\\[).*?(?=]</script>)"
        Pattern pat = Pattern.compile("(?<=<script type=\\\\\"application\\/ld\\+json\\\\\">\\[).*?(?=]</script>)");
        Matcher mat = pat.matcher(rawContent);
        while (mat.find()) {
          eventJsonListRaw+=mat.group();
        }
        //filter event_id from rawContent
      } catch (Exception e ) {
        System.out.println("unexpected har data");
      }

      // meetup sends a json where fields are delineated by \" instead of "
      // so we need to replace these. But care not to replace \\" which is how the escape
      // quotes in a json value field.
      String strippedJson = eventJsonListRaw;
      Pattern escapedQuote = Pattern.compile("(?<!\\\\)\\\\\\\"");
      Matcher mat2 = escapedQuote.matcher(strippedJson);
      strippedJson = mat2.replaceAll("\"");
      strippedJson="{ events : ["+strippedJson+"]}";


      // package json event data into application Event
      // Only process new event ids
      List<Event> newEvents = new ArrayList<Event>();

      if (!strippedJson.equals("")) {
        JsonObject eventJsonList = JsonParser.parseString(strippedJson).getAsJsonObject();
        JsonArray eventsArray = eventJsonList.getAsJsonArray("events");
        for (JsonElement jo : eventsArray) {
          JsonObject event = jo.getAsJsonObject();
          String id = eventBriteJsonId(event);
          if (!eventService.exists(EventTypeEnum.MEETUP,id)) {
            newEvents.add(createEventFrom(event));
          }
        }
      }


      eventService.createEvents(newEvents);

      //Package into Domain Event
      return newEvents.size();

  }


  /* transform local event format to Event object */
  private Event createEventFrom(JsonObject eventJson) {

      String eventId = eventBriteJsonId(eventJson);
      String url = eventJson.get("url").getAsString(); 

      String eventName = eventJson.get("name").getAsString();
      System.out.println("event eventName is : " + eventName);

      String eventDescription = eventJson.get("description").getAsString();
      System.out.println("description is " + eventDescription.substring(Math.min(30,eventDescription.length())));

      //start
      LocalDateTime start = DateTimeUtils.ISOInstantToLocalDateTime(eventJson.get("startDate").getAsString());
      System.out.println(start.toString());

      JsonObject location = eventJson.getAsJsonObject("location");
      JsonObject geo = location.getAsJsonObject("geo");
      String latitudeString = geo.get("latitude").getAsString();
      double latitude = Double.parseDouble(latitudeString);
      String longitudeString = geo.get("longitude").getAsString();
      double longitude = Double.parseDouble(longitudeString);
      System.out.println("lat " + latitude);
      System.out.println("lat " + longitude);

      // -> Location -> Address (Type==PostalAddress) -> addressLocality (city)
      // -> Location -> Address (Type==PostalAddress) -> addressRegion   (state)
      JsonObject address = location.getAsJsonObject("address");
      String city = "";
      String state = "";
      if ( address.get("@type").getAsString().equals("PostalAddress") ) {
        city = address.get("addressLocality").getAsString();
        state = address.get("addressRegion").getAsString();
      } else {
        System.out.println("unable to determine addressLocality and addressRegion because unknown Address Type " + address.get("@type").getAsString());
      }

      System.out.println("city " + city);
      System.out.println("state " + state);

      System.out.println("url is : " + url);

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
  }

  //I am assuming this last part is the eventId
  //https://www.meetup.com/monmouth-county-golf-n-sports-fans-social-networking/events/294738939/
  private String eventBriteJsonId(JsonObject eventJson) {
      String url = eventJson.get("url").getAsString(); 
      String[] splits = url.split("/");
      return splits[splits.length-1];
  }



}
