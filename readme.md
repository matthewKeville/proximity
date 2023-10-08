# Proximity

**Proximity** is an event aggregation server that is capable of generating custom RSS feeds and calendars.

# Features

- Proximity can scrape event data from [eventbrite](https://eventbrite.com) , [meetup](https://meetup.com) and [allevents](https://allevents.in) based on user defined scanning routines.
- Proximity can maintain the accuracy of it's dataset by ensuring currency through it's update protocol.
- Proximity is capable of transforming scanned events into RSS feeds and iCalendars.
- Proximity features a filtering pipeline that can distill it's findings by removing unwanted data like promotional 
  or online only events.
- Proximity has a companion client **prxy** that can be used to interact with the server and it's event data on the command line
    using an interactive TUI or programmatically in the CLI modes.

## Installation ***WIP***

TODO

## Configuration

<br>

To use Proximity you must define a `settings.json`.

```json
./settings.json

{
  "eventbrite_api_key" : "<your eventbrite api key>",
  "eventbrite_max_pages": 10,
  "allevents_max_pages": 10,
  "scans" : [],
  "compilers" : []
}
```

The top level attributes of this json object specifies general rules about scraping providers. Throughout this document provider referes to an online event host like `meetup.com` The main meat of the configuration
lies within the  `scans` and `compilers` attributes.

<br>

## Scan Routines

<br>

Scan routines define what data to collect, where to collect it from, and how often. A routine requires 4 essential
parts a **name** , a **delay** , **providers** and **geographical circle** defined by a  **latitude**, **longitude** and **radius**.

Here is an example scan routine that scans for events in Philadelphia, PA every hour.

```json
{
  "name" : "Philly scan",
  "radius" : 5.0,
  "latitude" : 39.9526,
  "longitude" : -75.1652,
  "delay" : 3600,
  "meetup" : true,
  "allevents" : true,
  "eventbrite" : false
}
```
Note : this routine only scans **meetup** and **allevents** because **eventbrite** is explicitly
disabled. However, you can simply omit disabled providers in the configuration alltogether.


There are some scan routine attributes not on display above.

- `"auto" : true` Instead of specifying a geolocation you can ask the server to infer based on ip.
- `"disable" : true` you can disable an entire scan which may be useful for troubleshooting.
- `"run_on_restart" : true` By default **proximity will wait **delay** seconds to run any configured scan routines, but you can force a routine to run ASAP.

<br>

## Compilers  ***WIP***

Compilers are defined by 4 key attributes, **name**, **type**, **path**,
and **filters**.

This compiler creates an RSS feed on the filesystem at `~/feeds/philly-jawns.rss`.
It uses a disk filter exclude events outside of the geographical disk.

```json
{
  "name" : "Philly Jawns",
  "type" : "rss",
  "path" : "~/feeds/philly-jawns.rss",
  "conjuctive" : true,
  "filters" : [
    {
      "type" : "disk",
      "radius" : 3.0,
      "latitude" : 39.9526,
      "longitude" : -75.1652
    },
  ]
}
```

Note : the attribute **conjunctive** defines how to logically compose the filters.
With a value of false, the filters would be disjunctive, meaning an event need only 
be true for one of the defined filters for inclusion.

## Filters ***WIP***

Other filters include

```
{
  "type" : "virtual",
  "allowed" : false
}
```

## Usage

While you can invoke the server directly through `proximity.jar`. It is recommended you
use the companion client `prxy` to manage the **proxmity** instance.

### Manage Server


```sh
prxy --daemon # start the server
```

```sh
prxy --kill # kill the server
```

```sh
prxy --restart # equivalent to ./prxy --kill && ./prxy --daemon
```

### Configuration Check

```sh
prxy --list-routine # print loaded routines as json
```

```sh
prxy --list-compiler # print loaded compilers as json string
```

***These can be helpful when troubleshooting configuration issues.***

### Format Options


```sh
prxy # no format, loads an interactive table
```

```sh
prxy --json # print events as json
```

### Flags (Location)


```sh
prxy --radius <radius> --latitude <latitude> --longitude <longitude> 
```

or

```sh
prxy --routine <routine-name> # use the same location settings as named routine
```

***When no Location Filter is specified `prxy` will not filter events by location***

#### Flags (Content)

```sh
prxy --virtual <True/False> #allow online events in result set?
```





