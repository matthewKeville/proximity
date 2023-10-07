# Proximity

An event aggregator written in Java & Go

## Features

- Scrape event data from [eventbrite](https://eventbrite.com) , [meetup](https://meetup.com) and [allevents](https://allevents.in).
- Create custom RSS feeds and iCalendars.
- Explore events near you with a colorful TUI built with the excellent [charm](https://github.com/charmbracelet) toolchain and [bubble-table](https://github.com/Evertras/bubble-table) extension.
- Highly customizable and easily configurable settings written in JSON.

## ~~Installation~~

TODO

## Overview

Proximity runs a Java server to perform web scraping against the target sites. This was designed
as a utility that runs routinely, but infrequently. To scrape event data, you define
scan ***routines*** in a configuration file `settings.json`. The server will load these scan ***routines*** into memory on startup
and perform them at the requested frequency.

When the server finds events not in it's database it updates any configured ***compilers***.
<br><br>
***compilers*** in proximity are output mechanisms that transform events into a format that can be integrated
into other software. One of the main  motivations for this  project was a desire to have an **RSS** feed that would
inform me when events of interest where happening in my area. Currently proximity supporst two types of compilers
**RSS** and **iCalendar**.

Event data collected by proximity is stored in sqlite database, so it is easy to construct your own filters
or pipelines out of the routine scan data. Additionally this data can be accessed through the server by making calls
to the Web Api.

As the project evolved I found it unergonomic to interface with the data strictly through the sqlite3 client and felt limited by the command line capabilities of java so I created a companion client `prxy` written in go that communicates with the main java server through the Web API.

#### TLDR

A Java server performs scan **routines** and executes **compilers** that create custom event output formats.
<br>
<br>
`prxy` is a CLI to access the data found by the server.


## ~~Configuration~~

**Under construction**

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

## ~~scan routines~~

TODO

## ~~compilers~~

TODO

## Usage

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
prxy --json # print events as json string
```

### Filters (Location)


```sh
prxy --radius <radius> --latitude <latitude> --longitude <longitude> 
```

or

```sh
prxy --routine <routine-name> # use the same location settings as named routine
```

***When no Location Filter is specified `prxy` will not filter events by location***

#### Filters (Content)

```sh
prxy --virtual <True/False> #allow online events in result set?
```





