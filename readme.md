# Proximity

An event aggregator written in Java & Go

## Features

- Scrape event data from [eventbrite](https://eventbrite.com) , [meetup](https://meetup.com) and [allevents](https://allevents.in).
- Create custom RSS feeds and iCalendars.
- Explore events near you with a colorful TUI built with the excellent [charm](https://github.com/charmbracelet) toolchain and [bubble-table](https://github.com/Evertras/bubble-table) extension.
- Highly customizable and easily configurable settings written in JSON.

## Installation

TODO

## Overview

Proximity runs a java server to perform web scraping against the target sites. This was designed
as a utility that runs routinely, but infrequently. To scrape event data, you define
`scans routines` in a configuration file `settings.json`. The server will load these `scans routines` into memory on startup
and perform them at the requested frequency.

When the server finds events not in it's database it updates any configured `compilers`.
<br><br>
`compilers` in proximity are output mechanisms that transform events into a format that can be integrated
into other software. One of the main  motivations for this  project was a desire to have an **RSS** feed that would
inform me when events of interest where happening in my area. Currently proximity supporst two types of compilers
**RSS** and **iCalendar**.

Event data collected by proximity is stored in sqlite database, so it is easy to construct your own filters
or pipelines out of the routine scan data. Additionally this data can be accessed through the server by making calls
to the Web Api.

As the project evolved I found it unergonomic to interface with the data strictly through the sqlite3 client and felt limited by the command line capabilities of java so I created a companion client `prxy` written in go that communicates with the main java server through the Web API.

When `prxy` requests data from the server it will tell the server
to infer the geolocation based on the current IP address. As a consequence if you setup a `scan routine` that is not
based on your current location, then `prxy` will return no events. If you would like to customize what data
is retrieved from the server you can  specify a geographical region with `--radius`  `--latitude` `--longitude`.
It is worth noting that this invocation `prxy --radius --latitude --longitude` does not perform or request a scan, it
accesses data already on  the server.

#### TLDR

Java server performs `scan routines` and creates `compilers` based on `settings.json`.
<br>
`prxy` is a CLI utility to access the data found by the Java server.


## Configuration
```
./settings.json

{
  "db_connection_string" :  "jdbc:sqlite:app.db",
  "eventbrite_db_connection_string" :  "jdbc:sqlite:eventbrite.db",
  "eventbrite_api_key" : <your eventbrite api key>,
  "eventbrite_max_pages": 10,
  "allevents_max_pages": 10,
  "run_on_restart" : false,
  "scans" : []
  "compilers" : []
}
```

## scan routines 

TODO

## compilers 

TODO

## Usage

start the server

```sh
prxy --daemon
```

check server status (useful to test settings.json)

```sh
prxy --status
```

view local events in an interactive table

```sh
prxy
```

print events as json on the command line

```sh
prxy --json
```

Filter events returned from the server based on geolocation
```sh
prxy --radius <radius> --latitude <latitude> --longitude <longitude> 
```
Allow online events in result set
```sh
prxy --virtual <virtual>
```





