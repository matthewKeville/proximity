package main

import (
  "proximity-client/grid"
  //"proximity-client/http"
  //"proximity-client/event"
  "os"
  "os/exec"
  "io/ioutil"
  "strings"
  "syscall"
  "flag"
  "fmt"
  "errors"
  "strconv"
  "github.com/charmbracelet/bubbletea"
)

func startServer() int {

  ///////////////////////////
  //delete previous pid file
  ///////////////////////////
  os.Remove("./pid")

  fmt.Printf("starting proximal server")

  ///////////////////////////
  // start the server
  ///////////////////////////

  cwd, err := os.Getwd()
  if err != nil {
    fmt.Println("encountered an error finding cwd")
    fmt.Println(err)
    os.Exit(1)
  }

  jarPath := cwd+"/proximal-1.0-SNAPSHOT-jar-with-dependencies.jar"
  cmd := exec.Command("java","-jar",jarPath)
  fmt.Println(cmd.Args)
  err2 := cmd.Start()

  if err2 != nil {

    fmt.Println("encountered an error starting server")
    fmt.Println(err2)
    os.Exit(1)

  } 

  fmt.Printf("daemon started... %d",cmd.Process.Pid)
 
  ///////////////////////////
  //write pid to file
  ///////////////////////////

  pid := cmd.Process.Pid

  file, err3 :=  os.Create("pid")
  if err3 != nil {
    fmt.Printf("failed to create pid file %d",pid)
    os.Exit(2)
  }
  
  _, err4  := file.WriteString(fmt.Sprintf("%d",pid))
  if err4 != nil {
    fmt.Printf("failed writing  pid to file %d",pid)
    os.Exit(2)
  }

  return pid

}

func serverRunning() (bool, int)  {


  pidFile := "pid"

  ///////////////////////////
  //pid file exists ?
  ///////////////////////////

  _, err := os.Stat(pidFile)
  if err != nil {
    if errors.Is(err, os.ErrNotExist) {

      return false, -1

    } else {

      fmt.Printf("unable to check pidFile existance")
      os.Exit(1)

    }
  }

  ///////////////////////////
  // read pid file
  ///////////////////////////

  data, err  := ioutil.ReadFile(pidFile)
  if  err != nil {

    fmt.Printf("unable to read pidFile")
    os.Exit(1)

  }

  ///////////////////////////
  // parse pid
  ///////////////////////////

  pidString := string(data)
  pidString = strings.TrimRight(pidString, "\n")
  pid, err := strconv.Atoi(pidString)

  if err != nil {

    fmt.Println("unable to parse pidFile")
    fmt.Println(err)
    os.Exit(1)

  }

  ///////////////////////////
  // process still running? (see docs)
  ///////////////////////////

  process, err := os.FindProcess(pid)
  err = process.Signal(syscall.Signal(0))
  if ( err == nil ) {
    return true, pid
  } 
  return false, -1

}

func main() {

  serverUp, serverPid := serverRunning()

  // flags

  daemonPtr := flag.Bool("daemon", false, "start proximity daemon")
  killPtr := flag.Bool("kill", false, "kill proximity daemon")
  statusPtr := flag.Bool("status", false, "print proximity server status report")
  flag.Parse()


  if  *killPtr {

    if !serverUp {
      fmt.Printf("daemon is already dead")
      os.Exit(0)
    } 

    fmt.Printf("killing daemon")
    err := syscall.Kill(serverPid,syscall.SIGKILL)

    if ( err != nil ) {
      fmt.Printf("unable to kill daemon")
      fmt.Println(err)
    }

    os.Exit(0)
    
  }


  if *daemonPtr {

    if serverUp {
      fmt.Printf("daemon is already started")
    } else {
      startServer()
    }

    os.Exit(0)

  }

  if  *statusPtr {

    if !serverUp {
      fmt.Printf("daemon is not running")
      os.Exit(0)
    }

    fmt.Printf("placeholder status report")
    os.Exit(0)
    // here I would call an endpoint on the server /status

  }


  if ( !serverUp ) {
    fmt.Println("proximity server has not been started")
    fmt.Println("please execute prxy --daemon  to start the server")
    os.Exit(0)
  }

  // launch table view
  p := tea.NewProgram(grid.InitialModel())
  if _, err := p.Run(); err != nil {
    fmt.Printf("An error occurred, error : %v",err)
    os.Exit(1)
  }

}

