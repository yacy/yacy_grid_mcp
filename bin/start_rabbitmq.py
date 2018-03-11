#!/usr/bin/env python2.7

# to run this script, erlang must be installed
# Mac OS: brew install erlang
# debian/ubuntu: apt-get install erlang

import os
import time
import socket
import urllib
import subprocess

#https://dl.bintray.com/rabbitmq/binaries/rabbitmq-server-generic-unix-3.6.12.tar.xz

rabbitversion = 'rabbitmq-server-generic-unix-3.6.12'
rabbitfilename = 'rabbitmq_server-3.6.12'

path_apphome = os.path.dirname(os.path.abspath(__file__)) + '/..'
os.chdir(path_apphome)
# os.system('ls')

def checkportopen(port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    return sock.connect_ex(('127.0.0.1', port)) == 0

def mkapps():
    if not os.path.isdir(path_apphome + '/data'): os.makedirs(path_apphome + '/data')
    if not os.path.isdir(path_apphome + '/data/apps'): os.makedirs(path_apphome + '/data/apps')

if not checkportopen(5672):
    print('rabbitmq is not running')
    mkapps()
    if not os.path.isfile(path_apphome + '/data/apps/' + rabbitversion + '.tar.xz'):
        print('downloading ' + rabbitversion)
        urllib.urlretrieve ('http://dl.bintray.com/rabbitmq/binaries/' + rabbitversion + '.tar.xz', path_apphome + '/data/apps/' + rabbitversion + '.tar.xz')
    rabbitpath = path_apphome + '/data/apps/rabbitmq'
    if not os.path.isdir(rabbitpath):
        print('decompressing ' + rabbitversion)
        os.system('tar xJf ' + path_apphome + '/data/apps/' + rabbitversion + '.tar.xz -C ' + path_apphome + '/data/apps/')
        os.rename(path_apphome + '/data/apps/' + rabbitfilename, rabbitpath)
    # run rabbitmq
    print('running rabbitmq')
    os.chdir(rabbitpath + '/sbin')
    subprocess.call('./rabbitmq-server &', shell=True)
    time.sleep(5)
    subprocess.call('./rabbitmq-plugins enable rabbitmq_management', shell=True)
    subprocess.call('./rabbitmqctl add_user anonymous yacy', shell=True)
    subprocess.call('./rabbitmqctl set_user_tags anonymous administrator', shell=True)
    subprocess.call('./rabbitmqctl set_permissions -p / anonymous ".*" ".*" ".*"', shell=True)

print('to view the administration pages, open http://127.0.0.1:15672/')
print('log in with anonymous:yacy')
