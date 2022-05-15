#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
containerRuns=$(docker ps | grep -i "yacy_grid_minio" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy_grid_minio" | wc -l ) 

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
  echo "MinIO container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy_grid_minio
  echo "MinIO container re-started"
else
  docker run -d --restart=unless-stopped --name yacy_grid_minio -e "MINIO_ROOT_USER=admin" -e "MINIO_ROOT_PASSWORD=12345678" -v yacy_grid_minio:/data -p ${bindhost}:9000:9000 -p ${bindhost}:9001:9001 quay.io/minio/minio server /data --console-address ":9001"
  echo "Minio started."
fi
echo "Open http://${callhost}:9001 and log in with admin:12345678"
