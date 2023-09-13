package keville;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Properties;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class EventService {

  private String connectionString;
  private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventService.class);

  public EventService(Properties properties) {
    connectionString = properties.getProperty("connection_string");
  }

  /* locate an Event row in the database */
  public Event getEvent(/* pk id */int id) {
    Connection con = getDbConnection();
    Event event;
    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM EVENT WHERE ID=?;"); //what is the query?
      ps.setString(1,Integer.toString(id));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        event  = eventRowToEvent(rs);
        return event;
      } 
    } catch (SQLException e) {
      LOG.error("An error occurred retrieving an event from the database");
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }
    return null;
  }

  /* return a list of Events from the DB */
  public List<Event> getEvents(Predicate<Event> filter) {

    /* probably not a great idea to pull all events into memory, then filter.
     * sqlite is probably more optimized for filtering but that is more code overhead 
     */
    List<Event> allEvents = new ArrayList<Event>();
    Connection con = getDbConnection();
    try {
      String sql = "SELECT * FROM EVENT;";
      Statement stmt = con.createStatement();

      ResultSet rs = stmt.executeQuery(sql);
      while ( rs.next() ) {
        Event event  = eventRowToEvent(rs);
        allEvents.add(event);
      }
    } catch (SQLException e) {
      LOG.error("An error occurred retrieving events from the database");
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
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
    Connection con = getDbConnection();
    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM EVENT WHERE EVENT_ID=? AND SOURCE=?;");
      ps.setString(1,eventId);
      ps.setString(2,type.toString());
      ResultSet rs = ps.executeQuery();
      if ( rs.next() ) {
        //redundant?
        return (rs.getString("event_id").equals(eventId) && rs.getString("source").equals(type.toString()));
      }
    } catch (SQLException e) {
      LOG.error("An error occurred checking if an event exists { type, eventId } = { " + 
          type + " , " + eventId + " } ");
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
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

    Connection con = getDbConnection();

    //do not create duplicates
    if ( exists(event.eventType,event.eventId) ) { 
      LOG.warn(" event - " + "eventid : " +  event.id + "\n\t was requested to be created , but already exists ");
      return false; 
    }

    try {
      String queryTemplate = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE,URL,VIRTUAL)" +
        " VALUES ( ? , ? , ? , ? , ?, ?, ? , ?, ?);";
      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1,event.eventId);
      ps.setString(2,event.eventType.toString());
      ps.setString(3,event.name);
      ps.setString(4,event.description);
      ps.setString(5,event.start.toString());
      ps.setString(6,event.city);
      ps.setString(7,event.state);
      ps.setString(8,event.url);
      ps.setString(9,""+(event.virtual ? 1 : 0));
      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;

    } catch (SQLException e) {
      LOG.error("an error occurred creating an event in the database");
      LOG.error("event : \n" + event.toString());
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }

    return false;
  }

  /**
   * 
   * @return : true if all events were created.
   */
  public boolean createEvents(List<Event> events) {
    int fails = 0;
    boolean allPass = true;
    for ( Event e : events ) {
      boolean success = createEvent(e);
      if (!success) {
        fails++;
      }
      allPass &= success;
    }
    LOG.info("created  " + (events.size() - fails) + " of " + events.size() + " events for createEvents ");
    return allPass;
  }

  private Connection getDbConnection() {
    Connection con  = null;
    LOG.debug("connecting to " + connectionString);
    try {
      con = DriverManager.getConnection(connectionString);
      LOG.debug("connected to " + connectionString);
    } catch (SQLException e) {
      LOG.error("Critical error : unable to read events from database app.db");
      LOG.error(e.getMessage());
      System.exit(5);
    }
    return con;
  }

  private void closeDbConnection(Connection con) {
    try {
      con.close();
    } catch (Exception e) {
      LOG.error("unable to close db connection");
      LOG.error(e.getMessage());
    }
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
        LOG.error("error converting event row to Event object");
        LOG.error("mismatch between EventTypeEnum string in database and EventTypeEnum types");
        LOG.error("offending string : " + source);
        LOG.error(iae.getMessage());
      }
      int id = rs.getInt("id");
      String eventId = rs.getString("event_id");
      String name = rs.getString("name");
      String description = rs.getString("description");
      String startTimeString = rs.getString("start_time");

      Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTimeString));

      double longitude = rs.getDouble("longitude");                            
      double latitude = rs.getDouble("latitude");                            
      String city = rs.getString("city");
      String state = rs.getString("state");
      String url = rs.getString("url");
      boolean virtual = rs.getInt("virtual") == 1;

      EventBuilder eb = new EventBuilder();
      eb.setId(id);
      eb.setEventTypeEnum(eventType);
      eb.setName(name);
      eb.setDescription(description);
      eb.setStart(start);
      eb.setLongitude(longitude);
      eb.setLatitude(latitude);
      eb.setCity(city);
      eb.setState(state);
      eb.setUrl(url);
      eb.setVirtual(virtual);
      event = eb.build();

    } catch (SQLException se) {
      LOG.error("an error occured converting event row to Event object");
      LOG.error(se.getMessage());
    }
    return event;
  }

}
