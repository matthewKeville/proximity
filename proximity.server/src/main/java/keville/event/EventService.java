package keville.event;

import keville.merger.EventMerger;
import keville.providers.Providers;
import keville.scanner.ScannedEventsReport;
import keville.settings.Settings;
import keville.location.LocationBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class EventService {

  private static Settings settings;
  private static Logger LOG = LoggerFactory.getLogger(EventService.class);

  public static void init(Settings s) {
    settings = s;
    initDb();
  }

  public static Event getEvent(/* pk id */int id) {
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
  public static List<Event> getEvents(Predicate<Event> filter) {

    /* probably not a great idea to pull all events into memory, then filter.
     * sqlite is probably more optimized for filtering but that is more code overhead 
     */
    List<Event> allEvents = new LinkedList<Event>();
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
  public static List<Event> getAllEvents() {

    Predicate<Event> tautology = new Predicate<Event>() {
      public boolean test(Event e) { return true; }
    };

    return getEvents(tautology);
  }

  public static List<Event> getOudatedEvents(int batchSize,Duration maxAcceptableAge) {

    Predicate<Event> filter = (e -> e.status == EventStatusEnum.INCOMPLETE);
    filter = filter.or(e ->  e.lastUpdate.plus(maxAcceptableAge).isBefore(Instant.now()));
    filter = filter.and(e ->  e.status != EventStatusEnum.QUARENTINE);

    return getEvents(filter).stream().limit(batchSize).collect(Collectors.toList());

  }

  /* 
   * determine if this event exists in the db 
   * Existence is determined by a unique combination of eventId (domain)
   * and eventType (source). [New events from scanners do not have id]
   */
  public static boolean exists(EventTypeEnum type, String eventId) {
    return getEventById(type,eventId) != null;
  }

  /**
     update an event row in the database, modifiying it's LAST_UPDATE timestamp.
     @return : update success
  */
  public static boolean updateEvent(Event event) {    

    Connection con = getDbConnection();

    try {

    String queryTemplate = 
      " UPDATE " + 
        " EVENT " +
      " SET " +
        " EVENT_ID = ?, " +
        " SOURCE = ?, " +
        " NAME = ?, " +
        " DESCRIPTION = ?, " +
        " START_TIME = ?, " + 
        " END_TIME = ?, " + 
        " LOCATION_NAME = ?, " +
        " COUNTRY = ?, " + 
        " REGION = ?, " +
        " LOCALITY = ?, " +
        " STREET_ADDRESS = ?, " +
        " LATITUDE = ?, " +
        " LONGITUDE = ?, " +
        " ORGANIZER = ?, " +
        " URL = ?, " +
        " VIRTUAL = ?, " +
        " STATUS = ?, " +
        " LAST_UPDATE  = ? " +
      " WHERE " + 
        " ID = ? ";

      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1,event.eventId);
      ps.setString(2,event.eventType.toString());
      ps.setString(3,event.name);
      ps.setString(4,event.description);
      ps.setString(5,event.start.toString());
      ps.setString(6,event.end.toString());
      ps.setString(7,event.location.name);
      ps.setString(8,event.location.country);
      ps.setString(9,event.location.region);
      ps.setString(10,event.location.locality);
      ps.setString(11,event.location.streetAddress);
      if ( event.location.longitude == null || event.location.latitude == null ) {
        ps.setObject(12,null);
        ps.setObject(13,null);
      } else {
        ps.setDouble(12,event.location.latitude);
        ps.setDouble(13,event.location.longitude);
      }
      ps.setString(14,event.organizer);
      ps.setString(15,event.url);
      ps.setString(16,""+(event.virtual ? 1 : 0));
      ps.setString(17,event.status.toString());
      ps.setString(18,Instant.now().toString());

      ps.setInt(19,event.id);

      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;

    } catch (Exception e) {
      LOG.error("an error occurred updating an event in the database, id : " + event.id);
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }

    return false;

  }


  /**
   * 
   * @return : true if event creation succeeds
   */
  public static boolean createEvent(Event event) {

    Connection con = getDbConnection();

    //do not create duplicates
    if ( exists(event.eventType,event.eventId) ) { 
      LOG.warn(" event - " + "eventid : " +  event.id + "\n\t was requested to be created , but already exists ");
      return false; 
    }

    try {


    String queryTemplate = "INSERT INTO EVENT (" +
      "EVENT_ID,SOURCE,NAME," +
      "DESCRIPTION,START_TIME,END_TIME," + 
      "LOCATION_NAME," +
      "COUNTRY,REGION,LOCALITY," +
      "STREET_ADDRESS,LATITUDE,LONGITUDE," +
      "ORGANIZER,URL,VIRTUAL," +
      "STATUS,LAST_UPDATE" +
      ") VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

      PreparedStatement ps = con.prepareStatement(queryTemplate);
      ps.setString(1,event.eventId);
      ps.setString(2,event.eventType.toString());
      ps.setString(3,event.name);
      ps.setString(4,event.description);
      ps.setString(5,event.start.toString());
      ps.setString(6,event.end.toString());
      ps.setString(7,event.location.name);
      ps.setString(8,event.location.country);
      ps.setString(9,event.location.region);
      ps.setString(10,event.location.locality);
      ps.setString(11,event.location.streetAddress);
      if ( event.location.longitude == null || event.location.latitude == null ) {
        ps.setObject(12,null);
        ps.setObject(13,null);
      } else {
        ps.setDouble(12,event.location.latitude);
        ps.setDouble(13,event.location.longitude);
      }
      ps.setString(14,event.organizer);
      ps.setString(15,event.url);
      ps.setString(16,""+(event.virtual ? 1 : 0));
      ps.setString(17,event.status.toString());
      ps.setString(18,Instant.now().toString());
      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated == 1;

    } catch (Exception e) {
      LOG.error("an error occurred creating an event in the database");
      LOG.error("event : \n" + event.toString());
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }

    return true;
  }

  /**
   * 
   * @return : newly created events
   */
  public static ScannedEventsReport processFoundEvents(List<Event> events) {

    List<Event> created  = new  LinkedList<Event>();
    List<Event> updated  = new  LinkedList<Event>();
    List<Event> unchanged  = new  LinkedList<Event>();

    for ( Event e : events ) {

      Event dbe = getEventById(e.eventType,e.eventId);

      if ( dbe == null ) {

        if ( createEvent(e) ) {
          dbe = getEventById(e.eventType,e.eventId);
          created.add(dbe);
        } else {
          LOG.error("couldn't create a new event (type, eventId) : ( " + e.eventType.toString() + " , "  +  e.eventId + " )");
        }

      } else {

        // known events found in a scan can be updated
        EventMerger eventMerger = Providers.getMerger(dbe.eventType);
        if ( eventMerger == null ) {
            LOG.error("Unable to find merger for type : " + dbe.eventType);
            LOG.error("aborting processing for event id " + dbe.id);
            continue;
        }
        Event merge = eventMerger.merge(dbe,e);

        if ( merge != null ) {

          LOG.info("found an updated version of existing event " + merge.id);
          //update event in db
          updateEvent(merge);
          updated.add(merge);

        } else {

          //refresh last update and status
          LOG.info("found an existing event in scan " + dbe.id);
          updateEvent(dbe);  
          unchanged.add(dbe);

        }

      }

    }

    return new ScannedEventsReport(created,updated,unchanged);

  }

  /* check if the db exists, if not create a new db */
  public static void initDb() {
    File dbFile = new File(settings.dbFile());
    if (dbFile.exists()) {
      //todo : check if table are correct...
      //assume okay for now
      LOG.info("db file found : " + settings.dbFile());
      LOG.warn("skipping db integrity check");
    } else {
      LOG.info("no db file found, creating new event db");
      Connection con = getDbConnection();
      try {
        Statement stmt = con.createStatement();
        String sql = """
          CREATE TABLE EVENT(
            ID INTEGER PRIMARY KEY AUTOINCREMENT,
            EVENT_ID TEXT NOT NULL,
            SOURCE STRING NOT NULL,
            NAME TEXT NOT NULL,
            DESCRIPTION TEXT,
            START_TIME TEXT NOT NULL,
            END_TIME TEXT NOT NULL,
            LOCATION_NAME TEXT,
            COUNTRY TEXT,
            REGION TEXT,
            LOCALITY TEXT,
            STREET_ADDRESS TEXT,
            LONGITUDE REAL,
            LATITUDE REAL,
            ORGANIZER TEXT,
            URL TEXT,
            VIRTUAL INTEGER,
            LAST_UPDATE TEXT NOT NULL,
            STATUS TEXT NOT NULL
          );
        """;
        stmt.execute(sql);
      } catch (SQLException e) {
        LOG.error("error creating event database");
        LOG.error(e.getMessage());
      } finally {
        closeDbConnection(con);
      }

    }
  }

  private static String getDbConnectionString() {
    return "jdbc:sqlite:" + settings.dbFile();
  }

  private static Connection getDbConnection() {
    Connection con  = null;
    LOG.debug("connecting to " + getDbConnectionString());
    try {
      con = DriverManager.getConnection(getDbConnectionString());
      LOG.debug("connected to " + getDbConnectionString());
    } catch (SQLException e) {
      LOG.error("Critical error : unable to read events from database app.db");
      LOG.error(e.getMessage());
      System.exit(5);
    }
    return con;
  }

  private static void closeDbConnection(Connection con) {
    try {
      con.close();
    } catch (Exception e) {
      LOG.error("unable to close db connection");
      LOG.error(e.getMessage());
    }
  }

  private static Event getEventById(EventTypeEnum type, String eventId) {
    Connection con = getDbConnection();
    try {
      PreparedStatement ps = con.prepareStatement("SELECT * FROM EVENT WHERE EVENT_ID=? AND SOURCE=?;");
      ps.setString(1,eventId);
      ps.setString(2,type.toString());
      ResultSet rs = ps.executeQuery();
      if ( rs.next() ) {
        return eventRowToEvent(rs);
      }
    } catch (SQLException e) {
      LOG.error("An error occurred checking if an event exists { type, eventId } = { " + 
          type + " , " + eventId + " } ");
      LOG.error(e.getMessage());
    } finally {
      closeDbConnection(con);
    }
    return null;
  }

  // convert sqllite row to domain Event map
  private static Event eventRowToEvent(ResultSet rs) {

    EventBuilder eb = new EventBuilder();
    LocationBuilder lb = new LocationBuilder();

    try {
    
      String source = rs.getString("source");
      if ( !rs.wasNull() )  {
        try {
          eb.setEventTypeEnum(EventTypeEnum.valueOf(source));
        } catch (IllegalArgumentException iae) {
          LOG.error("mismatch between EventTypeEnum string in database and EventTypeEnum types");
          LOG.error("offending string : " + source);
          LOG.error(iae.getMessage());
        }
      }

      eb.setId(rs.getInt("id")); //NOT NULL
      eb.setEventId(rs.getString("event_id")); //NOT NULL

      String name = rs.getString("name");
      if ( !rs.wasNull() ) {
        eb.setName(name);
      }

      String description = rs.getString("description");
      if ( !rs.wasNull() ) {
        eb.setDescription(description);
      }

      String startTimeString = rs.getString("start_time");
      if ( !rs.wasNull() ) {
        Instant start  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTimeString));
        eb.setStart(start);
      }

      String endTimeString = rs.getString("end_time");
      if ( !rs.wasNull() ) {
        Instant end  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(endTimeString));
        eb.setEnd(end);
      }

      String locationName = rs.getString("location_name");
      if ( !rs.wasNull() ) {
        lb.setName(locationName);
      }

      String country = rs.getString("country");
      if ( !rs.wasNull() ) {
        lb.setCountry(country);
      }

      String region = rs.getString("region");
      if ( !rs.wasNull() ) {
        lb.setRegion(region);
      }

      String locality = rs.getString("locality");
      if ( !rs.wasNull() ) {
        lb.setLocality(locality);
      }

      Double longitude = rs.getDouble("longitude");                            
      Double latitude = rs.getDouble("latitude");                            
      if ( !rs.wasNull() ) { //assuming these are always coupled
        lb.setLatitude(latitude); 
        lb.setLongitude(longitude); 
      }

      String organizer = rs.getString("organizer");
      if ( !rs.wasNull() ) {
        eb.setOrganizer(organizer);
      }

      String url = rs.getString("url");
      if ( !rs.wasNull() ) {
        eb.setUrl(url);
      }

      int virtual = rs.getInt("virtual");
      eb.setVirtual(!rs.wasNull() && (virtual == 1));

      String lastUpdateTimeString = rs.getString("last_update");
      if ( !rs.wasNull() ) {
        Instant lastUpdate  = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(lastUpdateTimeString));
        eb.setLastUpdate(lastUpdate);
      }

      String status = rs.getString("status");
      if ( !rs.wasNull() )  {
        try {
          eb.setStatus(EventStatusEnum.valueOf(status));
        } catch (IllegalArgumentException iae) {
          LOG.error("mismatch between EventStatusEnum string in database and EventStatusEnum types");
          LOG.error("offending string : " + status);
          LOG.error(iae.getMessage());
        }
      }

      eb.setLocation(lb.build());

    } catch (SQLException se) {
      LOG.error("an error occured converting event row to Event object");
      LOG.error(se.getMessage());
    }

    return eb.build();
  }

}
