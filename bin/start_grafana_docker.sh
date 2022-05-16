#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
containerRuns=$(docker ps | grep -i "yacy_grid_grafana" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy_grid_grafana" | wc -l ) 

usage() { echo "usage: $0 [-p | --production]" 1>&2; exit 1; }

args=$(getopt -q -o ph -l production,help -- "$@")
if [ $? != 0 ]; then usage; fi
set -- $args
while true; do
  case "$1" in
    -h | --help ) usage;;
    -p | --production ) bindhost="127.0.0.1"; callhost="localhost"; shift 1;;
    --) break;;
  esac
done

if [ ${containerRuns} -gt 0 ]; then
  echo "Grafana container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy_grid_grafana
  echo "Grafana container re-started"
else
  docker run -d --restart=unless-stopped --name yacy_grid_grafana -p ${bindhost}:3000:3000 --link yacy_grid_elasticsearch grafana/grafana-enterprise
  echo "Grafana started."
fi
echo "Open http://${callhost}:3000 and log in with admin:admin"
