package keville;

import keville.Event;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.File;

import org.junit.Test;
import org.junit.BeforeClass;

import java.util.Properties;

import java.time.Instant;

public class EventServiceTests
{


    private static Properties props;
    private static EventService eventService;
    private static Connection con;
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventServiceTests.class);

    /*
     * Create external db connection (external to EventService)
     * Populate the test db with a set of dummy data.
     * Configure EventService instance to communicate with test.db
     */
    @BeforeClass
    public static void classSetup() {

      //clear db

      File dbFile = new File("test.db");
      if (!dbFile.delete()) {
        LOG.error(" test setup was unable to delete previous database file test.db");
      }

      //establish connection

      String connectionString = "jdbc:sqlite:test.db";
      props = new Properties();
      props.put("connection_string",connectionString);
      eventService = new EventService(props);

      try {
        con = DriverManager.getConnection(connectionString);
        LOG.info("connected to " + connectionString);
      } catch (SQLException e) {
        LOG.error(e.getMessage());
        System.exit(5);
      }

      try {

        //create tables

        /*
        String sql0 = "CREATE TABLE EVENT(" +
          "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
          "EVENT_ID TEXT NOT NULL," + 
          "SOURCE STRING NOT NULL," +
          "NAME TEXT NOT NULL," + 
          "DESCRIPTION TEXT," +
          "START_TIME TEXT NOT NULL," +
          "LONGITUDE REAL," +
          "LATITUDE REAL," +
          "CITY TEXT," + 
          "STATE TEXT," + 
          "URL TEXT," +
          "VIRTUAL INTEGER);";
        */

        String sql0 = "CREATE TABLE EVENT(" +
          "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
          "EVENT_ID TEXT NOT NULL," + 
          "SOURCE STRING NOT NULL," +
          "NAME TEXT NOT NULL," + 
          "DESCRIPTION TEXT," +
          "START_TIME TEXT NOT NULL," +
          "LOCATION_NAME TEXT," +
          "COUNTRY TEXT," +
          "REGION TEXT," +
          "LOCALITY TEXT," +
          "STREET_ADDRESS TEXT," +
          "LONGITUDE REAL," +
          "LATITUDE REAL," +
          "ORGANIZER TEXT," +
          "URL TEXT," +
          "VIRTUAL INTEGER);";

        Statement stmt = con.createStatement();
        stmt.executeUpdate(sql0);

        //populate db with testing data
    

        String sql1 = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,REGION,LOCALITY)" +
        "VALUES ('aaaa', 'DEBUG','yoga','sunset yoga in bradley beach','2023-08-30T21:00:00Z','NJ','Bradley')";

        String sql2 = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,REGION,LOCALITY)" +
        "VALUES ('bbbb', 'DEBUG','free surfing lessons','beginners surfing lessons provided by asbury community center','2023-08-30T21:00:00Z','NJ','Bradley')";

        stmt = con.createStatement();
        stmt.executeUpdate(sql1);

        stmt = con.createStatement();
        stmt.executeUpdate(sql2);

      } catch (SQLException e) {
        LOG.error("error creating testing data in sqlite database");
        LOG.error(e.getMessage());
      }

    }

    @Test
    public void getEventReturnsKnownEvent()
    {
      Event event = eventService.getEvent(1);
      assertNotNull(event);
      assertEquals(event.id, 1);
    }

    @Test
    public void getEventReturnsNullWhenUnknown()
    {
      Event event = eventService.getEvent(1000);
        assertNull(event);
    }

    @Test
    public void extantEventExists() {
      assertTrue(eventService.exists(EventTypeEnum.DEBUG,"aaaa"));
    }

    @Test
    public void nonExtantEventDoesNotExist() {
      assertFalse(eventService.exists(EventTypeEnum.DEBUG,"abcd"));
    }

    @Test
    public void nonExtantEventDoesNotExistTwo() {
      assertFalse(eventService.exists(EventTypeEnum.MEETUP,"aaaa"));
    }

    @Test
    public void createEventCreatesEvent() {

      EventBuilder eb = new EventBuilder();
      LocationBuilder lb = new LocationBuilder();

      eb.setEventId("test-event-12");
      eb.setEventTypeEnum(EventTypeEnum.DEBUG);
      eb.setName("createEventCreatesEvent");
      eb.setDescription("a test for junit");
      eb.setStart(Instant.now());

      lb.setLatitude(0.0);
      lb.setLongitude(0.0);
      lb.setLocality("Trenton");
      lb.setRegion("New Jersey");

      eb.setUrl("https://google.com");
      eb.setVirtual(false);
      eb.setLocation(lb.build());
      Event event = eb.build();

      assertTrue(eventService.createEvent(event));

    }

}
