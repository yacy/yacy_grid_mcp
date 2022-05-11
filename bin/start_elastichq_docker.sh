#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-elastichq" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-elastichq" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-elastichq
else
  docker run -d  --restart=unless-stopped --name yacy-grid-elastichq --link yacy-grid-elasticsearch -p 5000:5000 -e HQ_DEFAULT_URL=http://yacy-grid-elasticsearch:9200 elastichq/elasticsearch-hq
fi
echo "elasticshq started. Open http://127.0.0.1:5000"

