package keville.settings.parser;

public class SettingsParserException extends Exception {
  private String json = "";

  SettingsParserException(String msg) {
    super(msg);
  }

  public String getJson() {
    return json;
  }
}
