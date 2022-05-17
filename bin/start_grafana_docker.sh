#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`
appname=Grafana
containername=yacy-grid-grafana

usage() { echo "usage: $0 [-p | --production]" 1>&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p | --production ) bindhost="127.0.0.1"; callhost="localhost"; shift 1;;
    -h | --help | -* | --* | * ) usage;;
  esac
done

containerRuns=$(docker ps | grep -i "${containername}" | wc -l ) 
containerExists=$(docker ps -a | grep -i "${containername}" | wc -l ) 
if [ ${containerRuns} -gt 0 ]; then
  echo "${appname} container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start ${containername}
  echo "${appname} container re-started"
else
  docker run -d --restart=unless-stopped -p ${bindhost}:3000:3000 \
	 --link yacy-grid-elasticsearch \
         --name ${containername} grafana/grafana-enterprise
  echo "${appname} started."
fi
./dockerps.sh

echo "Open http://${callhost}:3000 and log in with admin:admin"
