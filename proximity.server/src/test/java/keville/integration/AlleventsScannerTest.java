package keville.integration;

import keville.providers.AllEvents.AllEventsScanner;
import keville.scanner.ScanReport;
import keville.settings.Settings;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AlleventsScannerTest {

    private static Logger LOG = LoggerFactory.getLogger(AlleventsScannerTest.class);
    private AllEventsScanner scanner;

    public AlleventsScannerTest(@Autowired Settings settings) {
      this.scanner = new AllEventsScanner(settings);
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
