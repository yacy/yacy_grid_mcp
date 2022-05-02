#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-elasticsearch" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-elasticsearch" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-elasticsearch
else
  docker run -d --restart=unless-stopped --name yacy-grid-elasticsearch -p 127.0.0.1:9200:9200 -p 127.0.0.1:9300:9300 -v `pwd`/../conf/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml -v yacy-grid-elasticsearch-data:/usr/share/elasticsearch/data elasticsearch:6.8.20
fi
echo "Elasticsearch started. Open http://127.0.0.1:9200/_cat/health to check health status"

