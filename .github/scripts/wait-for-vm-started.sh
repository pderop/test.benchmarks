#!/bin/bash

# Script used to wait for a GCE vm to be fully started

if [ $# -ne 3 ]; then
  echo "Usage: $0 <vm name> <zone> <max_wait_sec>"
  exit 1
fi

vm_name=$1
vm_zone=$2
max_wait_sec=$3
start_time=$(date +"%s")

echo "Wait for VM \"$vm_name\" to be fully started."

wait_for_log() {
  expected=$1
  error_message=$2

  while true; do
    sleep 3
    status=$(gcloud compute instances get-serial-port-output $vm_name --zone $vm_zone 2>&1 | grep "$expected")
    if [ $? -eq 0 ]; then
      return 0
    fi

    if [ "$error_message" != "" ]; then
      status=$(gcloud compute instances get-serial-port-output $vm_name --zone $vm_zone 2>&1 | egrep "$error_message")
      if [ $? -eq 0 ]; then
        echo $status
        return 1
      fi
    fi

    curr_time=$(date +"%s")
    diff_time=$(expr $curr_time - $start_time)
    if [ $diff_time -gt $max_wait_sec ]; then
      echo "Timeout"
      return 1
    fi
  done
}

if wait_for_log "Finished Google Compute Engine Startup Scripts"; then
  if wait_for_log "startup-script exit status 0" 'startup-script exit status [1-9]+'; then
    echo "vm $vm_name started."
    exit 0
  fi
fi

exit 1
