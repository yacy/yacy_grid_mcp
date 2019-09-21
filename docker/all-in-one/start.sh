adduser --disabled-password --gecos '' r
adduser r sudo
echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
chmod a+rwx /elasticsearch-5.5.0 -R
su -m r -c '/elasticsearch-5.5.0/bin/elasticsearch -Ecluster.name=yacygrid &'
cd /apache-ftpserver-1.1.0
./bin/ftpd.sh res/conf/ftpd-typical.xml &
/rabbitmq_server-3.6.6/sbin/rabbitmq-server -detached
sleep 5s;
/rabbitmq_server-3.6.6/sbin/rabbitmq-plugins enable rabbitmq_management
/rabbitmq_server-3.6.6/sbin/rabbitmqctl add_user yacygrid password4account
echo [{rabbit, [{loopback_users, []}]}]. >> /rabbitmq_server-3.6.6/etc/rabbitmq/rabbitmq.config
/rabbitmq_server-3.6.6/sbin/rabbitmqctl set_permissions -p / yacygrid ".*" ".*" ".*"
cd /yacy_grid_mcp
sleep 5s;
gradle run
