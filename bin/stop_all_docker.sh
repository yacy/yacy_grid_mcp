#!/bin/bash
cd "`dirname $0`"

docker stop yacy-grid-elastichq
docker stop yacy-grid-grafana
docker stop yacy-grid-crawler
docker stop yacy-grid-loader
docker stop yacy-grid-parser
docker stop yacy-grid-search
docker stop yacy-grid-mcp
docker stop yacy-grid-elasticsearch
docker stop yacy-grid-rabbitmq
docker stop yacy-grid-minio

