package desktop

import (
  "os/exec"
  "log"
)

func OpenUrl(url string) {

  // I think "start" works on windows
  cmd := exec.Command("open",url) //open should be xplatform for MacOS

  cmd.Stderr = nil
  cmd.Stdin = nil

  log.Printf("opening %s via open command",url)

  err := cmd.Start()

  if err != nil {
    log.Fatalf("encountered an error opening the url %s", err)
  } 

}
