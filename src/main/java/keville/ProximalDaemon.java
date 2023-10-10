package keville;

import keville.providers.Providers;
import keville.providers.Eventbrite.EventCache;
import keville.event.EventService;
import keville.event.Event;
import keville.event.Events;
import keville.event.EventTypeEnum;
import keville.scanner.EventScannerScheduler;
import keville.scanner.ScanRoutine;
import keville.updater.EventUpdaterScheduler;
import keville.gson.InstantAdapter;
import keville.gson.FileAdapter;
import keville.util.GeoUtils;
import keville.settings.Settings;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.time.Instant;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static spark.Spark.*;

public class ProximalDaemon 
{
    static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProximalDaemon.class);
    static Thread scannerThread;
    static Thread updaterThread;
    static Settings settings;

    static double DEFAULT_LAT; 
    static double DEFAULT_LON; 
    static final double DEFAULT_RAD = Double.MAX_VALUE;

    static {
      initialize();
    }

    public static void main( String[] args )
    {
        Gson gson = new GsonBuilder()
          .registerTypeAdapter(Instant.class, new InstantAdapter())
          .registerTypeAdapter(File.class, new FileAdapter())
          .create();

        port(4567);

        get("/status", (request, response) -> { 
              LOG.info("recieved GET /status");

              String result = "Server : Online";
              result += "\nScan routines loaded : " + settings.scanRoutines.size();
              result += "\nCompilers loaded : " + settings.eventCompilers.size();
              result += "\nFilters loaded : " + settings.filters.size();

              return result;
        });

        get("/events", (request, response) -> 
            { 

              LOG.info("recieved GET /events");

              String routineName = null;
              String filterName = null;
              Predicate<Event> filter = null;
              Double radius = Double.MAX_VALUE;
              Double latitude = DEFAULT_LAT;
              Double longitude = DEFAULT_LON;

              //////////////////////////////
              // Determine Query Geography
              //////////////////////////////

              if (request.queryMap().hasKey("routine")) {
               
                routineName = request.queryMap().get("routine").value();
                ScanRoutine sr = settings.scanRoutines.get(routineName);
                if ( sr != null ) {
                  radius = sr.radius;
                  latitude = sr.latitude;
                  longitude = sr.longitude;
                } 

              } 

              // latitude,longitude, and radius query params can override routine
              
              if (request.queryMap().hasKey("radius")) {
                Double queryRadius = request.queryMap().get("radius").doubleValue();
                if ( queryRadius != null ) {
                  radius = queryRadius;
                }
              }

              if (request.queryMap().hasKey("latitude")) {
                Double queryLatitude = request.queryMap().get("latitude").doubleValue();
                if ( queryLatitude != null ) {
                  latitude = queryLatitude;
                } 
              }

              if (request.queryMap().hasKey("longitude")) {
                Double queryLongitude = request.queryMap().get("longitude").doubleValue();
                if ( queryLongitude != null ) {
                  longitude = queryLongitude;
                } 
              }

              //////////////////////////////
              // Primitive Filters
              //////////////////////////////

              Integer daysBefore;
              if (request.queryMap().hasKey("daysBefore")) {
                Integer queryDaysBefore = request.queryMap().get("daysBefore").integerValue();
                if ( queryDaysBefore != null ) {
                  daysBefore = queryDaysBefore;
                } else {
                  daysBefore = Integer.MAX_VALUE;
                }
              } else {
                daysBefore = Integer.MAX_VALUE;
              }

              boolean hideVirtual = request.queryMap().hasKey("virtual") && !request.queryMap().get("virtual").booleanValue();

              //////////////////////////////
              // Filter (User Defined) 
              //////////////////////////////

              if (request.queryMap().hasKey("filter")) {
               
                filterName = request.queryMap().get("filter").value();
                filter = settings.filters.get(filterName);
                if ( filter == null ) {

                  filter = new Predicate<Event>() {  //so cumbersome, why no Predicate.True?
                    public boolean test(Event x) { return true; }
                  };
                }

              } 


              LOG.info("processing request with parameters : " 
                  + ((routineName != null) ? " routine = " + routineName : "")
                  + ((filterName != null) ? " filter = " + filterName : "")
                  + " radius = " + radius 
                  + " latitude  = " + latitude 
                  + " longitude = " + longitude 
                  + " dayBefore " + daysBefore 
                  + " hideVirtual " + hideVirtual);

              // This is here because of the effectively final constraint for
              // lambdas, rewrite this .
              final double finalLat = latitude;
              final double finalLon = longitude;
              final Predicate<Event> finalFilter = filter;

              return gson.toJson(
                 EventService.getAllEvents()
                .stream()
                .filter( e -> e.eventType != EventTypeEnum.DEBUG )
                .filter(Events.InTheFuture())
                .filter(Events.WithinKMilesOf(latitude,longitude,radius))
                .filter( e -> !hideVirtual || !e.virtual)
                .filter(e -> finalFilter == null || finalFilter.test(e))
                .map( e -> Events.CreateClientEvent(e,finalLat,finalLon) )
                .collect(Collectors.toList())
              );
        });


        get("/routine", (request, response) ->  { 
          return gson.toJson(
            settings.scanRoutines.values()
            .stream()
            .collect(Collectors.toList())
          );
        });

        get("/compiler", (request, response) ->  { 
          return gson.toJson(
            settings.eventCompilers
            .stream()
            .collect(Collectors.toList())
          );
        });

        get("/filter", (request, response) ->  { 
          return gson.toJson(
            settings.filters.keySet()
            .stream()
            .collect(Collectors.toList())
          );
        });

        LOG.info("spark initialized");

    }

    static void initialize() {

      // in the future, we should check ~/.config/proximity/settings.json and adhere to XDG standard
      String settingsFileString = "./settings.json";
      Path settingsPath = FileSystems.getDefault().getPath(settingsFileString);

      try {

        String jsonString = new String(Files.readAllBytes(settingsPath),StandardCharsets.UTF_8);
        settings = Settings.parseSettings(jsonString);
        LOG.info("Configured from settings file :  "  + settingsPath.toString());

      } catch (Exception e) {

        LOG.error("Error parsing settings : " + settingsPath.toString());
        LOG.error(e.getMessage());
        System.exit(1);

      }

      Map<String,Double> coords = GeoUtils.getClientGeolocation();
      DEFAULT_LAT = coords.get("latitude"); 
      DEFAULT_LON = coords.get("longitude");

      LOG.info("settings loaded ...");
      LOG.info(settings.toString());

      Providers.init(settings);

      EventCache.applySettings(settings); //this doesn't seem like it belongs
                                          //here. We should remain Type 
                                          //agnostic here.
                                          
      EventService.applySettings(settings);

      EventScannerScheduler scannerScheduler = new EventScannerScheduler(settings);
      EventUpdaterScheduler updaterScheduler = new EventUpdaterScheduler(settings);


      scannerThread = new Thread(scannerScheduler, "ScannerThread");
      updaterThread = new Thread(updaterScheduler, "UpdaterThread");

      scannerThread.start();
      updaterThread.start();

    }

}
