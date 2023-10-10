package keville.settings;

import keville.event.Event;
import keville.event.Events;
import java.util.function.Predicate;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.time.DayOfWeek;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class Filters {

    static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Filters.class);
    static Map<String,Predicate<Event>> customFilters = new HashMap<String,Predicate<Event>>();

    public static void registerCustomFilters(Map<String,Predicate<Event>> custom) {
        customFilters = custom;
    }

    public static Predicate<Event> parseFilterChain(JsonArray filterChainJson,boolean conjunctive) {

        Predicate<Event> filter = new Predicate<Event>() {  //so cumbersome, why no Predicate.True?
            public boolean test(Event x) { return true; }
        };

        for ( JsonElement filterJsonElm : filterChainJson ) {

          JsonObject filterJson = filterJsonElm.getAsJsonObject();
          Predicate<Event> subFilter = parseFilter(filterJson);

          if ( subFilter != null ) {

            filter = (conjunctive) ? filter.and(subFilter) : filter.or(subFilter);

          } else {

            LOG.warn("part of the filter chain is misconfigured");

          }

        }

        return filter;

    }

    public static Predicate<Event> parseFilter(JsonObject filterJson) {
    
        if ( !filterJson.has("type") ) {

          LOG.error("misconfigured filter, you must provide a \"type\"");
          return null;

        }

        String filterType = filterJson.get("type").getAsString();

        switch (filterType) {

          case "disk":
            
            return parseDiskFilter(filterJson);

          case "inPerson":

            return parseInPersonFilter(filterJson);

          case "daysAwayRange":

            return parseDaysAwayRangeFilter(filterJson);

          case "weekdays":

            return parseWeekdaysFilter(filterJson);

          //case "months": //Implemented in Events.java

          case "keywords":

            return parseKeywordsFilter(filterJson);

          case "custom":

            return parseCustomFilter(filterJson);

          default:

            LOG.warn("filter type : " + filterType + " is unknown");
            return null;

        }

    }

    private static Predicate<Event> parseCustomFilter(JsonObject filterJson) {

        final String filterName = "custom";

        if ( !loggedKeyCheck(filterJson,"name",filterName) ) {
          return null;
        }
        String name = filterJson.get("name").getAsString();

        if ( !customFilters.containsKey(name) ) {
            LOG.error("referencing an unknown custom filter : " + name);
            return null;
        }
        return customFilters.get(name);
    }

    /*
     * {
     *  "type" : "inPerson"
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseInPersonFilter(JsonObject filterJson) {

        final String filterName = "inPerson";

        if ( !loggedKeyCheck(filterJson,"invert",filterName) ) {
          return null;
        }
        return Events.InPerson(filterJson.get("invert").getAsBoolean());

    }

    /*
     * {
     *  "type" : "disk"
     *  "radius" : 4.0,
     *  "latitude" : 40.3434,
     *  "longitude" : 34.0000
     * }
    */
    private static Predicate<Event> parseDiskFilter(JsonObject filterJson) {

        final String filterName = "disk";

        if ( !loggedKeyCheck(filterJson,"radius",filterName) ) {
          return null;
        }
        Double radius = filterJson.get("radius").getAsDouble(); 

        if ( !loggedKeyCheck(filterJson,"latitude",filterName) ) {
          return null;
        }
        Double latitude = filterJson.get("latitude").getAsDouble(); 

        if ( !loggedKeyCheck(filterJson,"longitude",filterName) ) {
          return null;
        }
        Double longitude = filterJson.get("longitude").getAsDouble(); 

        return Events.WithinKMilesOf(latitude,longitude,radius);

    }

    /*
     * {
     *  "type" : "daysAwayRange",
     *  "minDays" : 0,
     *  "maxDays" : 7
     * }
    */
    private static Predicate<Event> parseDaysAwayRangeFilter(JsonObject filterJson) {

        final String filterName = "daysAwayRange";

        if ( !loggedKeyCheck(filterJson,"minDays",filterName) ) {
          return null;
        }
        Integer minDays = filterJson.get("minDays").getAsInt(); 

        if ( !loggedKeyCheck(filterJson,"maxDays",filterName) ) {
          return null;
        }
        Integer maxDays = filterJson.get("maxDays").getAsInt(); 

        return Events.DaysAwayRange(minDays,maxDays);

    }

    /*
     * {
     *  "type" : "weekdays",
     *  "days" : ["monday","thursday"]
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseWeekdaysFilter(JsonObject filterJson) {

        final String filterName = "weekdays";

        if ( !loggedKeyCheck(filterJson,"days",filterName) ) {
          return null;
        }
        JsonArray days = filterJson.get("days").getAsJsonArray();
        LinkedList<DayOfWeek> daysOfWeek = new LinkedList<DayOfWeek>();
        days.forEach( day -> {
                switch (day.getAsString().toUpperCase()) {
                    case "MONDAY":
                      daysOfWeek.add(DayOfWeek.MONDAY);
                    case "TUESDAY":
                      daysOfWeek.add(DayOfWeek.TUESDAY);
                    case "WEDNESDAY":
                      daysOfWeek.add(DayOfWeek.WEDNESDAY);
                    case "THURSDAY":
                      daysOfWeek.add(DayOfWeek.THURSDAY);
                    case "FRIDAY":
                      daysOfWeek.add(DayOfWeek.FRIDAY);
                    case "SATURDAY":
                      daysOfWeek.add(DayOfWeek.SATURDAY);
                    case "SUNDAY":
                      daysOfWeek.add(DayOfWeek.SUNDAY);
                }
        });

        if ( !loggedKeyCheck(filterJson,"invert",filterName) ) {
          return null;
        }
        boolean invert = filterJson.get("invert").getAsBoolean(); 

        return Events.Weekdays(daysOfWeek,invert);
    }

    /*
     * {
     *  "type" : "keywords",
     *  "keys" : ["the","keywords"]
     *  "caseSensitive" : false
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseKeywordsFilter(JsonObject filterJson) {

        final String filterName = "keywords";

        if ( !loggedKeyCheck(filterJson,"keys",filterName) ) {
          return null;
        }
        JsonArray keys = filterJson.get("keys").getAsJsonArray();
        LinkedList<String> keywords = new LinkedList<String>();
        keys.forEach( k -> keywords.add(k.getAsString()));

        if ( !loggedKeyCheck(filterJson,"caseInsensitive",filterName) ) {
          return null;
        }
        boolean caseInsensitive = filterJson.get("caseInsensitive").getAsBoolean();

        if ( !loggedKeyCheck(filterJson,"invert",filterName) ) {
          return null;
        }
        boolean invert = filterJson.get("caseSensitive").getAsBoolean(); 

        return Events.Keywords(keywords,caseInsensitive,invert);
    }

    /* 
     * Check if key exists for a filter, log diagnostics
     */
    private static boolean loggedKeyCheck(JsonObject json,String key,String filterName) {
        if (!json.has(key)) {
          LOG.error("filter : " + filterName + " requires key : " + key);
          return false;
        }
        return true;
    }

}
