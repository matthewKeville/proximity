package keville.settings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import keville.compilers.EventCompiler;
import keville.event.Event;
import keville.scanner.ScanRoutine;

public record Settings(String dbFile,String eventbriteApiKey,int eventbriteMaxPages,int alleventsMaxPages,
    Map<String,ScanRoutine> scanRoutines, Map<String,Predicate<Event>> filters, List<EventCompiler> eventCompilers) {

  public String StringOf(Settings settings) {

    String result = "\ndbFile : " + settings.dbFile;

    result += "\neventbriteApiKey : " + settings.eventbriteApiKey;
    result += "\neventbriteMaxPages : " + settings.eventbriteMaxPages;
    result += "\nalleventsMaxPages : " + settings.alleventsMaxPages;

    result += "\n Scan Routines : " +  settings.scanRoutines.size() + "\n";

    Iterator<String> routineKeyIterator = settings.scanRoutines.keySet().iterator();
    while ( routineKeyIterator.hasNext() ) {
      ScanRoutine sr = scanRoutines.get(routineKeyIterator.next());
      result+= "\n"+sr.toString();
    }

    result += "\n Event Compilers : " +  settings.eventCompilers.size() + "\n";

    for ( EventCompiler ec : settings.eventCompilers ) {
      result+= "\n"+ec.toString();
    }

    result += "\n Custom Filters : " +  settings.filters.keySet().size() + "\n";
    Iterator<String> filterKeyIterator = settings.filters.keySet().iterator();
    while ( filterKeyIterator.hasNext() ) {
      result+= "\n"+filterKeyIterator.next();
    }

    result += "\n";

    return result;
  }


}
