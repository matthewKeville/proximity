package keville;

import keville.util.JSONUtils;
import keville.AllEvents.AllEventsHarUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.BeforeClass;

import java.util.Properties;
import java.util.List;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AllEventsScannerTests
{

    private static Properties props;
    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllEventsScannerTests.class);
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

      String errMsg = " you must supply a path 'allevents_har_sample' in 'testing.properties' to a testing HAR file ";
      assertNotNull(errMsg,props.getProperty("allevents_har_sample"));
      assertFalse(errMsg,props.getProperty("allevents_har_sample").isEmpty());

      Path harPath = Path.of(props.getProperty("allevents_har_sample"));
      LOG.info("using HAR sample : "+props.getProperty("allevents_har_sample"));

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
      String  eventJsonPayload = AllEventsHarUtil.extractEventStubsJson(harString,"");
      assertTrue(JSONUtils.isValidJson(eventJsonPayload));
    }




}
