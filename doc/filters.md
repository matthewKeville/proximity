### disk

the  **disk** filter returns events within a  geographical disk.

> **radius**    : the disk radius in miles
<br>
> **latitude**  : the latitudinal center of the disk 
<br>
> **longitude** : the longitudinal center of the disk

```json
{
    "type" : "disk",
    "radius" : 3.0,
    "latitude" : 40.1784,
    "longitude" : -74.0218
}
```
***Events within 3.0  miles of  Philadelphia.***

---

### inPerson

the **inPerson** filter returns in-person only events.

> **invert**    : if true, return only  online events

```json
{
    "type" : "inPerson",
    "invert" : true
}
```

***Events in person***

---
### weekdays

the weekdays filter returns all events that are on an allowed weekday 

> **days**  : a list of allowed days
<br>
> **invert** : if true, exclude matches


```json
{
    "type" : "weekdays",
    "days" : ["friday","saturday","sunday"],
    "invert" : true
}
```

***Events on Friday,Saturday or Sunday***

---

### keywords

the keywords filter returns all events that contain a keyword in the
description or name.

> **keys**  : a list of allowed words
<br>
> **caseInsensitive** : if false, the words must be an exact match
<br>
> **invert** : if true, exclude matches

```json
{
    "type" : "keywords",
    "keys" : ["night","club","adult","adults"],
    "caseInsensitive" : true,
    "invert" : false
}
```
***Events that have a keyword "night", "club", "adult", or "adults" in the name or description***

---

### daysAwayRange

the **daysAwayRange** is a local temporal filter that returns events
that fall within a given temporal distance.

> **minDays**  : the minimum number of days before the event
<br>
> **maxDays** : the maximum number of days before the event

```json
{
    "type" : "daysAwayRange",
    "minDays" : 0,
    "maxDays" : 7
}
    
```
***Events that are less than or equal to a week away***

---

### custom

the **custom** filter is used to reference filters defined by the user in the `settings.json`

> **name** : the unique name of the user-defined filter

```json
{
    "type" : "custom",
    "name" : "myCustomFilterName"
}
    
```
***Events that satisfy the custom filter myCustomFilterName***


