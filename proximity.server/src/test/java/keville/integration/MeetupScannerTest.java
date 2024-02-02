package keville.integration;

import keville.providers.meetup.MeetupScanner;
import keville.scanner.ScanReport;
import keville.settings.Settings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MeetupScannerTest
{

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MeetupScannerTest.class);
    private MeetupScanner scanner;

    public MeetupScannerTest(
        @Autowired Settings settings,
        @Autowired MeetupScanner meetupScanner) {
      this.scanner = meetupScanner;
    }

    @Test
    public void scan() throws Exception {
      double latitude = 40.1784;
      double longitude = -74.0218;
      //double radius = 6;
      double radius = 14;
      ScanReport report = scanner.scan(latitude,longitude,radius);
      LOG.info(report.toString());
    }
}
