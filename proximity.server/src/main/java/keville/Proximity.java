package keville;

import keville.settings.Settings;
import keville.settings.parser.SettingsParser;
import keville.settings.parser.SettingsParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@EnableJdbcAuditing
@EnableJdbcRepositories
@SpringBootApplication
public class Proximity {
  static Logger LOG = LoggerFactory.getLogger(Proximity.class);

  public static void main(String[] args) {
    SpringApplication.run(Proximity.class);
  }

  @Bean
  TaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  @Bean
  Settings parseSettings() {

    // read settings from current directory (quit if bad cfg)

    File localSettings = new File("settings.json");
    if ( localSettings.exists() ) {

      LOG.info("found local settings..");

      try {

        String localSettingsString = Files.readString(localSettings.toPath());
        Settings settings = SettingsParser.parseSettings(localSettingsString);
        LOG.info(settings.toString());
        return settings;

      //broadcast startup errors to standard out AND logs
      } catch (Exception e) {

        if ( e instanceof SettingsParserException ) {

          LOG.error("Bad configuration in settings.json");
          LOG.error(e.getMessage());

          System.err.println("Bad configuration in settings.json");
          System.err.println(e.getMessage());

        } else {

          LOG.error("Unexpected error parsing settings.json");
          LOG.error(e.getClass().toString());
          LOG.error(e.getMessage());

          System.err.println("Unexpected error parsing settings.json");
          System.err.println(e.getMessage());
        }

        System.exit(1);


      }

    }

    // read default settings from classpath

    try {
      InputStream inputStream = Proximity.class.getResourceAsStream("/settings.json");
      if (inputStream == null) {
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

}
