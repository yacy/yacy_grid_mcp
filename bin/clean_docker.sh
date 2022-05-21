#!/bin/bash
cd "`dirname $0`"

docker stop yacy-grid-elasticsearch
docker rm yacy-grid-elasticsearch

docker stop yacy-grid-rabbitmq
docker rm yacy-grid-rabbitmq

docker stop yacy-grid-minio
docker rm yacy-grid-minio

docker stop yacy-grid-elastichq
docker rm yacy-grid-elastichq

docker stop yacy-grid-grafana
docker rm yacy-grid-grafana

docker stop yacy-grid-mcp
docker rm yacy-grid-mcp
docker rmi yacy_grid_mcp

docker stop yacy-grid-crawler
docker rm yacy-grid-crawler
docker rmi yacy_grid_crawler

docker stop yacy-grid-loader
docker rm yacy-grid-loader
docker rmi yacy_grid_loader

docker stop yacy-grid-parser
docker rm yacy-grid-parser
docker rmi yacy_grid_parser

docker stop yacy-grid-search
docker rm yacy-grid-search
docker rmi yacy_grid_search

