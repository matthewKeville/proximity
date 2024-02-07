package kill

import (
  "syscall"
)

func KillByPid(pid int) error {

  return syscall.Kill(pid,syscall.SIGKILL)

}
