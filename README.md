# YaCy Grid Component: Master Connect Program (MCP)

The YaCy Grid is the second-generation implementation of YaCy, a peer-to-peer search engine.
A YaCy Grid installation consists of a set of micro-services which communicate with each other
using a common infrastructure for data persistence. The required storage functions of the YaCy Grid are:
- An asset storage, basically a file sharing environment for YaCy components
- A message system providing an Enterprise Integration Framework using a message-oriented middleware
- A database system providing search-engine related retrieval functions.

All YaCy components are microservices which can be deployed i.e. using Docker or other application
hosting methods which are provided as cloud services from many different providers. A YaCy grid will
scale with the number of microservices that connect to a common broker.
That broker is the MCP, this application.

The MCP provides http servlets to access the services mentioned above: assets, messages, databases.
If a YaCy Grid microservice wants to connect to this infrastructure, it connects to the MCP to
use them. The API to access MCP servlets are integrated into the MCP as well, every YaCy Grid service
must integrate the whole MCP as infrastructure to get access to the API. This makes it possible that
the MCP acts also as router to the infrastructure: if a YaCy Grid service has used the MCP once, it
learns from the MCP to connect to the infrastucture itself. For example:
- a YaCy Grid service starts up and connects to the MCP
- the Grid service pushes a message to the message queue using the MCP
- the MCP fullfills the message send operation and respons with the actual address of the message broker
- the YaCy Grid service learns the direct connection information
- whenever the YaCy Grid service wants to connect to the message broker again, it can do so using a direct broker connection.
This process is done transparently, the Grid service does not need to handle such communication details itself. The routing is done automatically.
To use the MCP inside other grid components the git submodule functionality is used.

The asset storage, message distribution and database services provided by the MCP are handled in an
abstract way, that means there is no need to know which system actually performs the operation.
However, the following external services are supposed to be supported by the MCP:
- ftp server for asset storage
- RabbitMQ message queues for the message system
- Elasticsearch for database operations.

For a full enterprise-integration installation, the operator has to start and support a ftp server, a rabbitmq and an elasticsearch. However, all of them can be omitted and the MCP will provide all of these services with built-in functions. For database operations and message storage a MapDB is used, asset files are stored in a local data directory.

## Port numbers in the YaCy Grid:
Every YaCy Grid service has a default port. However every service can be started several times.
The service is able to detect that another service is running on the same port and will choose an
alternative port. That means we will not have any port number collisions any more, just more microservices for one grid.

The default port number of the MCP is 8100

Other port numbers will be:
-  8200: webloader, a http(s) loader acting as headless browser which is able to enrich http with AJAX content
-  8300: webcrawler, a crawler which loads a lot of documents from web documents
-  8400: warcmanager, a process which combines single WARC files to bigger ones to create archives
-  8500: yacyparser, a parser service which turns WARC into YaCy JSON
-  8600: yacyenricher, a semantic enricher for YaCy JSON objects
-  8700: yacyindexer, a loader which pushes parsed/enriched YaCy JSON content to a search index
-  8800: aggregation, a search front-end which combines different index sources into one
-  8900: moderation, a search front-end which for content moderation, i.e. search index account management
- 10100: successmessages, a service which handles the successful operation messages
- 10200: errormessages, a service which handles failure messages and broken action chains
-  2121: ftp, a FTP server to be used for mass data / file storage
-  5672: rabbitmq, a rabbitmq message queue server to be used for global messages, queues and stacks
-  9300: elastic, an elasticsearch server or main cluster address for global database storage

## Communication
Please join our forums at http://forum.yacy.net

## How do I install the yacy_grid_mcp: Download, Build, Run
At this time, yacy_grid_mcp is not provided in compiled form, you easily build it yourself. It's not difficult and done in one minute! The source code is hosted at https://github.com/yacy/yacy_grid_mcp, you can download it and run loklak with:

    > git clone https://github.com/yacy/yacy_grid_mcp.git
    > cd yacy_grid_mcp
    > gradle run

### Requirements for development

Maven and Gradle tools should be installed. To refresh gradle settings in eclipse, right click on project -> configure -> Add Gradle Nature

## How to install the infrastructure services (message server, ftp server)?

### install and start apache ftp server
- Download apache-ftpserver-1.1.0.tar.gz (or a later version) from https://mina.apache.org/downloads-ftpserver.html
- decompress the server package with

    > tar xfz apache-ftpserver-1.1.0.tar.gz
    
- modify the write right of the anonymous user that we will use to write assets: edit the file

    apache-ftpserver-1.1.0/res/conf/users.properties

- set the write permission for anonymous (we will introduce user accounts later, for now we use anonymous)

    ftpserver.user.anonymous.writepermission=true
    
- set a home path for the user, i.e.

    ftpserver.user.anonymous.homedirectory=/Users/admin/Downloads/ftphome
    
- edit the file apache-ftpserver-1.1.0/bin/ftpd.sh and set JAVA_HOME according to your system, i.e. on a Mac you set

    JAVA_HOME=/usr
    
- run the server, doing:

    > cd apache-ftpserver-1.1.0
    > bin/ftpd.sh res/conf/ftpd-typical.xml
    
This will run the ftp server at port 2121. To test the connection use a standard ftp client and start it with

    > ftp -P 2121 anonymous@127.0.0.1

### install and start rabbitmq:

- Download rabbitmq-server-generic-unix-3.6.6.tar.xz
- Extract the package with

    > tar xf rabbitmq-server-generic-unix-3.6.6.tar.xz

- Run the server with

    > rabbitmq_server-3.6.6/sbin/rabbitmq-server

- install the management plugin to be able to use a web interface:

    > rabbitmq_server-3.6.6/sbin/rabbitmq-plugins enable rabbitmq_management

- To view the administration pages, open in your web browser: http://127.0.0.1:15672
- open http://127.0.0.1:15672/api/ for an api documentation
- open http://127.0.0.1:15672/cli/ for the rabbitmqadmin

## How to use the API
To test the api, try the following example:

### Writing messages
Call:

    curl "127.0.0.1:8100/yacy/grid/mcp/messages/send.json?serviceName=testService&queueName=testQueue&message=hello_world"
    
This will send a message "hello_world" to the queue 'test' of service 'test'. You can ask for the number of entries in the queue with

    curl "http://127.0.0.1:8100/yacy/grid/mcp/messages/available.json?serviceName=testService&queueName=testQueue"
    
To get an entry from such a queue, call:

    curl "http://127.0.0.1:8100/yacy/grid/mcp/messages/receive.json?serviceName=testService&queueName=testQueue"
    
If you did not run the RabbitMQ message server, the messages are written to

    data/mcp-8100/messages/testService
    
If you started a RabbitMQ message server, please monitor the writing of the messages at the web page http://127.0.0.1:15672

### Writing assets
Call:

    curl "http://127.0.0.1:8100/yacy/grid/mcp/assets/store.json?path=/xx/test.txt&asset=hello_world"
    
This will write an asset to the path xx/test.txt with the content "hello_world".

    curl "http://127.0.0.1:8100/yacy/grid/mcp/assets/load?path=/xx/test.txt"
    
will load the asset again.

If you started a ftp server, the file(s) will be written relatively to the root path of the ftp home path.
If you did not start a ftp server, you can find the file in data/mcp-8100/assets/xx/test.txt


### Using a second MCP to use the primary MCP
The MCP organises the connection to the remote RabbitMQ server and a ftp server, but if another Grid Service is started, the MCP tells that service to handle the connection to the RabbitMQ and ftp server itself. The MCP can act as such an external service: just start another MCP and it will run at port 8101. Repeat the curl commands as given in the example above, but now use port 8101 to access the MCP. You will see that the second MCP learns from the first MCP to handle the connection by itself.


## What is the software license?
LGPL 2.1


Have fun!

@0rb1t3r
