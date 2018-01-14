#!/usr/bin/env sh
cd "`dirname $0`"

# the following command is not good, because afterwards the index must be prepared with the mapping again
#curl -XDELETE 'http://elastic:changeme@localhost:9200/web'

curl -XPOST 'http://elastic:changeme@localhost:9200/web/_delete_by_query' -d '{
    "query" : { 
        "match_all" : {}
    }
}'

echo
