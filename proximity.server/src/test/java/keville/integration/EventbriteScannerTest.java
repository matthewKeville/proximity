package keville.integration;

import keville.providers.Eventbrite.EventbriteScanner;
import keville.scanner.ScanReport;
import keville.settings.Settings;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EventbriteScannerTest
{

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventbriteScannerTest.class);
    private EventbriteScanner scanner;

    public EventbriteScannerTest(@Autowired Settings settings,
        @Autowired EventbriteScanner eventbriteScanner) {
      this.scanner = eventbriteScanner;
    }

    @Test
    public void scan() throws Exception {
      double latitude = 40.1784;
      double longitude = -74.0218;
      double radius = 6;
      ScanReport report = scanner.scan(latitude,longitude,radius);
      LOG.info(report.toString());
    }
}
