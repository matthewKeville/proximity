package keville.compilers;

import keville.Event;

import java.util.function.Predicate;
import java.util.List;
import java.util.Date;
import java.io.File;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Geo;

public class ICalCompiler extends EventCompiler {

  static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ICalCompiler.class);

  public ICalCompiler(Predicate<Event> filter, File file) {
    super(filter,file);
  }


  public void compile(List<Event> events) {

    ICalendar ical = new ICalendar();        

    for ( Event ev : events ) {


      VEvent event = new VEvent();

      event.setSummary(ev.name);
      event.setDescription(ev.description);
      event.setOrganizer(ev.organizer);
      event.setUrl(ev.url);
      event.setGeo(new Geo(ev.location.latitude,ev.location.longitude));
      event.setDateStart(Date.from(ev.start));

      ical.addEvent(event);

    }

    try {

      Biweekly.write(ical).go(file);

    } catch (Exception e) {

      LOG.error("unable to create calendar file");

    }


  }

}