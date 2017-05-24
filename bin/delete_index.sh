#!/usr/bin/env sh
cd "`dirname $0`"

curl -XDELETE 'http://elastic:changeme@localhost:9200/web'
echo
