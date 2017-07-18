cd /apache-ftpserver-1.1.0 
./bin/ftpd.sh res/conf/ftpd-typical.xml &
/rabbitmq_server-3.6.6/sbin/rabbitmq-server -detached
sleep 5s;
/rabbitmq_server-3.6.6/sbin/rabbitmq-plugins enable rabbitmq_management
/rabbitmq_server-3.6.6/sbin/rabbitmqctl add_user yacygrid password4account
echo [{rabbit, [{loopback_users, []}]}]. >> /rabbitmq_server-3.6.6/etc/rabbitmq/rabbitmq.config
/rabbitmq_server-3.6.6/sbin/rabbitmqctl set_permissions -p / yacygrid ".*" ".*" ".*"
elasticsearch-5.5.0/bin/elasticsearch -d -p pid -Ecluster.name=yacygrid
cd /yacy_grid_mcp
sleep 5s;
gradle run
