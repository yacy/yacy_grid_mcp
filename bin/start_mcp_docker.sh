#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-mcp" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-mcp" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-mcp
else
  docker run -d --restart=unless-stopped -p 127.0.0.1:8100:8100 --link yacy-grid-minio --link yacy-grid-rabbitmq --link yacy-grid-elasticsearch -e YACYGRID_GRID_S3_ADDRESS=admin:12345678@yacy-grid-minio:9000 -e YACYGRID_GRID_BROKER_ADDRESS=guest:guest@yacy-grid-rabbitmq:5672 -e YACYGRID_GRID_ELASTICSEARCH_ADDRESS=yacy-grid-elasticsearch:9300 --name yacy-grid-mcp yacy-grid-mcp
fi
echo "YaCy Grid MCP started. To get the app status, open http://localhost:8100/yacy/grid/mcp/info/status.json"
