package location

import (
  "fmt"
)


type Location struct {
  Name string
  Country string
  Region string
  Locality string
  StreetAddress string
  Latitude float32
  Longitude float32
}

func (l Location) ToString() string {
  return fmt.Sprintf("Name : %s\tRegion : %s\tLocality : %s",l.Name,l.Region,l.Locality);
}
