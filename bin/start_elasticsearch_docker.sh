#!/bin/bash

containerRuns=$(docker ps | grep -i "yacy-grid-elasticsearch" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-elasticsearch" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-elasticsearch
else
  docker run -d --name yacy-grid-elasticsearch -p 9200:9200 -p 9300:9300 -v yacy-grid-elasticsearch-data:/usr/share/elasticsearch/data -e "discovery.type=single-node" elasticsearch:6.8.20
fi


