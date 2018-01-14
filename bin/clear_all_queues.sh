./clear_loader_queues.sh
curl 'http://127.0.0.1:8100/yacy/grid/mcp/messages/clear.json?serviceName=crawler&queueName=webcrawler'
curl 'http://127.0.0.1:8100/yacy/grid/mcp/messages/clear.json?serviceName=indexer&queueName=elasticsearch'
curl 'http://127.0.0.1:8100/yacy/grid/mcp/messages/clear.json?serviceName=parser&queueName=yacyparser'
