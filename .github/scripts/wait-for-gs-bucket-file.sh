#!/bin/bash

# wait for some files to be put in a google cloud storage bucket entry

if [ $# -ne 2 ]; then
  echo "Usage: $0 <bucket url path> <max-wait in minutes>"
  exit 1
fi

bucket=$1
found_total=0
# since we poll every 3 seconds, adjust the max-wait param (minutes)
max_wait=$(expr $2 \* 20)

for ((i = 1; i <= $max_wait; i++)); do
  if gsutil -q stat $bucket; then
    exit 0
  fi
  sleep 3
done

echo "Timeout while waiting for $bucket"
exit 1
