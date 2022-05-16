#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
appname=MinIO
containername=yacy-grid-minio

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

containerRuns=$(docker ps | grep -i "${containername}" | wc -l ) 
containerExists=$(docker ps -a | grep -i "${containername}" | wc -l ) 
if [ ${containerRuns} -gt 0 ]; then
  echo "${appname} container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start ${containername}
  echo "${appname} container re-started"
else
    docker run -d --restart=unless-stopped -p ${bindhost}:9000:9000 -p ${bindhost}:9001:9001 \
           -e "MINIO_ROOT_USER=admin" \
           -e "MINIO_ROOT_PASSWORD=12345678" \
           -v ${containername}:/data \
           --name ${containername} quay.io/minio/minio server /data --console-address ":9001"
  echo "${appname} started."
fi
./dockerps.sh

echo "Open http://${callhost}:9001 and log in with admin:12345678"
