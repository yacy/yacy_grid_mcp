#!/usr/bin/env sh
cd "`dirname $0`"
cd ../conf/mappings
curl -XPUT http://elastic:changeme@localhost:9200/web -H 'Content-Type: application/json' --data-binary "@web.json"
echo
