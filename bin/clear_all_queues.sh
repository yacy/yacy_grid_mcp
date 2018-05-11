#!/usr/bin/env sh
cd "`dirname $0`"

./clear_crawler_queues.sh &
./clear_parser_queues.sh &
./clear_indexer_queues.sh &
./clear_loader_queues.sh

