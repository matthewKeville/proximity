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

import java.time.LocalDateTime;

public class EventServiceTests
{


    private static Properties props;
    private static EventService eventService;
    private static Connection con;

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
        System.out.println(" test setup was unable to delete previous database file test.db");
      }

      //establish connection

      String connectionString = "jdbc:sqlite:test.db";
      props = new Properties();
      props.put("connection_string",connectionString);
      eventService = new EventService(props);

      try {
        con = DriverManager.getConnection(connectionString);
        System.out.println("connected to " + connectionString);
      } catch (SQLException e) {
        System.out.println(e.getMessage());
        System.exit(5);
      }

      try {

        //create tables

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
          "URL TEXT);";

        Statement stmt = con.createStatement();
        stmt.executeUpdate(sql0);

        //populate db with testing data

        String sql1 = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE)" +
        "VALUES ('aaaa', 'DEBUG','yoga','sunset yoga in bradley beach','2023-08-30T21:00:00Z','Bradley','NJ')";

        String sql2 = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE)" +
        "VALUES ('bbbb', 'DEBUG','free surfing lessons','beginnners surfing lessons provided by asbury community center','2023-08-30T21:00:00Z','Asbury Park','NJ')";

        String sql3 = "INSERT INTO EVENT (EVENT_ID,SOURCE,NAME,DESCRIPTION,START_TIME,CITY,STATE)" +
        "VALUES ('cccc', 'DEBUG','knitting','learn to knit like the best of them','2023-08-30T21:00:00Z','Belmar','NJ')";


        stmt = con.createStatement();
        stmt.executeUpdate(sql1);

        stmt = con.createStatement();
        stmt.executeUpdate(sql2);

        stmt = con.createStatement();
        stmt.executeUpdate(sql3);

      } catch (SQLException e) {
        System.out.println(e.getMessage());
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
      Event event = new Event(
          "test-event-12",
          EventTypeEnum.DEBUG,
          "createEventCreatesEvent",
          "a test for Junit",
          LocalDateTime.now(),
          0.0,
          0.0,
          "Trenton",
          "New Jersey",
          "https://google.com"
          );
      assertTrue(eventService.createEvent(event));


    }

}
