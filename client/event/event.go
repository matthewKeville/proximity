package event

import (
  "fmt"
  "proximity-client/location"
  "time"
)


type Event struct {
  EventType string
  Id int
  EventId string
  Name string
  Description string
  Start time.Time
  //start
  Location location.Location
  Url string
  Organizer string
  Virtual bool
}

func (e Event) ToString() string {
  return fmt.Sprintf(
    "Type : %s\n" +
    "Id : %d\n" +
    "EventId : %s\n" +
    "Name : %s\n" +
    "Desc : %s\n" +
    "Time : %s\n" +
    "Region : %s\n" +
    "Locality : %s\n" +
    "Latitude : %f\n" +
    "Longitude : %f\n" +
    "Url : %s\n" +
    "Org  : %s\n" +
    "Virtual  : %t\n",
      e.EventType,
      e.Id,
      e.EventId,
      e.Name,
      e.Description,
      e.Start,
      e.Location.Region,
      e.Location.Locality,
      e.Location.Latitude,
      e.Location.Longitude,
      e.Url,
      e.Organizer,
      e.Virtual); 
}
