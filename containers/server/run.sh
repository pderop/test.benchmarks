#!/usr/bin/env bash

[[ $# -ne 6 ]] && echo "Usage: $0 <projectid> <bucket name> <app name> <backend host> <backend port>" && exit 1

PROJECT_ID=$1
BUCKET=$2
APP=$3
PROTOCOL=$4
BACKEND_HOST=$5
BACKEND_PORT=$6

echo "Starting server for app: ${APP}, protocol: ${PROTOCOL}"

gsutil cp "gs://${BUCKET}/apps/${APP}.jar" .
java -version
java -DPROTOCOL=$PROTOCOL -DBACKEND_HOST=$BACKEND_HOST -DBACKEND_PORT=$BACKEND_PORT -jar ${APP}.jar

echo "Server exit ($?)"
exit 0

