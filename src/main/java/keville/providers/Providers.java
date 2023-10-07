package keville.providers;

import keville.event.EventTypeEnum;
import keville.settings.Settings;

import keville.providers.AllEvents.AllEventsScanner;
import keville.providers.Eventbrite.EventbriteScanner;
import keville.providers.meetup.MeetupScanner;
import keville.merger.DefaultEventMerger;
import keville.updater.DefaultUpdater;

import keville.scanner.EventScanner;
import keville.merger.EventMerger;
import keville.updater.EventUpdater;

import java.util.Map;
import java.util.HashMap;

public class Providers {

    public static Map<EventTypeEnum,Provider> providers;

    public static void init(Settings settings) {

        EventMerger defaultMerger = new DefaultEventMerger();
        providers = new HashMap<EventTypeEnum,Provider>();
        
        Provider ae = new Provider(
                new AllEventsScanner(settings),
                new DefaultUpdater(),
                defaultMerger
        );

        Provider eb = new Provider(
                new EventbriteScanner(settings),
                new DefaultUpdater(),
                defaultMerger
        );

        Provider mu = new Provider(
                new MeetupScanner(settings),
                new DefaultUpdater(),
                defaultMerger
        );

        providers.put(EventTypeEnum.ALLEVENTS,ae);
        providers.put(EventTypeEnum.EVENTBRITE,eb);
        providers.put(EventTypeEnum.MEETUP,mu);
                                    

    }

    //@Precondition : Providers.init()
    public static EventScanner getScanner(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.scanner;
    }

    //@Precondition : Providers.init()
    public static EventUpdater getUpdater(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.updater;
    }

    //@Precondition : Providers.init()
    public static EventMerger getMerger(EventTypeEnum type) {
        Provider p = providers.get(type);
        return ( p == null ) ? null : p.merger;
    }

}


