package keville.providers.Eventbrite;

import keville.settings.Settings;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Not sure why we cache at all? */
@Component
public class EventCache {

  private static Logger LOG = LoggerFactory.getLogger(EventCache.class);

  private EventbriteAPI eventbriteAPI;
  private EventbriteRepository eventRepository;
  private Settings settings;

  public EventCache(@Autowired Settings settings,
      @Autowired EventbriteAPI eventbriteAPI,
      @Autowired EventbriteRepository eventbriteRepository) {
    this.settings = settings;
    this.eventbriteAPI = eventbriteAPI;
    this.eventRepository = eventbriteRepository;
  }

  public JsonObject getEventById(String eventId) throws UnlikelyEventIdException, EventbriteAPIException {

    if (!NumberUtils.isCreatable(eventId)) {
      throw new UnlikelyEventIdException("id string : " + eventId + " is an unlikely event id ");
    }

    //could theoretically throw JsonParseException, but its not expected to happen
    Optional<Event> event = eventRepository.findByEventId(eventId);
    if ( event.isPresent() ) {
      return JsonParser.parseString(event.get().json).getAsJsonObject();
    }

    JsonObject eventJson = eventbriteAPI.getEvent(eventId);
    eventRepository.save(new Event(eventId,eventJson.toString()));

    return eventJson;

  }

}
