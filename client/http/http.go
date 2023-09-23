package http

import (
  "fmt"
  "log"
  "net/http"
  "io/ioutil"
  "encoding/json"
  "proximity-client/event"
)


func GetEvents(latitude float64,longitude float64, radius float64,showVirtual bool,daysBefore int) []event.Event {

  params := fmt.Sprintf("?virtual=%t",showVirtual)

  if latitude != 0.0 && longitude != 0.0  && radius != 0.0 {
    params += fmt.Sprintf("&latitude=%f&longitude=%f",latitude,longitude)
  } 

  if ( radius != 0.0 ) {
    params += fmt.Sprintf("&radius=%f",radius)
  } 

  if ( daysBefore != 0 ) {
    params += fmt.Sprintf("&daysBefore=%d",daysBefore)
  } 

  requestString := fmt.Sprintf("http://localhost:4567/events%s",params)
  fmt.Printf("requesting : http://localhost:4567/events%s",params)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Printf("error getting :  %s ",err)
    log.Printf(err.Error())
    return []event.Event{}
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Fatalln("can't extract body")
    log.Fatalln(err)
  }

  var e []event.Event

  err2 := json.Unmarshal(body,&e)
  if ( err2 != nil ) {
    log.Fatalln("can't unmarshal")
    log.Fatalln(err2)
  }

  return e

}
