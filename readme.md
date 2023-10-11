# Proximity

**Proximity** is an event aggregation server that is capable of generating custom RSS feeds and calendars.

# Features

- Proximity can scrape event data from [eventbrite](https://eventbrite.com) , [meetup](https://meetup.com) and [allevents](https://allevents.in) based on user defined scanning routines.
- Proximity can maintain the accuracy of it's dataset by ensuring currency through it's update protocol.
- Proximity is capable of transforming scanned events into RSS feeds and iCalendars.
- Proximity features a filtering pipeline that enables users to create datasets with specificity.
- Proximity has a companion client **prxy** that can be used to interact with the server and it's event data on the command line
    using an interactive TUI or programmatically in the CLI modes.


## Configuration

<br>

To use Proximity you must define a `settings.json`.

```json
{
  "eventbrite_api_key" :"<your-eventbrite-api-key>",
  "eventbrite_max_pages": 5,
  "allevents_max_pages": 5,

  "routines" : [],
  "compilers" : [],
  "filters" : []

}
```
The `settings.json` can be divided into two parts. The first part is defining
policies about scraping data from providers. The second part declares what
proximity will do.

The what is determined by 3 lists : **routines**,**compilers**, and **filters**

- **routines**         : define how/where/when to scrape event data
- **compilers**        : package found events into another data format
- **filters**          : define custom event filters


## Routines

<br>

Routines define what data to collect, where to collect it from, and how often. A routine requires 4
components a **name** , a **delay** , **providers** and a **geographical circle** defined by a  **latitude**, **longitude** and **radius**.

Here is an example routine that scans for events in Philadelphia, PA every hour.

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

There are some routine attributes not on display above.

- `"auto" : true`  Instead of specifying a geolocation you can ask the server to infer based on ip.
- `"disable" : true` You can disable an entire scan which may be useful for troubleshooting.
- `"run_on_restart" : true` By default **proximity** will wait **delay** seconds to run any configured scan routines, but you can force a routine to run ASAP.


## Compilers 

Comilers produce export formats for found data like RSS or iCalendar. Compilers are defined by 5 key components, **name**, **type**, **path**, 
**conjunction**, and **filters**.

This compiler creates an RSS feed on the filesystem at `~/feeds/philly-jawns.rss`.
It uses a disk filter to exclude events outside of the geographical disk.

```json
{
  "name" : "Philly Jawns",
  "type" : "rss",
  "path" : "~/feeds/philly-jawns.rss",
  "conjunction" : true,
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

Note : the attribute **conjunction** defines how to logically compose the filters.
With a value of false, the filters would be disjunctive, meaning an event need only 
be true for one of the defined filters for inclusion.

## Filters

Proximity ships with a set or primitive filters that can be used as building blocks to
create more specific filters. In the **filters** section of your **settings.json** you define
custom filters that can be referenced in your compilers definitions or used to query the server with ( more on that below ).

For more information on primitive filters see [filters](docs/filters.md)


```json
"filters" : [
    {
      "name" : "nearPhillyInPersonOnTheWeekEnd",
      "conjunction" : true,
      "filters" : [
        {
          "type" : "disk",
          "radius" : 3.0,
          "latitude" : 39.9526,
          "longitude" : -75.1652,
        },
        {
          "type" : "inPerson",
          "invert" : false
        },
        {
          "type" : "weekdays",
          "days" : ["friday","saturday","sunday"],
          "invert" : false
        }
      ]
    }
]
```

This filter ( as the name would imply ) returns events that are near Philadelphia,
that are not online, that are on the weekend. Here we use 3 primitive filters
**disk** , **inPerson**, and **weekdays** in synthesis to create a custom filter. **inPerson** and **weekdays** 
both have a attribute called **invert**, when inverted the filter does the opposite of what
is expected. An inverted **inPerson** filter returns only online events. An inverted **weekdays**
filter returns events not with the listed days.

---

When we want to reference this filter in our **compilers** definition. We do it by
specifying a **custom** type.

```json
{
    "type" : "custom",
    "name" : "nearPhillyInPersonOnTheWeekEnd"
}
```


## Usage

**proximity** is distribuited as an executable JAR, while you can invoke the server directly through `proximity.jar`. It is recommended you
use the companion client `prxy` to manage the **proxmity** instance. 

When you have a valid `settings.json`, you launch **proximity** using `prxy` like below.

```sh
prxy --daemon
```

At this point, **proximity** will parse your configuration and perform scans and
compilations as defined. You can check that your configuration was valid
by issuing this command to the client.

```sh
prxy --status
```

If you want to stop or restart the server you can issue these commands.

```sh
prxy --kill
prxy --restart
```

## Viewing events

If you want to check event data interactively you have 2 views at your disposal.


**Json view**

```sh
prxy --json # recommended that you use jq , i.e. prxy --json | jq '.'
```
will ask **proximity** for all events in json format.

---

**table view**

```sh
prxy
```

---

***view flags***

You can pass flags to the client to create different views into the event data.

To restrict your view by geography you can use the following options.

```sh
prxy --radius <radius> --latitude <latitude> --longitude <longitude> 
```

or

```sh
prxy --routine <routine-name> # use the same location settings as named routine
```

***When using a routine, you can override routine parameters with the explicit
radius, longitude, and latitude flags.***

***When no Location Filter is specified `prxy` will not filter events by location***

---

**custom filters**

As mentioned earlier, we can use our custom filters to query data. That
is done using the `--filter` flags. To use the custom filter defined earlier.

```sh
prxy --filter nearPhillyInPersonOnTheWeekEnd
# or
prxy --json --filter nearPhillyInPersonOnTheWeekEnd
```
