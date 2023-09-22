package keville;

import keville.Event;
import keville.Settings;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.BeforeClass;


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

      String settingsFileString = "./custom.json";
      Path jobFilePath = FileSystems.getDefault().getPath(settingsFileString);

      try {
        String jsonString = new String(Files.readAllBytes(jobFilePath),StandardCharsets.UTF_8);
        Settings settings = Settings.parseSettings(jsonString);
        EventService.applySettings(settings);
      } catch (Exception e) {
        LOG.error("unable to parse settings : " + jobFilePath.toString());
        LOG.error(e.getMessage());
        System.exit(1);
      }


      //clear db

      File dbFile = new File("test.db");
      if (!dbFile.delete()) {
        LOG.error(" test setup was unable to delete previous database file test.db");
      }

      //establish connection

      String connectionString = "jdbc:sqlite:test.db";
      props = new Properties();
      props.put("connection_string",connectionString);

      try {

        con = DriverManager.getConnection(connectionString);
        LOG.info("connected to " + connectionString);

      } catch (SQLException e) {

        LOG.error(e.getMessage());
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

}
