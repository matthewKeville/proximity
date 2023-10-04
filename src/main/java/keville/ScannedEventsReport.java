package keville;

import java.util.List;
import java.util.LinkedList;

public class ScannedEventsReport {

  public List<Event> created;
  public List<Event> updated;
  public List<Event> unchanged;

  public ScannedEventsReport(List<Event> created, List<Event> updated, List<Event> unchanged) 
  {
    this.created = created;
    this.updated = updated;
    this.unchanged = unchanged;
  }

  public List<Event> getAll() {
    List<Event> all = new LinkedList<Event>();
    all.addAll(created);
    all.addAll(updated);
    all.addAll(unchanged);
    return all;
  }

}
