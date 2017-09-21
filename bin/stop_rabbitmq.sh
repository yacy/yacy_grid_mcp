#!/usr/bin/env sh
ps aux | grep rabbitmq | grep -v 'grep' |awk '{print $2}' | cut -d/ -f 1 | xargs kill
