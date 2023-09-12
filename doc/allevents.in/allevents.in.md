# Locating events in the browser

navigating to this url `https://allevents.in/asbury%20park/all`

will display a grid of events for asbury park new jersey.

---

you scroll down until a `view more` button apears (once?)

`<a href="https://allevents.in/asbury%20park/all?page=2" id="show_more_events" class="mb20 btn-show-more"> View More </a>`

when there are no more events to load the style changes to `display: none;`

`<a href="https://allevents.in/asbury%20park/all?page=2" id="show_more_events" class="mb20 btn-show-more" style="display: none;"> View More </a>`

it has a very simple xpath `//*[@id="show_more_events"]`

---

## variation of initial search

this website uses ( i think ) a format for searching for events in your region

<town>-<ANSI-State-2L in lower case>

so for Jackson, NJ we would hit this url : `https://allevents.in/jackson-nj/all`

which gives the right results for Jackson in New Jersey as opposed to :  `https://allevents.in/jackson-ms/all`

## redirection

the intiail url you hit may not return an html file containing the data we are looking for.

Instead you may be redirected to another url that contains the data. I think this happens

whenever you hit a url with a state code. It will redirect to a page with no state code.

- I imagine it is doing this by way of headers in the request redirect.


# Data location

the event data is embedded into the webpage because this website adheres

to SCHEMA which is a data standard for browsers to extract meta data from web pages

in this case events.


## the data comes in 3 varieties

  - embedded into the target url (or redirect)
    - the HTML document for the requested page ex. `https://allevents.in/asbury%20park/all`
      contains a <script></script> with `application/ld+json` data in the format `JSON-LD`
  - an ajax request to the allevents api (for the requested region) : `allevents.in/api/index.php/categorization/web/v1/list` 
    - returns a response with `JSON-LD`
  - an ajax request to the allevents api (for the greater nearby region) : `../find_events-from-nearby-cities`
    - returns a response with `JSON-LD`
    - these are the results that show in a grid after you run out of content matching the initial query

## incomplete data ( event stubs )

  the data that can be gleaned from the above network requests is incomplete, it is a view
  into the full event json
  
    - it lacks the full DATETIME , it has startDate but not startTime.
    - it lacks a description, although the names on this site are descriptive
    - it contains a url to a webpage where the full event data exists

# Full Data Sample (INC)

navigating to the url of an event triggers a request ( or is embedded in the page , not sure atm )
that contains all the data we are interested in.


{"@context":"https://schema.org","@type":"Event","name":"Sea Hear Now Festival: The Killers, Foo Fighters, Greta Van Fleet &amp; Weezer - 2 Day Pass","image":"https://cdn-az.allevents.in/events5/banners/0c634da5adc89917293ac37a658ac28aeaf8f5884ef9d654e10b9206615431d0-rimg-w1000-h1000-gmir.png?v=1693998849","startDate":"2023-09-16T03:30:00-05:00","endDate":"2023-09-16T05:30:00-05:00","url":"https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097","eventStatus":"https://schema.org/EventScheduled","location":{"@type":"Place","name":"Asbury Festival Area","address":{"@type":"PostalAddress","name":"Asbury Festival Area, Asbury Park, United States","addressLocality":"Asbury Park","addressRegion":"NJ","addressCountry":"US"},"geo":{"@type":"GeoCoordinates","latitude":"40.223491","longitude":"-73.999216"}},"eventAttendanceMode":"https://schema.org/OfflineEventAttendanceMode","description":"New Jersey\u2019s surf-themed SEA.HEAR.NOW festival will return to North Beach Asbury Park and Bradley Park this September 16 and 17. Headlined by the Killers and Foo Fighters, the fest also features a surfing component in which teams captained by pro surfers Cam Richards and Sam Hammer will square off.","offers":[{"@type":"AggregateOffer","availability":"https://schema.org/InStock","priceCurrency":"USD","availabilityStarts":"2023-09-06","availabilityEnds":"2023-09-16T05:30:00-05:00","validFrom":"2023-09-06","lowPrice":"362","highPrice":"3696","price":"362","url":"https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097"},{"@type":"Offer","availability":"https://schema.org/InStock","priceCurrency":"USD","availabilityStarts":"2023-09-06","availabilityEnds":"","validFrom":"2023-09-06","url":"https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097","price":"362","name":"lowPrice"},{"@type":"Offer","availability":"https://schema.org/InStock","priceCurrency":"USD","availabilityStarts":"2023-09-06","availabilityEnds":"","validFrom":"2023-09-06","url":"https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097","price":"1114","name":"averagePrice"},{"@type":"Offer","availability":"https://schema.org/InStock","priceCurrency":"USD","availabilityStarts":"2023-09-06","availabilityEnds":"","validFrom":"2023-09-06","url":"https://allevents.in/asbury%20park/sea-hear-now-festival-the-killers-foo-fighters-greta-van-fleet-and-weezer-2-day-pass/230005595539097","price":"3696","name":"highPrice"}],"performer":[{"@type":"Person","name":"Sea Hear Now Festival"}]}







