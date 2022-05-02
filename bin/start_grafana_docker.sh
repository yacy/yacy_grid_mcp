#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-grafana" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-grafana" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-grafana
else
  docker run -d --restart=unless-stopped --name yacy_grid-grafana -p 127.0.0.1:3000:3000 --link yacy-grid-elasticsearch grafana/grafana-enterprise
fi
echo "Grafana started. Open http://127.0.0.1:3000 and log in with admin:admin"
