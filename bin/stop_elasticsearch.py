#!/usr/bin/env python2.7

import subprocess

p = subprocess.Popen("ps aux | grep 'org.elasticsearch.bootstrap.Elasticsearch' | grep -v grep | awk '{print $2}' | cut -d/ -f 1", shell=True, stdout=subprocess.PIPE)
pid = p.stdout.read()

if pid:
    print('killing elasticsearch, pid = ' + pid)
    subprocess.call('kill ' + pid, shell=True)
else:
    print('elasticsearch was not running')
