package keville.facebook;

import keville.Event;
import keville.EventScanner;
import keville.EventTypeEnum;
import keville.util.DateTimeUtils;

import java.io.Writer;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.File;

import java.time.LocalDateTime;
import java.time.Duration;

import java.util.stream.Collectors;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.firefox.FirefoxOptions;

import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.client.ClientUtil;
import com.browserup.harreader.model.Har;
import com.browserup.harreader.model.HarEntry;
import com.browserup.harreader.model.HarResponse;
import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarHeader;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarPostData;
import com.browserup.bup.proxy.CaptureType;


/*
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
*/

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class FacebookScanner implements EventScanner {

  private keville.EventCache masterEventCache;
  private String city;
  private String state;

  /* 
   * this is very wrong city,state or lat / long in Eventbrite
   * cleary I need some Location interace 
   */
  public FacebookScanner(String city,String state, keville.EventCache masterEventCache) {
    this.masterEventCache = masterEventCache;
    this.city = city;
    this.state = state;
  }

  public int scan() {
     
      BrowserUpProxy proxy = new BrowserUpProxyServer();
      //BrowserMobProxyServer proxy = new BrowserMobProxyServer();
      proxy.start(0); /* can concurrent instances use the same port? */
      System.out.println("scan job started on port "+proxy.getPort());
      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

      seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
      seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

      ChromeOptions options = new ChromeOptions();
      options.setCapability(CapabilityType.PROXY, seleniumProxy);
      options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

      WebDriver driver = new ChromeDriver(options);
      //WebDriver driver = new ChromeDriver(caps);
      //WebDriver driver = new FirefoxDriver(options);
      proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
      proxy.newHar("eventScanHar");


      //facebook search makes url search terms space separated
      //We transform Marco Island FL to Marco%20Island%20FL
      List<String> cityStateStrings = new ArrayList<String>(Arrays.asList(city.split(" "))); //Arrays.asList is a view we need a bona fide List
      cityStateStrings.addAll(Arrays.asList(state.split(" ")));
      Iterator<String> tokens = cityStateStrings.iterator();
      String query = tokens.next();
      while (tokens.hasNext()) {
        query = "%20" + tokens.next();
      }

      String targetUrl = String.format("https://facebook.com/events/search/?q=%s",query);
      System.out.println(targetUrl);

      driver.get(targetUrl);
      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

      // Scroll to the bottom of the page
      JavascriptExecutor js = (JavascriptExecutor) driver;

      /* this is probably a usefl utility i.e. definition doesn't belong here*/ 
      // meetup loads as we scroll, but if we scroll to fast it won't load
      // all the data. So we scroll slowly until we notice we can't scroll anymore
      int maxScrolls = 6;
      int currentScroll = 0;

      long lastScrollY = (long)js.executeScript("return window.scrollY");
      long scrollY = -1;
      while ( currentScroll != maxScrolls && scrollY != lastScrollY ) {
        lastScrollY = scrollY;
        js.executeScript("window.scrollBy(0,1000)");
        scrollY = (long) js.executeScript("return window.scrollY");
        currentScroll++;
        try {
          Thread.sleep(750/*ms*/); //potentially too fast?
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
      

      Map<String,String> headers = proxy.getAllHeaders();
      for ( String hval : headers.keySet() ) {
        String val = headers.get(hval);
        System.out.println(hval + "\n" + val);
        System.out.println("hmmm");
      }

      // Parse Har Data
      Har har = proxy.getHar();

      if (driver != null) {
        proxy.stop();
        driver.quit();
      }

      Writer stringWriter = new StringWriter();
      List<String> eventJsonSets = new ArrayList<String>();
      try {
        har.writeTo(stringWriter);
        String rawContent = stringWriter.toString();
        FileWriter fileWriter;
        File debugFile = new File("facebook-test");
        debugFile.createNewFile(); /*creates if not extant*/
        fileWriter = new FileWriter(debugFile);
        fileWriter.write(rawContent);
        fileWriter.close();

        String combinedResponseData = "";

        for ( HarEntry entry : har.getLog().getEntries() ) {
          HarRequest hreq = entry.getRequest();
          HarResponse hres = entry.getResponse();
          if ( hreq.getUrl().equals("https://www.facebook.com/api/graphql/") )  {
            HarContent hc = hres.getContent();
            //System.out.println(hc.getText());
            combinedResponseData += hc.getText();
          }
        }


        Pattern escapedQuote = Pattern.compile("(?<!\\\\)\\\\\\\"");
        // (?<={\\\\\"edges\\\\\":).*?](?=,\\\\\"filters)   -- is expanded to java escaped syntax below
        //Pattern pat = Pattern.compile("(?<=\\{\\\\\\\\\"edges\\\\\\\\\":).*?](?=,\\\\\\\\\"filters)");
        Pattern pat = Pattern.compile("(?<=\\{\\\\\\\"edges\\\\\\\":).*?](?=,\\\\\\\"filters)"); /* sourced from regex101.com */
        //Matcher mat = pat.matcher(rawContent);
        Matcher mat = pat.matcher(combinedResponseData);
        //this matcher finds event list strings [{e1},...,{e2}]
        while (mat.find()) {
          //fb uses \" for json field values so we replace with them with "
          String rawEventString = mat.group();
          System.out.println("raw match found");
          System.out.println(rawEventString);
          Matcher mat2 = escapedQuote.matcher(rawEventString);
          eventJsonSets.add(mat2.replaceAll("\""));
        }
        //filter event_id from rawContent
      } catch (Exception e ) {
        System.out.println("unexpected har data");
      }


      /*
      for ( String ejss : eventJsonSets ) {
        JsonElement ejs = JsonParser.parseString(ejss);
        if (ejs.isJsonArray()) {
          for (JsonElement ej : (JsonArray) ejs) {
            System.out.println("event object found : " + ej.toString());
          }
        }
      }
      */
      

      /*

      // package json event data into application Event
      // Only process new event ids
      List<Event> newEvents = new ArrayList<Event>();

      if (!strippedJson.equals("")) {
        JsonObject eventJsonList = JsonParser.parseString(strippedJson).getAsJsonObject();
        JsonArray eventsArray = eventJsonList.getAsJsonArray("events");
        for (JsonElement jo : eventsArray) {
          JsonObject event = jo.getAsJsonObject();
          String id = eventBriteJsonId(event);
          if (!masterEventCache.has(id)) {
            newEvents.add(createEventFrom(event));
          }
        }
      }


      masterEventCache.addAll(newEvents);

      //Package into Domain Event
      return newEvents.size();
      */
      return 0;

  }


  /* transform local event format to Event object */
  private Event createEventFrom(JsonObject eventJson) {

    /*

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

    return new Event(EventTypeEnum.MEETUP,
        eventId,
        eventName,
        eventDescription,
        start,
        longitude,
        latitude,
        city,
        state,
        url
        );

    */
    return null;
  }


}
