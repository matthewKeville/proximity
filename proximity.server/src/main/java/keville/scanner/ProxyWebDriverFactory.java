package keville.scanner;

import org.springframework.stereotype.Component;

@Component
public class ProxyWebDriverFactory {
  public ProxyWebDriver getInstance() {
    return new DefaultProxyWebDriver();
  }
}
