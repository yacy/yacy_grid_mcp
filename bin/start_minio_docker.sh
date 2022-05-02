#!/bin/bash
cd "`dirname $0`"

containerRuns=$(docker ps | grep -i "yacy-grid-minio" | wc -l ) 
containerExists=$(docker ps -a | grep -i "yacy-grid-minio" | wc -l ) 

if [ ${containerRuns} -gt 0 ]; then
  echo "container is already running"
elif [ ${containerExists} -gt 0 ]; then
  docker start yacy-grid-minio
else
  docker run -d --restart=unless-stopped --name yacy-grid-minio -e "MINIO_ROOT_USER=admin" -e "MINIO_ROOT_PASSWORD=12345678" -v yacy-grid-minio:/data -p 127.0.0.1:9000:9000 -p 127.0.0.1:9001:9001 quay.io/minio/minio server /data --console-address ":9001"
fi
echo "Minio started. Open http://127.0.0.1:9001 and log in with admin:12345678"
