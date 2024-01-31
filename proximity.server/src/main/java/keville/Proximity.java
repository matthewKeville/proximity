package keville;

import keville.providers.Providers;
import keville.providers.Eventbrite.EventCache;
import keville.event.EventService;
import keville.compilers.EventCompilerScheduler;
import keville.updater.EventUpdaterScheduler;
import keville.scanner.EventScannerScheduler;
import keville.util.GeoUtils;
import keville.settings.Settings;
import keville.settings.SettingsParser;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class Proximity 
{
    static Logger LOG = LoggerFactory.getLogger(Proximity.class);
    static Thread scannerThread;
    static Thread updaterThread;
    static Thread compilerThread;

    static double DEFAULT_LAT; 
    static double DEFAULT_LON; 
    static final double DEFAULT_RAD = Double.MAX_VALUE;


    public static void main( String[] args )
    {
      SpringApplication.run(Proximity.class);
    }

    @Bean 
    Settings settings() {

      try {
        // read settings 
        InputStream inputStream = Proximity.class.getResourceAsStream("/settings.json");
        if ( inputStream == null ) {
          throw new BeanCreationException("settings.json is empty");
        }
        String settingsJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        Settings settings = SettingsParser.parseSettings(settingsJson);
        return settings;
      } catch (Exception e) {
        LOG.error(e.getMessage());
        throw new BeanCreationException("settings.json is unparsable");
      }

    }

    //TODO : Break this Runner into distinct runners for the respective background processes { Scanner , Updater , Compiler }
    @Bean
    CommandLineRunner startThreads(@Autowired Settings settings) {

      return args -> {

        try {

          // build dependencies

          Map<String,Double> coords = GeoUtils.getClientGeolocation();
          DEFAULT_LAT = coords.get("latitude"); 
          DEFAULT_LON = coords.get("longitude");

          LOG.info(settings.toString());

          // This is really bad, EventCache needs to be init after EventService
          // This structure is inflexible and incoherent..
          EventService.init(settings);
          EventCache.applySettings(settings);
          Providers.init(settings);

          // spawn threads

          EventScannerScheduler scannerScheduler = new EventScannerScheduler(settings);
          EventUpdaterScheduler updaterScheduler = new EventUpdaterScheduler(settings);
          EventCompilerScheduler compilerScheduler = new EventCompilerScheduler(settings);

          scannerThread = new Thread(scannerScheduler, "ScannerThread");
          updaterThread = new Thread(updaterScheduler, "UpdaterThread");
          compilerThread = new Thread(compilerScheduler, "CompilerThread");

          scannerThread.start();
          updaterThread.start();
          compilerThread.start();

        } catch (Exception e) {
          LOG.error("failed to init");
          LOG.error(e.getMessage());
        }

      };

    }
}
