package keville;

import keville.providers.Eventbrite.EventCache;
import keville.settings.Settings;
import keville.settings.SettingsParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

    // read settings from current directory
    File localSettings = new File("settings.json");
    if ( localSettings.exists() ) {
      LOG.info("found local settings..");
      try {
        String localSettingsString = Files.readString(localSettings.toPath());
        Settings settings = SettingsParser.parseSettings(localSettingsString);
        return settings;
      } catch (Exception e) {
        LOG.error(e.getMessage());
        throw new BeanCreationException("./settings.json is unparsable");
      }
    }

    // read settings from classpath
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

  @Bean
  CommandLineRunner startThreads(
      @Autowired Settings settings) {

    return args -> {

      try {
        LOG.info(settings.toString());
      } catch (Exception e) {
        LOG.error("failed to init");
        LOG.error(e.getMessage());
      }

    };

  }
}
