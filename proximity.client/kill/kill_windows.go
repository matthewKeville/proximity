package kill

import (
  "os/exec"
  "fmt"
)

func KillByPid(pid int) error {

  cmd := exec.Command("taskkill", "/F", "/T", "/PID", fmt.Sprintf("%d",pid))
  return cmd.Run()

}
