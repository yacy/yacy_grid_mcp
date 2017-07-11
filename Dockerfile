FROM ubuntu:latest
MAINTAINER Harshit Prasad

# Update
RUN apt-get update
RUN apt-get upgrade -y

# add packages
RUN apt-get install -y git openjdk-8-jdk

#install gradle required for build
RUN add-apt-repository ppa:cwchien/gradle
RUN apt-get update
RUN apt-get install gradle

# copy mcp configuration file
COPY data/mcp-8100/conf/config.properties

# install apache ftp server 1.1.0
RUN wget http://www-eu.apache.org/dist/mina/ftpserver/1.1.0/dist/apache-ftpserver-1.1.0.tar.gz
RUN tar xfz apache-ftpserver-1.1.0.tar.gz
RUN cat config-ftp.properties >> apache-ftpserver-1.1.0/res/conf/users.properties

# run ftp server
RUN cd apache-ftpserver-1.1.0 bin/ftpd.sh res/conf/ftpd-typical.xml

# install RabbitMQ server
RUN wget https://www.rabbitmq.com/releases/rabbitmq-server/v3.6.6/rabbitmq-server-generic-unix-3.6.6.tar.xz
RUN tar xf rabbitmq-server-generic-unix-3.6.6.tar.xz

# run the RabbitMQ server
RUN cd rabbitmq_server-3.6.6/sbin/rabbitmq-server

# install erlang language for RabbitMQ
RUN apt-get install erlang

# install the management plugin to be able to use a web interface
RUN rabbitmq_server-3.6.10/sbin/rabbitmq-plugins enable rabbitmq_management

# use the same username and password as given in instructions
RUN rabbitmq_server-3.6.6/sbin/rabbitmqctl add_user yacygrid password4account
RUN echo [{rabbit, [{loopback_users, []}]}]. >> rabbitmq_server-3.6.6/etc/rabbitmq/rabbitmq.config

# clone the github repo
RUN git clone https://github.com/yacy/yacy_grid_mcp.git
WORKDIR /yacy_grid_mcp

# compile
RUN gradle build

# Expose web interface ports
# 2121: ftp, a FTP server to be used for mass data / file storage
# 5672: rabbitmq, a rabbitmq message queue server to be used for global messages, queues and stacks
# 9300: elastic, an elasticsearch server or main cluster address for global database storage
EXPOSE 2121 5672 9300

# Define default command.
CMD ["yacy-start"]
