package keville.settings.parser;

import keville.event.Event;
import keville.event.Events;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.time.DayOfWeek;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class FilterParser {

    static Logger LOG = LoggerFactory.getLogger(FilterParser.class);
    static Map<String,Predicate<Event>> customFilters = new HashMap<String,Predicate<Event>>();

    public static void registerCustomFilters(Map<String,Predicate<Event>> custom) {
        customFilters = custom;
    }

    public static Map<String,Predicate<Event>> parseEventFilters(JsonArray filtersJson) throws SettingsParserException {

      Map<String,Predicate<Event>> eventFilters = new HashMap<String,Predicate<Event>>();

      for ( JsonElement jsonElm : filtersJson ) {

        if ( !jsonElm.isJsonObject()) {
          throw new SettingsParserException("Invalid settings : filters array expects Object");
        }
        
        JsonObject filterJson = jsonElm.getAsJsonObject();

        if ( !filterJson.has("name") ) {
          throw new SettingsParserException("Invalid filter : name is a required field");
        }

        String name = ParseUtil.tryParseNonEmptyString(filterJson,"name","Invalid filter : name expects string");

        Predicate<Event> filter = parseEventFilter(filterJson);

        eventFilters.put(name,filter);

      }

      return eventFilters;

    }

    public static Predicate<Event> parseEventFilter(JsonObject filterJson) throws SettingsParserException {

      
      /////////////////////
      // conjunction

      boolean conjunction = true;
      if ( filterJson.has("conjunction") &&
          ParseUtil.tryParseBoolean(filterJson,"conjunction","Invalid filter : conjunction expects boolean")) {
        conjunction = true;
      }

      /////////////////////
      // filters (internal)

      if ( !filterJson.has("filters") )  {
        throw new SettingsParserException("Invalid filter : filters is a required field");
      } 
  
      JsonArray jsonFilterChain = ParseUtil.tryParseArray(filterJson,"filters","Invalid filter : filters expects array ");
      return parseFilterChain(jsonFilterChain, conjunction);

    }

    public static Predicate<Event> parseFilterChain(JsonArray filterChainJson,boolean conjunctive) throws SettingsParserException {

        Predicate<Event> filter = new Predicate<Event>() {  //so cumbersome, why no Predicate.True?
            public boolean test(Event x) { return true; }
        };

        for ( JsonElement filterJsonElm : filterChainJson ) {

          if ( !filterJsonElm.isJsonObject() ) {
            throw new SettingsParserException("Invalid filter chain : filters expects objects ");
          }

          JsonObject filterJson = filterJsonElm.getAsJsonObject();
          Predicate<Event> subFilter = parseFilter(filterJson);
          filter = (conjunctive) ? filter.and(subFilter) : filter.or(subFilter);

        }

        return filter;

    }

    // parse individual filters in the filter chain
    public static Predicate<Event> parseFilter(JsonObject filterJson) throws SettingsParserException {
    
        if ( !filterJson.has("type") ) {
          throw new SettingsParserException("Invalid filter : type is a required field");
        }

        String filterType = ParseUtil.tryParseNonEmptyString(filterJson,"type","Invalid filter : type expects string");

        switch (filterType) {

          case "disk":
            return parseDiskFilter(filterJson);
          case "inPerson":
            return parseInPersonFilter(filterJson);
          case "daysAwayRange":
            return parseDaysAwayRangeFilter(filterJson);
          case "weekdays":
            return parseWeekdaysFilter(filterJson);
          case "keywords":
            return parseKeywordsFilter(filterJson);
          case "custom":
            return parseCustomFilter(filterJson);
          default:
            throw new SettingsParserException("Invalid filter : type " + filterType + " is not recognized ");

        }

    }

    /*
     * {
     *  "type" : "custom"
     *  "name" : "name"
     * }
    */
    private static Predicate<Event> parseCustomFilter(JsonObject filterJson) throws SettingsParserException {

        if ( !filterJson.has("name") ) {
          throw new SettingsParserException("Invalid custom filter : name is a required field");
        }

        String name = ParseUtil.tryParseNonEmptyString(filterJson,"name","Invalid custom filter : name expects string");

        if ( !customFilters.containsKey(name) ) {
            LOG.error("referencing an unknown custom filter : " + name);
            throw new SettingsParserException("Invalid custom filter : " + name + " not in customFilters ");
        }

        return customFilters.get(name);
    }

    /*
     * {
     *  "type" : "inPerson"
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseInPersonFilter(JsonObject filterJson) throws SettingsParserException {

        if ( !filterJson.has("invert") ) {
          throw new SettingsParserException("Invalid inPerson filter : invert is a required field");
        }
        boolean invert = ParseUtil.tryParseBoolean(filterJson,"invert","Invalid inPerson filter : invert expects a boolean");
        return Events.InPerson(invert);

    }

    /*
     * {
     *  "type" : "disk"
     *  "radius" : 4.0,
     *  "latitude" : 40.3434,
     *  "longitude" : 34.0000
     * }
    */
    private static Predicate<Event> parseDiskFilter(JsonObject filterJson) throws SettingsParserException {

      if ( !filterJson.has("radius") ) {
        throw new SettingsParserException("Invalid disk filter : radius is a required field");
      }
      if ( !filterJson.has("latitude") ) {
        throw new SettingsParserException("Invalid disk filter : latitude is a required field");
      }
      if ( !filterJson.has("longitude") ) {
        throw new SettingsParserException("Invalid disk filter : longitude is a required field");
      }

      double radius = ParseUtil.tryParseDouble(filterJson,"radius","Invalid disk filter : radius expects double");
      double latitude = ParseUtil.tryParseDouble(filterJson,"latitude","Invalid disk filter : latitude expects double");
      double longitude = ParseUtil.tryParseDouble(filterJson,"longitude","Invalid disk filter : longitude expects double");

      return Events.WithinKMilesOf(latitude,longitude,radius);

    }

    /*
     * {
     *  "type" : "daysAwayRange",
     *  "minDays" : 0,
     *  "maxDays" : 7
     * }
    */
    private static Predicate<Event> parseDaysAwayRangeFilter(JsonObject filterJson) throws SettingsParserException {

      if ( !filterJson.has("minDays") ) {
        throw new SettingsParserException("Invalid daysAwayRange filter : minDays is a required field");
      }
      if ( !filterJson.has("maxDays") ) {
        throw new SettingsParserException("Invalid daysAwayRange filter : maxDays is a required field");
      }

      int minDays = ParseUtil.tryParseInt(filterJson,"minDays","Invalid daysAwayRange filter : minDays expects int");
      int maxDays = ParseUtil.tryParseInt(filterJson,"maxDays","Invalid daysAwayRange filter : maxDays expects int");

      return Events.DaysAwayRange(minDays,maxDays);

    }

    /*
     * {
     *  "type" : "weekdays",
     *  "days" : ["monday","thursday"]
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseWeekdaysFilter(JsonObject filterJson) throws SettingsParserException {

      if (!filterJson.has("days") ) {
        throw new SettingsParserException("Invalid weekdays filter : days is a required field");
      }

      JsonArray days = ParseUtil.tryParseArray(filterJson,"days","Invalid weekdays filter : days expects array");

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

      if (!filterJson.has("invert")) {
        throw new SettingsParserException("Invalid weekdays filter : invert is a required field");
      }
      boolean invert = ParseUtil.tryParseBoolean(filterJson,"invert","Invalid weekdays filter : invert expects boolean");

      return Events.Weekdays(daysOfWeek,invert);

    }

    /*
     * {
     *  "type" : "keywords",
     *  "keys" : ["the","keywords"],
     *  "caseInsensitive" : false,
     *  "invert" : false
     * }
    */
    private static Predicate<Event> parseKeywordsFilter(JsonObject filterJson) throws SettingsParserException {

      if (!filterJson.has("keys")) {
        throw new SettingsParserException("Invalid weekdays filter : keys is a required field");
      }

      JsonArray keys = ParseUtil.tryParseArray(filterJson,"keys","Invalid weekdays filter : keys expects Array");

      LinkedList<String> keywords = new LinkedList<String>();
      keys.forEach( k -> keywords.add(k.getAsString()));

      if (!filterJson.has("caseInsensitive")) {
        throw new SettingsParserException("Invalid weekdays filter : caseInsensitive is a required field");
      }
      boolean caseInsensitive = ParseUtil.tryParseBoolean(filterJson,"caseInsensitive","Invalid weekdays filter : caseInsensitive expects a boolean");

      if (!filterJson.has("invert")) {
        throw new SettingsParserException("Invalid weekdays filter : invert is a required field");
      }
      boolean invert = ParseUtil.tryParseBoolean(filterJson,"invert","Invalid weekdays filter : invert expects a boolean");

      return Events.Keywords(keywords,caseInsensitive,invert);
    }

}
