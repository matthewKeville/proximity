package keville.scanner;

import java.time.Duration;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.stereotype.Component;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.proxy.CaptureType;

@Component
public class DefaultProxyWebDriver implements ProxyWebDriver {

  private WebDriver webDriver;
  private BrowserMobProxy proxy;

  public DefaultProxyWebDriver() {

    this.proxy = new BrowserMobProxyServer();
    proxy.start(0); /* can concurrent instances use the same port? */

    Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
    seleniumProxy.setHttpProxy("localhost:"+proxy.getPort());
    seleniumProxy.setSslProxy("localhost:"+proxy.getPort());

    ChromeOptions options = new ChromeOptions();
    options.addArguments("headless");
    options.setCapability(CapabilityType.PROXY, seleniumProxy);
    options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);

    this.webDriver = new ChromeDriver(options);
    proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
    proxy.newHar("eventScanHar");

    webDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

  }

  public WebDriver getDriver() {
    return this.webDriver;
  }

  public BrowserMobProxy getProxy() {
    return this.proxy;
  }

  public void kill() {
    this.webDriver.quit();
    this.proxy.stop();
  }


}
