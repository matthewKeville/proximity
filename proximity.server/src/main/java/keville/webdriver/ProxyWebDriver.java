package keville.webdriver;

import org.openqa.selenium.WebDriver;

import net.lightbody.bmp.BrowserMobProxy;

public interface ProxyWebDriver {
  WebDriver getDriver();
  BrowserMobProxy getProxy();
  void kill();
}
