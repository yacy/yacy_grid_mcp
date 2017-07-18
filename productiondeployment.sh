#!/usr/bin/env bash
chmod +x $0
mkdir ~/yacy
cd ~/yacy

if [ "$1" = 'build' ]
then

	#install required dependencies
	apt-get install -y git openjdk-8-jdk
	apt-get update && apt-get install -y software-properties-common

	#install gradle
	add-apt-repository ppa:cwchien/gradle -y
	apt-get update
	apt-get install -y wget
	wget https://services.gradle.org/distributions/gradle-3.4.1-bin.zip
	mkdir /opt/gradle
	apt-get install -y unzip
	unzip -d /opt/gradle gradle-3.4.1-bin.zip
	export PATH=$PATH:/opt/gradle/gradle-3.4.1/bin
	export GRADLE_HOME=/opt/gradle/gradle-3.4.1
	export PATH=$PATH:$GRADLE_HOME/bin
	gradle -v

	#install apache ftp server
	wget http://www-eu.apache.org/dist/mina/ftpserver/1.1.0/dist/apache-ftpserver-1.1.0.tar.gz
	tar xfz apache-ftpserver-1.1.0.tar.gz

	#insall rabbitmq server
	wget https://www.rabbitmq.com/releases/rabbitmq-server/v3.6.6/rabbitmq-server-generic-unix-3.6.6.tar.xz
	tar xf rabbitmq-server-generic-unix-3.6.6.tar.xz
	apt-get install -y erlang

	#install elastic search
	wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.5.0.tar.gz
	sha1sum elasticsearch-5.5.0.tar.gz 
	tar -xzf elasticsearch-5.5.0.tar.gz

	#insall yacy_grid_mcp
	git clone https://github.com/yacy/yacy_grid_mcp.git
	cd yacy_grid_mcp
	cat docker/config-ftp.properties > ../apache-ftpserver-1.1.0/res/conf/users.properties


	# compile yacygridmcp
	gradle build
	mkdir data/mcp-8100/conf/ -p
	cp docker/config-mcp.properties data/mcp-8100/conf/config.properties
	cd ..


	#pull and run yacy_grid_parser
	git clone --recursive https://github.com/yacy/yacy_grid_parser.git
	cd yacy_grid_parser
	gradle build
	cd ..

	#pull and run yacy_grid_crawler
	git clone --recursive https://github.com/yacy/yacy_grid_crawler.git
	cd yacy_grid_crawler
	gradle build
	cd ..

	#pull and run yacy_grid_loader
	git clone --recursive https://github.com/yacy/yacy_grid_loader.git
	cd yacy_grid_loader
	gradle build
	cd ..
	adduser --disabled-password --gecos '' elastic
	adduser elastic sudo
	echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
	chmod a+rwx ~/yacy/elasticsearch-5.5.0 -R

fi

if [ "$1" = 'run' ]
then
	#run all required commands to start all required services
	su -m elastic -c '~/yacy/elasticsearch-5.5.0/bin/elasticsearch -Ecluster.name=yacygrid &'
	cd ~/yacy/apache-ftpserver-1.1.0
	./bin/ftpd.sh res/conf/ftpd-typical.xml &
	~/yacy/rabbitmq_server-3.6.6/sbin/rabbitmq-server -detached
	sleep 5s;
	~/yacy/rabbitmq_server-3.6.6/sbin/rabbitmq-plugins enable rabbitmq_management
	~/yacy/rabbitmq_server-3.6.6/sbin/rabbitmqctl add_user yacygrid password4account
	echo [{rabbit, [{loopback_users, []}]}]. >> /rabbitmq_server-3.6.6/etc/rabbitmq/rabbitmq.config
	~/yacy/rabbitmq_server-3.6.6/sbin/rabbitmqctl set_permissions -p / yacygrid ".*" ".*" ".*"
	cd ~/yacy/yacy_grid_mcp
	sleep 5s;
	gradle run &
	sleep 5s;
	cd ../yacy_grid_parser
	gradle run &
	cd ../yacy_grid_crawler
	gradle run &
	cd ../yacy_grid_loader
	gradle run &
	cd ..
fi
