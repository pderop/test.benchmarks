#!/bin/bash

# Script used to wait for a docker container to be started from a remote GCE VM

if [ $# -ne 3 ]; then
  echo "Usage: $0 <vm instance name> <zone> <image name>"
  exit 1
fi

INSTANCE_NAME=$1
ZONE=$2
IMAGE_NAME=$3
MAX_RETRIES=36

# Define the command to wait for the container to start
WAIT_COMMAND="while [ -z \"\$(docker ps -q --filter ancestor=$IMAGE_NAME)\" ]; do sleep 1; done"

# Retry the gcloud compute ssh command until it succeeds or reaches the maximum number of retries
retries=0
until gcloud compute ssh $INSTANCE_NAME --zone=$ZONE --command="$WAIT_COMMAND" || [ $retries -eq $MAX_RETRIES ]; do
    ((retries++))
    echo "Failed to SSH into instance, retrying in 10 seconds (attempt $retries/$MAX_RETRIES)..."
    sleep 5
done

# Check if the command succeeded or not
if [ $retries -eq $MAX_RETRIES ]; then
    echo "Failed to SSH into instance after $MAX_RETRIES attempts."
    exit 1
else
    echo "Container $IMAGE_NAME is running."
    exit 0
fi


