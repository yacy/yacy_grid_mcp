#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
containerRuns=$(docker ps | grep -i "yacy_grid_elastichq" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy_grid_elastichq" | wc -l ) 

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
  echo "ElasticHQ container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy_grid_elastichq
  echo "ElasticHQ container re-started"
else
  docker run -d  --restart=unless-stopped --name yacy_grid_elastichq --link yacy_grid_elasticsearch -p ${bindhost}:5000:5000 -e HQ_DEFAULT_URL=http://yacy_grid_elasticsearch:9200 elastichq/elasticsearch-hq
  echo "ElasticHQ container started."
fi
echo "Open http://${callhost}:5000"

