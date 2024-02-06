package keville.controller;

import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import keville.event.ClientEvent;
import keville.event.Event;
import keville.event.EventService;
import keville.event.EventTypeEnum;
import keville.event.Events;
import keville.gson.FileAdapter;
import keville.gson.InstantAdapter;
import keville.settings.ScanRoutine;
import keville.settings.Settings;

@RestController
public class WebController {

    private static final Logger LOG = LoggerFactory.getLogger(WebController.class);
    
    private Gson gson;
    private Settings settings;
    private EventService eventService;

    public WebController(@Autowired Settings settings, @Autowired EventService eventService) {
      this.settings = settings;
      this.eventService = eventService;
      this.gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantAdapter())
        .registerTypeAdapter(File.class, new FileAdapter())
        .create();
    }

    @GetMapping(value = {"/status"}) 
    public String getStatus() {
      
      LOG.info("recieved GET /status");

      String result = "Server : Online";
      result += "\nScan routines loaded : " + settings.scanRoutines().size();
      result += "\nCompilers loaded : " + settings.eventCompilers().size();
      result += "\nFilters loaded : " + settings.filters().size();

      return result;

    }

    @GetMapping(value = {"/events"})
    public Collection<ClientEvent> getEvents(
        @RequestParam Optional<String> routineNameParam,
        @RequestParam Optional<Double> radiusParam,
        @RequestParam Optional<Double> latitudeParam,
        @RequestParam Optional<Double> longitudeParam,
        @RequestParam Optional<Boolean> virtualParam,
        @RequestParam Optional<String> filterNameParam
        ) {

      LOG.info("recieved GET /events");

      Double latitude = 0.0;
      Double longitude = 0.0;
      Double radius = Double.MAX_VALUE;
      Boolean excludeVirtual = false;
      //ugh why no Predicate.true ..
      Predicate<Event> filter = new Predicate<Event>() {
        public boolean test(Event x) { return true; }
      };

      // fill in geography parameters with the routine, but defer
      // to explicit parameters over the routine template
      if ( routineNameParam.isPresent() ) {
        ScanRoutine routine = settings.scanRoutines().get(routineNameParam.get());
        if ( routine != null ) {
          if ( latitudeParam.isEmpty() ) {
            latitude = routine.latitude;
          }
          if ( longitudeParam.isEmpty() ) {
            longitude = routine.longitude;
          }
          if ( radiusParam.isEmpty() ) {
            radius = routine.radius;
          }
        }
      }

      // explicit params take precedence
      if ( latitudeParam.isPresent() ) {
        latitude = latitudeParam.get();
      }
      if ( longitudeParam.isPresent() ) {
        longitude = longitudeParam.get();
      }
      if ( radiusParam.isPresent() ) {
        radius = radiusParam.get();
      }
      if ( virtualParam.isPresent() ) {
        excludeVirtual = virtualParam.get();
      } 
      if ( filterNameParam.isPresent() ) {
        String name = filterNameParam.get();
        if ( settings.filters().containsKey(name) ) {
          filter = settings.filters().get(name);
        }
      }

      LOG.info("processing request with parameters : " 
          + (routineNameParam.isPresent()  ? " routine = " + routineNameParam.get() : "")
          + (filterNameParam.isPresent()   ? " filter = "  + filterNameParam.get()  : "")
          + " radius = "      + radius 
          + " latitude  = "   + latitude 
          + " longitude = "   + longitude 
          + " hideVirtual "   + excludeVirtual);

      return filterEvents(latitude,longitude,radius,excludeVirtual,filter);

    }

    @GetMapping(value = {"/routine"}) 
    public String getRoutines() {
      LOG.info("recieved GET /routine");
      return gson.toJson(
        settings.scanRoutines().values()
        .stream()
        .collect(Collectors.toList())
      );
    }

    @GetMapping(value = {"/compiler"}) 
    public String getCompilers() {
      LOG.info("recieved GET /routine");
      return gson.toJson(
        settings.eventCompilers()
        .stream()
        .collect(Collectors.toList())
      );
    }

    @GetMapping(value = {"/filter"}) 
    public String getFilters() {
      LOG.info("recieved GET /filter");
      return gson.toJson(
        settings.filters().keySet()
        .stream()
        .collect(Collectors.toList())
      );
    }

    // This method illustrates the constraint the the lamba values must
    // be effectively final. Passing them as formal params ensures this
    private Collection<ClientEvent> filterEvents(
        final double latitude,
        final double longitude,
        final double radius,
        final boolean excludeVirtual,
        final Predicate<Event> filter
      ) {
      return 
        eventService.getAllEvents()
          .stream()
          .filter(Events.InTheFuture())
          .filter(Events.WithinKMilesOf(latitude,longitude,radius))
          .filter(e -> !excludeVirtual || !e.virtual)
          .filter(e -> filter.test(e))
          .filter(e -> e.eventType != EventTypeEnum.DEBUG )
          .map( e -> Events.CreateClientEvent(e,latitude,longitude))
          .collect(Collectors.toList());
    }

}
