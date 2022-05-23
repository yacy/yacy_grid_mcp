#!/bin/bash
cd "`dirname $0`"

bindhost="127.0.0.1"
callhost="localhost"
appname="YaCy Grid MCP"
containername=yacy-grid-mcp
imagename=${containername//-/_}
dockerfile="Dockerfile"
production=false
open=false

usage() { echo "usage: $0 [-o | --open | -p | --production | --arm32 | --arm64 ]" 1>&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p | --production ) production=true; shift 1;;
    -o | --open ) open=true; shift 1;;
    --arm32 ) imagename=${imagename}:arm32; dockerfile=${dockerfile}_arm32; shift 1;;
    --arm64 ) imagename=${imagename}:arm64; dockerfile=${dockerfile}_arm64; shift 1;;
    -h | --help | -* | --* | * ) usage;;
  esac
done
if [ "$production" = true ] ; then imagename="yacy/${imagename}"; fi
if [ "$open" = true ] ; then bindhost="0.0.0.0"; callhost=`hostname`; fi

containerRuns=$(docker ps | grep -i "${containername}" | wc -l ) 
containerExists=$(docker ps -a | grep -i "${containername}" | wc -l ) 
if [ ${containerRuns} -gt 0 ]; then
  echo "${appname} container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start ${containername}
  echo "${appname} container re-started"
else
  if [[ $imagename != "yacy/"*":latest" ]] && [[ "$(docker images -q ${imagename} 2> /dev/null)" == "" ]]; then
      cd ..
      docker build -t ${imagename} -f ${dockerfile} .
      cd bin
  fi
  docker run -d --restart=unless-stopped -p ${bindhost}:8100:8100 \
	 --link yacy-grid-minio --link yacy-grid-rabbitmq --link yacy-grid-elasticsearch \
	 -e YACYGRID_GRID_S3_ADDRESS=admin:12345678@yacygrid.yacy-grid-minio:9000 \
	 -e YACYGRID_GRID_BROKER_ADDRESS=guest:guest@yacy-grid-rabbitmq:5672 \
	 -e YACYGRID_GRID_ELASTICSEARCH_ADDRESS=yacy-grid-elasticsearch:9300 \
	 --name ${containername} ${imagename}
  echo "${appname} started."
fi
./dockerps.sh

echo "To get the app status, open http://${callhost}:8100/yacy/grid/mcp/info/status.json"
