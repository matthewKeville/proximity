package keville.settings.parser;

import keville.event.Event;
import keville.compilers.EventCompiler;
import keville.compilers.RSSCompiler;
import keville.compilers.ICalCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//Parse the settings.json compilers field into EventCompilers
public class CompilerParser {

  static Logger LOG = LoggerFactory.getLogger(CompilerParser.class);

  public static List<EventCompiler> parseEventCompilers(JsonArray compilers) throws SettingsParserException {

    List<EventCompiler> eventCompilers = new ArrayList<EventCompiler>();

    for ( JsonElement compiler : compilers ) {
      if ( !compiler.isJsonObject() ) {
        throw new SettingsParserException("Invalid compiler : compiler is supposed to be an object");
      }
      eventCompilers.add(parseEventCompiler(compiler.getAsJsonObject()));
    }

    return eventCompilers;

  }

  /*
    {
        type : "rss" | "ical",
        path : "/path/to/the/result",
        conjunction : true,
        filters : [{}]
    }
    */
  public static EventCompiler parseEventCompiler(JsonObject compilerJson) throws SettingsParserException {

      ///////////////////////////
      //name

      if ( !compilerJson.has("name") ) {
        throw new SettingsParserException("Invalid compiler : name is a required field");
      }
      String name = ParseUtil.tryParseNonEmptyString(compilerJson,"name","Invalid compiler : name expects a string");

      ///////////////////////////
      //path
      
      if ( !compilerJson.has("path") ) {
        throw new SettingsParserException("Invalid compiler : path is a required field");
      }
      String pathString = ParseUtil.tryParseNonEmptyString(compilerJson,"path","Invalid compiler : path expects a string");

      try {
        Files.createDirectories(Paths.get(pathString).getParent()); //mkdir -p
      }  catch (IOException e ) {
        throw new SettingsParserException("unable to create directory path for compiler " + pathString);
      }
      File file = new File(pathString);

      ///////////////////////////
      //conjunction

      boolean conjunction = true;
      if ( compilerJson.has("conjunction") && 
          ParseUtil.tryParseBoolean(compilerJson,"conjunction","Invalid compiler : conjunction expects a boolean") ) {
        conjunction = true;
      }

      ///////////////////////////
      //filter chain

      if ( !compilerJson.has("filters") ) {
          throw new SettingsParserException("Invalid compiler : filters is a required field");
      }

      JsonArray jsonFilterChain = ParseUtil.tryParseArray(compilerJson,"filters","Invalid compiler compiler.filters expects an array");
      //TODO  fix Filters.parseFilterChain should throw , not return null
      Predicate<Event> filter = FilterParser.parseFilterChain(jsonFilterChain, conjunction);

      ///////////////////////////
      //type

      if ( !compilerJson.has("type") ) {
        throw new SettingsParserException("Invalid compiler : type is a required field");
      }
      String typeString = ParseUtil.tryParseNonEmptyString(compilerJson,"type","Invalid compiler : type expects string");

      ///////////////////////////
      //build

      if (typeString.equals("rss")) {

        return new RSSCompiler(name,filter,file);

      } else if (typeString.equals("ical")) {

        return new ICalCompiler(name,filter,file);

      } else {

        throw new SettingsParserException("Invalid compiler : type " + typeString + " is invalid ");
        
      }

  }

}
