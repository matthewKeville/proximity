package main

import (
  "proximity-client/grid"
  "proximity-client/http"
  "proximity-client/kill"
  "runtime"
  "os"
  "os/exec"
  "io/ioutil"
  "strings"
  "log"
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

  log.Printf("starting proximal server")

  ///////////////////////////
  // start the server
  ///////////////////////////

  cwd, err := os.Getwd()
  if err != nil {
    log.Fatalf("encountered an error finding cwd %s", err)
    os.Exit(1)
  }

  jarPath := cwd+"/proximity.jar"
  cmd := exec.Command("java","-jar",jarPath)

  //cmd.Stdout = os.Stdout
  //forward jvm stderr stream to this stderr
  //so client can see config failure
  cmd.Stderr = os.Stderr

  log.Printf("starting the server ")
  log.Printf("arguments : %s",cmd.Args)
  log.Printf("cmd : %s",cmd)

  err2 := cmd.Start()

  if err2 != nil {
    log.Fatalf("encountered an error starting server %s", err2)
  }

  log.Printf("daemon started... %d",cmd.Process.Pid)
 
  ///////////////////////////
  //write pid to file
  ///////////////////////////

  pid := cmd.Process.Pid

  file, err3 :=  os.Create("pid")
  if err3 != nil {
    log.Fatalf("failed to create pid file %d : %s",pid,err3)
  }
  
  _, err4  := file.WriteString(fmt.Sprintf("%d",pid))
  if err4 != nil {
    log.Fatalf("failed writing  pid to file %d : %s",pid,err4)
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
      log.Fatalf("unable to check pidFile existance : %s",err)
    }
  }

  ///////////////////////////
  // read pid file
  ///////////////////////////

  data, err  := ioutil.ReadFile(pidFile)
  if  err != nil {
    log.Fatalf("unable to read pidFile : %s",  err)
  }

  ///////////////////////////
  // parse pid
  ///////////////////////////

  pidString := string(data)
  pidString = strings.TrimRight(pidString, "\n")
  pid, err := strconv.Atoi(pidString)

  if err != nil {
    log.Fatalf("unable to parse pidFile : %s", err)
  }

  ///////////////////////////
  // process still running? 
  ///////////////////////////

  process, err := os.FindProcess(pid)

  if ( runtime.GOOS == "windows" ) {
    if ( err == nil ) {
      return true, pid
    }
    return false, -1
  }

  //https://pkg.go.dev/os#FindProcess
  // docs outline why special consideration is need under *nix

  err = process.Signal(syscall.Signal(0)) //send null signal
  if ( err == nil ) {
    return true, pid
  } 
  return false, -1

}

func main() {

  //https://stackoverflow.com/questions/19965795/how-to-write-log-to-file
  defaultLogger := log.Default()
  err := os.MkdirAll("logs", os.ModePerm)
  if err != nil {
    log.Fatalf("unable to create logs directory")
  }
  fh, err := os.OpenFile("logs/prxy.log", os.O_RDWR | os.O_CREATE | os.O_APPEND, 0666)
  if err != nil {
    log.Fatalf("error configuring log file")
  }
  defer fh.Close()
  defaultLogger.SetOutput(fh)
  log.Printf("proximity client started...")

  serverUp, serverPid := serverRunning()

  // flags

  daemonPtr := flag.Bool("daemon", false, "start proximity daemon")
  killPtr := flag.Bool("kill", false, "kill proximity daemon")
  restartPtr := flag.Bool("restart", false, "restart proximity daemon")

  jsonPtr := flag.Bool("json", false, "return a json response standard out")
  statusPtr := flag.Bool("status", false, "print proximity server status report") // not implemented
  listRoutinePtr := flag.Bool("list-routine", false, "print routines running on the server")
  listCompilerPtr := flag.Bool("list-compiler", false, "print compilers running on the server")
  listFilterPtr := flag.Bool("list-filter", false, "print filters known by the server")

  routinePtr := flag.String("routine","","use a routine's geography")
  filterPtr := flag.String("filter","","use a filter")
  radiusPtr := flag.Float64("radius",0.0,"event radius")
  latitudePtr := flag.Float64("latitude",0.0,"search latitude")
  longitudePtr := flag.Float64("longitude",0.0,"event search longitude")
  showVirtualPtr := flag.Bool("virtual", false,"show virtual events")
  daysBeforePtr := flag.Int("days", 0,"how many days out")


  flag.Parse()
  log.Printf("parsing flags")


  if  *killPtr {

    if !serverUp {
      fmt.Println("no daemon to kill")
      os.Exit(0)
    } 

    fmt.Println("killing daemon")
    log.Println("killing daemon")


    err := kill.KillByPid(serverPid)
    if ( err != nil ) {
      fmt.Println("unable to kill daemon")
      log.Panicf("unable to kill daemon : %s", err)
    }


    os.Exit(0)
    
  }


  if *daemonPtr {

    if serverUp {
      fmt.Println("daemon is already started")
    } else {
      startServer()
    }

    os.Exit(0)

  }

  if *restartPtr {

    log.Println("killing daemon")
    startServer()
    log.Println("starting daemon")
    fmt.Println("daemon restarted")
    os.Exit(0)

  }


  if ( !serverUp ) {
    fmt.Println("proximity server has not been started")
    fmt.Println("please execute prxy --daemon  to start the server")
    log.Println("tried to execute command but server is not started")
    os.Exit(0)
  }


  if  *statusPtr {
    status := http.GetStatus()
    fmt.Println(status)
    os.Exit(0)
  }

  if *listRoutinePtr {
    routines := http.GetRoutines()
    fmt.Println(routines)
    os.Exit(0)
  }

  if *listCompilerPtr {
    compilers := http.GetCompilers()
    fmt.Println(compilers)
    os.Exit(0)
  }

  if *listFilterPtr {
    filters := http.GetFilters()
    fmt.Println(filters)
    os.Exit(0)
  }



  if ( *jsonPtr ) {
    es := http.GetEventsAsJson(*latitudePtr,*longitudePtr,*radiusPtr,*showVirtualPtr,*daysBeforePtr,*routinePtr,*filterPtr)
    fmt.Println(es)
    os.Exit(0)
  }

  // launch table view
  p := tea.NewProgram(grid.InitialModel(*latitudePtr,*longitudePtr,*radiusPtr,*showVirtualPtr,*daysBeforePtr,*routinePtr,*filterPtr), tea.WithAltScreen())
  if _, err := p.Run(); err != nil {
    fmt.Println("An internal error occurred")
    log.Fatalf("An error occurred, error : %s",err)
  }

}

