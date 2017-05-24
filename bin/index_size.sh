#!/usr/bin/env sh
cd "`dirname $0`"

curl http://elastic:changeme@localhost:9200/web/_count
echo
