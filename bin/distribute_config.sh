#!/usr/bin/env sh
cd "`dirname $0`"
cd ..
cp data/mcp-8100/conf/config.properties ../yacy_grid_crawler/data/crawler-8300/conf/
cp data/mcp-8100/conf/config.properties ../yacy_grid_loader/data/loader-8200/conf/
cp data/mcp-8100/conf/config.properties ../yacy_grid_parser/data/parser-8500/conf/
