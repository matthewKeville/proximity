package keville.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JSONUtils {

    public static boolean isValidJson(String text) {
      try {
        JsonParser.parseString(text);
      } catch (JsonSyntaxException e) {
        return false;
      }
      return true;
    }

}
