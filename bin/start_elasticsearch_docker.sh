#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`
appname=Elasticsearch
containername=yacy-grid-elasticsearch

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
  echo "Elasticsearch container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start ${containername}
  echo "${appname} container re-started"
else
  docker run -d --restart=unless-stopped -p ${bindhost}:9200:9200 -p ${bindhost}:9300:9300 \
         -v `pwd`/../conf/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
         -v ${containername}:/usr/share/elasticsearch/data \
         -e ES_JAVA_OPTS="-Xms1g -Xmx4g" --name ${containername} \
	 elasticsearch:6.8.20
  echo "${appname} container started."
fi
./dockerps.sh

echo "Open http://${callhost}:9200/_cat/health to check health status"
