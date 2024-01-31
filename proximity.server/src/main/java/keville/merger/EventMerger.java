package keville.merger;

import keville.event.Event;

public interface EventMerger {

    //compare two Event objects representing an object at different points
    //in time. Resolve differences, if no differences return null.
    public Event merge(Event oldEvent,Event newEvent);

}
