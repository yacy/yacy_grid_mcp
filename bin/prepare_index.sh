#!/usr/bin/env sh
cd "`dirname $0`"
cd ../conf/mappings
curl -XPUT http://elastic:changeme@localhost:9200/web --data-binary "@web.json"
echo
