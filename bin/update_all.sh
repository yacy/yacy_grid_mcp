#!/usr/bin/env sh
cd "`dirname $0`"

cd ..
git pull origin master
gradle assemble

cd ../yacy_grid_crawler
git submodule foreach git pull origin master
git pull origin master
gradle assemble

cd ../yacy_grid_loader
git submodule foreach git pull origin master
git pull origin master
gradle assemble

cd ../yacy_grid_parser
git submodule foreach git pull origin master
git pull origin master
gradle assemble
