#!/usr/bin/env sh
cd "`dirname $0`"
cd ../conf/mappings
curl -XPUT http://elastic:changeme@localhost:9200/web -H 'Content-Type: application/json' --data-binary "@web.json"
curl -XPUT http://elastic:changeme@localhost:9200/crawler -H 'Content-Type: application/json' --data-binary "@crawler.json"
echo
