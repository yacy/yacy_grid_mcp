#!/bin/bash
cd "`dirname $0`"

./start_elasticsearch_docker.sh
./start_rabbitmq_docker.sh
./start_minio_docker.sh

cd ..
git pull origin master

sleep 3

bin/start_mcp_docker.sh

cd ../yacy_grid_crawler/
git pull origin master

sleep 3

bin/start_crawler_docker.sh

cd ../yacy_grid_loader
git pull origin master
bin/start_loader_docker.sh

cd ../yacy_grid_parser
git pull origin master
bin/start_parser_docker.sh

cd ../yacy_grid_search
git pull origin master
bin/start_search_docker.sh
