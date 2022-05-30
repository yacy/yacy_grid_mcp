#!/bin/bash
cd "`dirname $0`"

docker stop yacy-grid-search
docker stop yacy-grid-parser
docker stop yacy-grid-loader
docker stop yacy-grid-crawler
docker stop yacy-grid-mcp

docker rm yacy-grid-mcp
docker rm yacy-grid-crawler
docker rm yacy-grid-loader
docker rm yacy-grid-parser
docker rm yacy-grid-search

docker rmi yacy_grid_mcp
docker rmi yacy_grid_crawler
docker rmi yacy_grid_loader
docker rmi yacy_grid_parser
docker rmi yacy_grid_search

cd ..
git pull origin master
./bin/start_mcp_docker.sh
cd ../yacy_grid_crawler

sleep 10
git pull origin master
./bin/start_crawler_docker.sh
cd ../yacy_grid_loader

git pull origin master
./bin/start_loader_docker.sh
cd ../yacy_grid_parser

git pull origin master
./bin/start_parser_docker.sh
cd ../yacy_grid_search

git pull origin master
./bin/start_search_docker.sh
cd ../yacy_grid_mcp/bin
