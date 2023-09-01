package keville;

import keville.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Properties;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class EventService {

  private Connection con;
  private String connectionString;

  public EventService(Properties properties) {
    
    connectionString = properties.getProperty("connection_string");
    System.out.println("connecting to " + connectionString);

    try {
      con = DriverManager.getConnection(connectionString);
      System.out.println("connected to " + connectionString);
    } catch (SQLException e) {
      System.out.println("Critical error : unable to read events from database app.db");
      System.out.println(e.getMessage());
      System.exit(5);
    }

  }

  /* locate an Event row in the database */
  public Event getEvent(/* pk id */int id) {
    Event event;
    try {
      String sql = "SELECT * FROM EVENT WHERE ID="+id+";"; //what is the query?
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        event  = eventRowToEvent(rs);
        return event;
      } 
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
    return null;
  }

  /* return a list of Events from the DB */
  public List<Event> getEvents(Predicate<Event> filter) {

    /* probably not a great idea to pull all events into memory, then filter.
     * sqlite is probably more optimized for filtering but that is more code overhead 
     */
    List<Event> allEvents = new ArrayList<Event>();
    try {
      String sql = "select * from event";
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      while ( rs.next() ) {
        Event event  = eventRowToEvent(rs);
        allEvents.add(event);
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    //filter
    List<Event> result = allEvents.stream().
      filter(filter).
      collect(Collectors.toList());
    return result;
  }

  /* get all events */
  public List<Event> getEvents() {
    Predicate<Event> tautology = new Predicate<Event>() {
      public boolean test(Event e) { return true; }
    };
    return getEvents(tautology);
  }

  /* 
   * determine if this event exists in the db 
   * Existence is determined by a unique combination of eventId (domain)
   * and eventType (source). [New events from scanners do not have id]
   */
  public boolean exists(EventTypeEnum type, String eventId) {
    try {
      String sql = "SELECT * FROM EVENT WHERE "
        + "EVENT_ID='" + eventId + "' AND "
        + "SOURCE='"   + type.toString() + "';";
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      if ( rs.next() ) {
        //redundant?
        return (rs.getString("event_id").equals(eventId) && rs.getString("source").equals(type.toString()));
      }
    } catch (SQLException e) {
      System.out.println("error checking event existance\n" +e.getMessage());
    }
    return false;
  }

  /*
  public boolean updateEvent(Event event) {
    //TODO 
    return true;
  }
  */

  /**
   * 
   * @return : true if event creation succeeds
   */
  public boolean createEvent(Event event) {

    //do not create duplicates
    if ( exists(event.eventType,event.eventId) ) { return false; }

    try {
      String sql = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE,URL)" 
        + "VALUES (" + "'" + event.eventId + "', "    //"'asd43',"
        + "'" + event.eventType.toString() + "', "    //"'DEBUG',"
        + "'" + event.name + "', "                    //"'yoga',"
        + "'" + event.description + "', "             //"'sunset yoga in bradley beach',"
        + "'" + event.start.toString() + "', "        //"'2023-08-30T21:00:00Z',"
        + "'" + event.city + "', "                    //"'Bradley',"
        + "'" + event.state + "', "                   //"'NJ'"
        + "'" + event.url + "'"                       //"'NJ'"
        +");";                                        //google.com

      System.out.println(sql);
      Statement stmt = con.createStatement();
      int rowsUpdated = stmt.executeUpdate(sql);
      return rowsUpdated == 1;

    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    return false;
  }

  /**
   * 
   * @return : true if all events were created.
   */
  public boolean createEvents(List<Event> events) {
    boolean allPass = true;
    for ( Event e : events ) {
      allPass &= createEvent(e);
    }
    return allPass;
  }

  // convert sqllite row to domain Event map
  private Event eventRowToEvent(ResultSet rs) {
    Event event = null;
    try {
      String source = rs.getString("source");
      EventTypeEnum eventType = null;
      try {
        eventType = EventTypeEnum.valueOf(source);
      } catch (IllegalArgumentException iae) {
        System.out.println("mismatch between EventTypeEnum string in database and EventTypeEnum types");
        System.out.println("offending string : " + source);
        System.out.println(iae.getMessage());
      }
      int id = rs.getInt("id");
      String eventId = rs.getString("event_id");
      String name = rs.getString("name");
      String description = rs.getString("description");
      String startTimeString = rs.getString("start_time");
      System.out.println("starttimestring");
      System.out.println(startTimeString);

      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTimeString));

      double longitude = rs.getDouble("longitude");                            
      double latitude = rs.getDouble("latitude");                            
      String city = rs.getString("city");
      String state = rs.getString("state");
      String url = rs.getString("url");
      event = new Event(id,eventId,eventType,name,description,start,longitude,latitude,city,state,url);
    } catch (SQLException se) {
      System.out.println(se.getMessage());
    }
    return event;
  }

}
