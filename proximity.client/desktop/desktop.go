package desktop

import (
  "os/exec"
  "log"
  "runtime"
)

func OpenUrl(url string) {

  var cmd *exec.Cmd

  if ( runtime.GOOS == "windows" ) {
    cmd = exec.Command("cmd","/c","start",url)
  } else {
    cmd = exec.Command("open",url)
  }

  cmd.Stderr = nil
  cmd.Stdin = nil

  log.Printf("opening %s via open command",url)

  err := cmd.Start()

  if err != nil {
    log.Fatalf("encountered an error opening the url %s", err)
  } 

}
