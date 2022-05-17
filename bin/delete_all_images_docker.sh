#!/bin/bash
cd "`dirname $0`"

./clean_docker.sh

docker rmi yacy_grid_mcp
docker rmi yacy_grid_crawler
docker rmi yacy_grid_loader
docker rmi yacy_grid_parser
docker rmi yacy_grid_search
