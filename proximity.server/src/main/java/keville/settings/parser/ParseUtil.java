package keville.settings.parser;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//Convenience methods for parsing Settings.json with gson
public class ParseUtil {

  public static String tryParseNonEmptyString(JsonObject json,String field,String onFailMsg) throws SettingsParserException {
    try {
      String value  = json.get(field).getAsString();
      if ( StringUtils.isEmpty(value) ) {
        throw new SettingsParserException(onFailMsg);
      }
      return value;
    } catch ( UnsupportedOperationException | IllegalStateException ex ) {
      throw new SettingsParserException(onFailMsg);
    }
  }

  public static int tryParseInt(JsonObject json,String field,String onFailMsg) throws SettingsParserException {
    try {
      return json.get(field).getAsInt();
    } catch ( UnsupportedOperationException | NumberFormatException | IllegalStateException ex ) {
      throw new SettingsParserException(onFailMsg);
    }
  }

  public static double tryParseDouble(JsonObject json,String field,String onFailMsg) throws SettingsParserException {
    try {
      return json.get(field).getAsDouble();
    } catch ( UnsupportedOperationException | NumberFormatException | IllegalStateException ex ) {
      throw new SettingsParserException(onFailMsg);
    }
  }

  public static JsonArray tryParseArray(JsonObject json,String field,String onFailMsg) throws SettingsParserException {
    try {
      return json.get(field).getAsJsonArray();
    } catch ( IllegalStateException ex  ) {
      throw new SettingsParserException(onFailMsg);
    }
  }

  public static boolean tryParseBoolean(JsonObject json,String field,String onFailMsg) throws SettingsParserException {
    try {
      return json.get(field).getAsBoolean();
    } catch ( UnsupportedOperationException | IllegalStateException ex  ) {
      throw new SettingsParserException(onFailMsg);
    }
  }
}
