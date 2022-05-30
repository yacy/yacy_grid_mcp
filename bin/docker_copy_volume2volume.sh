#!/bin/bash

# source must exist
docker volume inspect $1 > /dev/null 2>&1
if [ "$?" != "0" ]; then echo "source volume $1 does not exist"; exit; fi

# target may not exist and is created on the fly if not
docker volume inspect $2 > /dev/null 2>&1
if [ "$?" != "0" ]; then docker volume create --name $2; fi

# start copy using an alpine image where the volumes are mounted
docker run --rm -it -v $1:/from:ro -v $2:/to alpine sh -c "cd /from; cp -av . /to"

# show result
docker volume ls
