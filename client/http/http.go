package http

import (
  "fmt"
  "log"
  "net/http"
  "net/url"
  "io/ioutil"
  "encoding/json"
  "proximity-client/event"
)

func GetStatus() string {

  requestString := "http://localhost:4567/status"
  log.Printf("requesting : %s", requestString)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Panicf("error getting :  %s ",err)
    return "Server Unreachable"
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Panicf("can't extract body %s", err)
  }

  return string(body)

}

func GetRoutines() string {

  requestString := "http://localhost:4567/routine"
  log.Printf("requesting : %s", requestString)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Panicf("error getting :  %s ",err)
    return "Server Unreachable"
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Panicf("can't extract body %s", err)
  }

  return string(body)

}

func GetCompilers() string {

  requestString := "http://localhost:4567/compiler"
  log.Printf("requesting : %s", requestString)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Panicf("error getting :  %s ",err)
    return "Server Unreachable"
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Panicf("can't extract body %s", err)
  }

  return string(body)

}

func GetViews() string {

  requestString := "http://localhost:4567/view"
  log.Printf("requesting : %s", requestString)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Panicf("error getting :  %s ",err)
    return "Server Unreachable"
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Panicf("can't extract body %s", err)
  }

  return string(body)

}



func GetEventsRaw(latitude float64,longitude float64, radius float64,showVirtual bool,daysBefore int,routine string,view string) []byte {

  //params := fmt.Sprintf("?virtual=%t",showVirtual)
  v := url.Values{}

  if ( routine != "" ) {
    v.Add("routine", routine);
  } 

  if ( view != "" ) {
    v.Add("view", view);
  } 

  if latitude != 0.0 && longitude != 0.0  && radius != 0.0 {
    v.Add("latitude", fmt.Sprintf("%f",latitude));
    v.Add("longitude", fmt.Sprintf("%f",longitude));
  } 

  if ( radius != 0.0 ) {
    v.Add("radius", fmt.Sprintf("%f",radius));
  } 


  if ( daysBefore != 0 ) {
    v.Add("daysBefore",fmt.Sprintf("%d",daysBefore));
  } 

  requestString := fmt.Sprintf("http://localhost:4567/events?%s",v.Encode())
  log.Printf("requesting : %s",requestString)

  resp, err := http.Get(requestString)
  if err != nil {
    log.Panicf("error getting :  %s ",err)
    return []byte{}
  }

  body, err :=  ioutil.ReadAll(resp.Body)

  if err != nil {
    log.Panicf("can't extract body %s", err)
  }

  return body

}

func GetEvents(latitude float64,longitude float64, radius float64,showVirtual bool,daysBefore int,routine string,view string) []event.Event {

  body := GetEventsRaw(latitude,longitude,radius,showVirtual,daysBefore,routine,view);

  var e []event.Event

  err2 := json.Unmarshal(body,&e)
  if ( err2 != nil ) {
    log.Panicf("can't unmarshal %s", err2)
  }

  return e

}

func GetEventsAsJson(latitude float64,longitude float64, radius float64,showVirtual bool,daysBefore int,routine string,view string) string {

  body := GetEventsRaw(latitude,longitude,radius,showVirtual,daysBefore,routine,view);

  return string(body[:])

}
