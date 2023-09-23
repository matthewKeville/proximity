package keville.compilers;

import keville.Event;

import java.util.List;
import java.util.function.Predicate;
import java.io.File;


public abstract class EventCompiler {

  protected File file;
  protected Predicate<Event> filter = null;

  public EventCompiler(Predicate<Event> filter, File file) {
    this.file = file;
    this.filter = filter;
  }

  public abstract void compile(List<Event>  discoveries);

}
