#!/usr/bin/env sh
cd "`dirname $0`"

cd ..
echo updating `pwd`
git pull origin master
gradle assemble

cd ../yacy_grid_crawler
echo updating `pwd`
git submodule foreach git pull origin master
git pull origin master
gradle assemble

cd ../yacy_grid_loader
echo updating `pwd`
git submodule foreach git pull origin master
git pull origin master
gradle assemble

cd ../yacy_grid_parser
echo updating `pwd`
git submodule foreach git pull origin master
git pull origin master
gradle assemble
