package keville.settings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import keville.compilers.EventCompiler;
import keville.event.Event;

public record Settings(
    String dbFile,
    String eventbriteApiKey,
    int eventbriteMaxPages,
    Map<String,ScanRoutine> scanRoutines,
    Map<String,Predicate<Event>> filters,
    List<EventCompiler> eventCompilers) 
{

  public String toString(Settings settings) {

    String result = "\ndbFile : " + settings.dbFile;

    result += "\neventbriteApiKey : " + settings.eventbriteApiKey;
    result += "\neventbriteMaxPages : " + settings.eventbriteMaxPages;

    result += "\nScan Routines : " +  settings.scanRoutines.size() + "\n";

    Iterator<String> routineKeyIterator = settings.scanRoutines.keySet().iterator();
    while ( routineKeyIterator.hasNext() ) {
      ScanRoutine sr = scanRoutines.get(routineKeyIterator.next());
      result+= "\n"+sr.toString();
    }

    result += "\nEvent Compilers : " +  settings.eventCompilers.size() + "\n";

    for ( EventCompiler ec : settings.eventCompilers ) {
      result+= "\n"+ec.toString();
    }

    result += "\nCustom Filters : " +  settings.filters.keySet().size() + "\n";
    Iterator<String> filterKeyIterator = settings.filters.keySet().iterator();
    while ( filterKeyIterator.hasNext() ) {
      result+= "\n"+filterKeyIterator.next();
    }

    result += "\n";

    return result;
  }


}
