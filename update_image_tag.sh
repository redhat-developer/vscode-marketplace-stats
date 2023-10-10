#! /bin/bash
if [ -n "$1" ]
then
  TAG=$1
else
  TAG=$(git rev-parse --short=7 HEAD)
fi
# login to the OpenShift cluster before launching this script

# Tags the imagestream
oc tag docker.io/fbricon/vscode-marketplace-stats:$TAG vscode-marketplace-stats-app:latest