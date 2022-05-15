#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
containerRuns=$(docker ps | grep -i "yacy_grid_mcp" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy_grid_mcp" | wc -l ) 

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
  echo "MCP container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy_grid_mcp
  echo "MCP container re-started"
else
  docker run -d --restart=unless-stopped -p ${bindhost}:8100:8100 --link yacy_grid_minio --link yacy_grid_rabbitmq --link yacy_grid_elasticsearch -e YACYGRID_GRID_S3_ADDRESS=admin:12345678@yacy_grid_minio:9000 -e YACYGRID_GRID_BROKER_ADDRESS=guest:guest@yacy_grid_rabbitmq:5672 -e YACYGRID_GRID_ELASTICSEARCH_ADDRESS=yacy_grid_elasticsearch:9300 --name yacy_grid_mcp yacy_grid_mcp
  echo "YaCy Grid MCP started."
fi
echo "To get the app status, open http://${callhost}:8100/yacy/grid/mcp/info/status.json"
