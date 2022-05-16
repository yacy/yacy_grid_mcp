#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
appname=RabbitMQ
containername=yacy-grid-rabbitmq

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
  docker run -d --restart unless-stopped \
         -p ${bindhost}:5672:5672 -p ${bindhost}:15672:15672 -p ${bindhost}:4369:4369 -p ${bindhost}:35197:35197 \
         -e RABBITMQ_CONFIG_FILE=/etc/rabbitmq/rabbitmq.conf \
         -v `pwd`/../conf/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf -v ${containername}:/var/lib/rabbitmq \
         --hostname ${containername} --name ${containername} rabbitmq:3.9-management-alpine
  echo "${appname} started."
fi
./dockerps.sh

echo "Open http://${callhost}:15672 and log in with guest:guest"
