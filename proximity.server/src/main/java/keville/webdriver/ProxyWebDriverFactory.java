package keville.webdriver;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProxyWebDriverFactory {

  private static Logger LOG = LoggerFactory.getLogger(ProxyWebDriverFactory.class);

  @Autowired 
  private Environment env;

  public ProxyWebDriver getInstance() {

    if ( Arrays.asList(this.env.getActiveProfiles()).contains("dev") ) {
      LOG.info("dev profile detected, using DebugProxyWebDriver");
      return new DebugProxyWebDriver();
    }

    return new DefaultProxyWebDriver();

  }
}
