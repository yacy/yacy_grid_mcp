#!/usr/bin/env sh
ps aux | grep elasticsearch | grep -v 'grep' |awk '{print $2}' | cut -d/ -f 1 | xargs kill
