package http

import (
  "log"
  "net/http"
  "io/ioutil"
  "encoding/json"
  "proximity-client/event"
)

func GetEvents() []event.Event {
  resp, err := http.Get("http://localhost:4567/events")
  if err != nil {
    log.Printf("error getting localhost:4567/events")
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
