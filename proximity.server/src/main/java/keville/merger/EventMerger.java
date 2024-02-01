package keville.merger;

import keville.event.Event;

/* An EventMerger resolves conflicting data between the same event in 2 points of time */
public interface EventMerger {

    //compare two Event objects representing this same event at different
    //times. Resolve differences, if no differences return null.
    public Event merge(Event oldEvent,Event newEvent);

}
