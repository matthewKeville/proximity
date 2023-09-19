package keville;

import keville.util.JSONUtils;
import keville.meetup.MeetupHarProcessor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.BeforeClass;

import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class MeetupScannerTests
{

    private static Properties props;
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupScannerTests.class);
    private static String harString;

    @BeforeClass
    public static void classSetup() {

      //load configuration w/ respect to testing.properties
      props = new Properties();
      try {
        File customProperties = new File("./testing.properties");
        if (customProperties.exists()) {
          LOG.info("testing properties found");
          props.load(new FileInputStream("./testing.properties"));
        } else {
          LOG.error("no testing.properties file found, unable to continue...");
          System.exit(2);
        }
      } catch (Exception e) {
        LOG.error("Unable to load testing.properties");
        LOG.error(e.getMessage());
        System.exit(1);
      }

      //minimal configuration met?
      if (props.getProperty("meetup_har_sample").isEmpty()) {
        LOG.info("You must provide a path to a meetup har sample to run this testing suite");
        LOG.error("meetup_har_sample is null");
        System.exit(2);
      }

      Path harPath = Path.of(props.getProperty("meetup_har_sample"));
      LOG.info("using HAR sample : "+props.getProperty("meetup_har_sample"));

      try {
       harString = Files.readString(harPath);
      } catch (Exception e) {
        LOG.error("error reading testing HAR data");
        LOG.error(e.getMessage());
      }
      assertNotNull(harString);

    }

    @Ignore
    public void extractEventsJsonReturnsValidJson()
    {
      String jsonString = MeetupHarProcessor.extractEventsJson(harString);
      boolean valid = JSONUtils.isValidJson(jsonString);
      assertTrue(JSONUtils.isValidJson(jsonString));
    }




}
