package keville.providers;

import keville.scanner.EventScanner;
import keville.updater.EventUpdater;
import keville.merger.EventMerger;

public class Provider {

    public EventScanner scanner;
    public EventUpdater updater;
    public EventMerger  merger;

    public Provider(
        EventScanner scanner, 
        EventUpdater updater, 
        EventMerger merger
    ) {
        this.scanner = scanner;
        this.updater = updater;     
        this.merger = merger;     
    }


}


