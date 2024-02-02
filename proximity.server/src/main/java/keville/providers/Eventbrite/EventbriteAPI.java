package keville.providers.Eventbrite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import keville.settings.Settings;

@Component
public class EventbriteAPI {

  private static Logger LOG = LoggerFactory.getLogger(EventbriteAPI.class);
  private static final String eventBaseUri = "https://www.eventbriteapi.com/v3/events/";
  private Settings settings;

  public EventbriteAPI(@Autowired Settings settings) {
    this.settings = settings;
  }

  public JsonObject getEvent(String eventId) throws EventbriteAPIException {

    HttpClient httpClient = HttpClient.newHttpClient();

    try {

      // EB API has expansions that pull in data from other endpoints (organizer &
      // venue)
      URI uri = new URI(eventBaseUri + eventId + "/" + "?expand=organizer,venue");
      HttpRequest request = HttpRequest.newBuilder()
          .uri(uri)
          .header("Authorization", "Bearer " + settings.eventbriteApiKey())
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

      if (response.statusCode() != 200) {

        LOG.warn(" Recieved " + response.statusCode() + " from event brite api ");
        throw new EventbriteAPIException("Bad response " + response.toString());

      } else {
        return JsonParser.parseString(response.body()).getAsJsonObject();
      }

    } catch (URISyntaxException | InterruptedException | IOException ex) {

      if (ex instanceof URISyntaxException) {
        throw new EventbriteAPIException("error building request URI : " + ex.getMessage());
      } else if (ex instanceof InterruptedException || ex instanceof IOException) {
        throw new EventbriteAPIException("error sending requeset : " + ex.getMessage());
      } else {
        throw new EventbriteAPIException("impossible exception");
      }

    }
  }

}
