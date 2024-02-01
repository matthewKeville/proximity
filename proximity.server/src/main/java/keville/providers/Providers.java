package keville.providers;

import keville.event.EventTypeEnum;
import keville.settings.Settings;

import keville.providers.AllEvents.AllEventsScanner;
import keville.providers.Eventbrite.EventbriteScanner;
import keville.providers.meetup.MeetupScanner;

import keville.scanner.EventScanner;
import keville.merger.EventMerger;
import keville.updater.EventUpdater;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class Providers {

    public  Map<EventTypeEnum,Provider> providers;

    public Providers(
      @Autowired Settings  settings,
      @Autowired AllEventsScanner allEventsScanner,
      @Autowired EventbriteScanner eventbriteScanner,
      @Autowired MeetupScanner meetupScanner,
      @Autowired EventMerger eventMerger,
      @Autowired EventUpdater eventUpdater
        ) {

        providers = new HashMap<EventTypeEnum,Provider>();
        
        Provider ae = new Provider(
          allEventsScanner,
          eventUpdater,
          eventMerger
        );

        Provider eb = new Provider(
          eventbriteScanner,
          eventUpdater,
          eventMerger
        );

        Provider mu = new Provider(
          meetupScanner, 
          eventUpdater,
          eventMerger
        );

        providers.put(EventTypeEnum.ALLEVENTS,ae);
        providers.put(EventTypeEnum.EVENTBRITE,eb);
        providers.put(EventTypeEnum.MEETUP,mu);

    }

    public EventScanner getScanner(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.scanner;
    }

    public EventUpdater getUpdater(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.updater;
    }

    public EventMerger getMerger(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.merger;
    }

}


