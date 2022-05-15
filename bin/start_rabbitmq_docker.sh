#!/bin/bash
cd "`dirname $0`"

bindhost="0.0.0.0"
callhost=`hostname`.local
containerRuns=$(docker ps | grep -i "yacy_grid_rabbitmq" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy_grid_rabbitmq" | wc -l ) 

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
  echo "RabbitMQ container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy_grid_rabbitmq
  echo "RabbitMQ container re-started"
else
  docker run -d --restart unless-stopped --hostname yacy_grid_rabbitmq --name yacy_grid_rabbitmq -p ${bindhost}:5672:5672 -p ${bindhost}:15672:15672 -p ${bindhost}:4369:4369 -p ${bindhost}:35197:35197 -e RABBITMQ_CONFIG_FILE=/etc/rabbitmq/rabbitmq.conf -v `pwd`/../conf/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf -v yacy_grid_rabbitmq:/var/lib/rabbitmq rabbitmq:3.9-management-alpine
  echo "RabbitMQ started."
fi
echo "Open http://${callhost}:15672 and log in with guest:guest"
