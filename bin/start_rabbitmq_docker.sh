#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-rabbitmq" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-rabbitmq" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-rabbitmq
else
  docker run -d --restart unless-stopped --hostname yacy-grid-rabbitmq --name yacy-grid-rabbitmq -p 127.0.0.1:5672:5672 -p 127.0.0.1:15672:15672 -p 127.0.0.1:4369:4369 -p 127.0.0.1:35197:35197 -e RABBITMQ_CONFIG_FILE=/etc/rabbitmq/rabbitmq.conf -v `pwd`/../conf/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf -v yacy-grid-rabbitmq:/var/lib/rabbitmq rabbitmq:3.9-management-alpine
fi
echo "RabbitMQ started. Open http://127.0.0.1:15672 and log in with guest:guest"
