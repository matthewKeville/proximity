package keville.compilers;

import keville.event.Event;

import java.util.List;
import java.util.function.Predicate;
import java.io.File;

public abstract class EventCompiler {

  public String name;
  public File file;
  public Predicate<Event> filter = null;

  public EventCompiler(String name,Predicate<Event> filter, File file) {
    this.name = name;
    this.file = file;
    this.filter = filter;
  }

  public abstract void compile(List<Event>  discoveries);

  public String toString() {
    String result = "";
    result += "\n\t" + this.name;
    result += "\n\t" + this.file.getPath().toString();
    result += "\n\t" + this.getClass().getSimpleName();
    return result;
  }

}
