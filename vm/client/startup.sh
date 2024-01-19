#!/usr/bin/env bash

set -ev

PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
INTERNAL_IP=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/INTERNAL_IP" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

sudo apt-get update
sudo apt-get install -yq openjdk-21-jdk
sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/jre/bin/java

