package keville.compilers;

import keville.event.Event;

import java.util.function.Predicate;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZoneOffset;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Geo;

public class ICalCompiler extends EventCompiler {

  static Logger LOG = LoggerFactory.getLogger(ICalCompiler.class);

  public ICalCompiler(String name,Predicate<Event> filter, File file) {
    super(name,filter,file);
  }


  public void compile(List<Event> events) {

    ICalendar ical = new ICalendar();        

    events = events.stream().filter(filter).collect(Collectors.toList());

    for ( Event ev : events ) {

      VEvent event = new VEvent();

      event.setSummary(ev.name);
      event.setDescription(ev.description);
      event.setOrganizer(ev.organizer);
      event.setUrl(ev.url);
      event.setGeo(new Geo(ev.location.latitude,ev.location.longitude));
      Date startDate = java.util.Date.from(ev.start.toInstant(ZoneOffset.UTC));
      event.setDateStart(startDate);
      ical.addEvent(event);

    }

    try {

      Biweekly.write(ical).go(file);

    } catch (Exception e) {

      LOG.error("unable to create calendar file");

    }


  }

}
