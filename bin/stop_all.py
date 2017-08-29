#!/usr/bin/env python

import os
import socket
import subprocess
import signal

path_apphome = os.path.dirname(os.path.abspath(__file__)) + '/..'
os.chdir(path_apphome)
# os.system('ls')

def checkportopen(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    return sock.connect_ex(('127.0.0.1', port)) == 0


if checkportopen(9200):
    print "killing elasticsearch"
    pid = subprocess.check_output(['fuser', path_apphome + '/data/mcp-8100/apps/elasticsearch/bin/nohup.out'])
    print 'pid is ', pid
    os.kill(int(pid), signal.SIGTERM)
    
    
if checkportopen(2121):
    print "ftp server is running"
    
if checkportopen(15672):
    print "rabbitmq is running"
    
if checkportopen(8100):
    print "yacy_grid_mcp is running"

if checkportopen(8200):
    print "yacy_grid_loader is running"

if checkportopen(8300):
    print "yacy_grid_crawler is running"

if checkportopen(8500):
    print "yacy_grid_parser is running"
