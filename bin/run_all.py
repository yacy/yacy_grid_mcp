#!/usr/bin/env python

import os
import socket
import urllib
import subprocess
from multiprocessing import Process


path_apphome = os.path.dirname(os.path.abspath(__file__)) + '/..'
os.chdir(path_apphome)
# os.system('ls')

def checkportopen(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    return sock.connect_ex(('127.0.0.1', port)) == 0

def mkapps():
    if not os.path.isdir(path_apphome + '/data'): os.makedirs(path_apphome + '/data')
    if not os.path.isdir(path_apphome + '/data/mcp-8100'): os.makedirs(path_apphome + '/data/mcp-8100')
    if not os.path.isdir(path_apphome + '/data/mcp-8100/apps'): os.makedirs(path_apphome + '/data/mcp-8100/apps')

def run_mcp():
    os.system('gradle run')

def run_loader():
    os.system('cd ../yacy_grid_loader')
    os.system('gradle run')

def run_crawler():
    os.system('cd ../yacy_grid_crawler')
    os.system('gradle run')

def run_parser():
    os.system('cd ../yacy_grid_parser')
    os.system('gradle run')
    

if not checkportopen(9200):
    print "Elasticsearch is not running"
    mkapps()
    elasticversion = 'elasticsearch-5.6.5'
    if not os.path.isfile(path_apphome + '/data/mcp-8100/apps/' + elasticversion + '.tar.gz'):
        print('Downloading ' + elasticversion)
        urllib.urlretrieve ('https://artifacts.elastic.co/downloads/elasticsearch/' + elasticversion + '.tar.gz', path_apphome + '/data/mcp-8100/apps/' + elasticversion + '.tar.gz')
    if not os.path.isdir(path_apphome + '/data/mcp-8100/apps/elasticsearch'):
        print('Decompressing' + elasticversion)
        os.system('tar xfz ' + path_apphome + '/data/mcp-8100/apps/' + elasticversion + '.tar.gz -C ' + path_apphome + '/data/mcp-8100/apps/')
        os.rename(path_apphome + '/data/mcp-8100/apps/' + elasticversion, path_apphome + '/data/mcp-8100/apps/elasticsearch')
    # run elasticsearch
    print('Running Elasticsearch')
    os.chdir(path_apphome + '/data/mcp-8100/apps/elasticsearch/bin')
    os.system('nohup ./elasticsearch &')

os.chdir(path_apphome)

if checkportopen(15672):
    print "RabbitMQ is Running"
    print "If you have configured it according to YaCy setup press N"
    print "If you have not configured it according to YaCy setup or Do not know what to do press Y"
    n=raw_input()
    if(n=='Y' or n=='y'):
        os.system('service rabbitmq-server stop')
        
if not checkportopen(15672):
    print "rabbitmq is not running"
    os.system('python bin/start_rabbitmq.py')
    
#subprocess.call('bin/update_all.sh')

if not checkportopen(2121):
    print "ftp server is not Running"
   

p1=None
p2=None
p3=None
p4=None
if not checkportopen(8100):
    print "yacy_grid_mcp is not running,running yacy_grid_mcp"
    p1 = Process(target=run_mcp)
    p1.start()
    

if not checkportopen(8200):
    print "yacy_grid_loader is not running,running yacy_grid_loader"
    p2 = Process(target=run_loader)
    p2.start()
    

if not checkportopen(8300):
    print "yacy_grid_crawler is not running,running yacy_grid_crawler"
    p3 = Process(target=run_crawler)
    p3.start()
    

if not checkportopen(8500):
    print "yacy_grid_parser is not running,running yacy_grid_parser"
    p4 = Process(target=run_parser)
    p4.start()

if p1 :
    p1.join
if p2 :
    p2.join
if p3 :
    p3.join
if p4 :
    p4.join
    
    
