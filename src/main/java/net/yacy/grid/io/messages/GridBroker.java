/**
 *  GridBroker
 *  Copyright 14.01.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.io.messages;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import net.yacy.grid.Services;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.mcp.Data;

/**
 * This GridBroker is a local implementation of the broker interface, using either a connection to a RabbitMQ
 * or another MCP on the grid. As a fail-over, the PeerBroker is used to connect a local database as broker.
 * 
 * Key element of the usage of an external MCP as broker is the connection 
 */
public class GridBroker extends PeerBroker implements Broker<byte[]> {

    private QueueFactory<byte[]> rabbitConnector;
    private QueueFactory<byte[]> mcpConnector;

    private String rabbitMQ_host, rabbitMQ_username, rabbitMQ_password;
    private int rabbitMQ_port;
    private String mcp_host;
    private int mcp_port;
    private boolean lazy;
    
    public GridBroker(File basePath, boolean lazy) {
        super(basePath);
        this.rabbitConnector = null;
        this.mcpConnector = null;
        this.rabbitMQ_host = null;
        this.rabbitMQ_port = -1;
        this.rabbitMQ_username = null;
        this.rabbitMQ_password = null;
        this.mcp_host = null;
        this.mcp_port = -1;
        this.lazy = lazy;
    }
    
    public static String serviceQueueName(Services service, GridQueue queue) {
        return service.name() + '_' + queue.name();
    }

    public boolean connectRabbitMQ(String address) {
        if (!address.startsWith(RabbitQueueFactory.PROTOCOL_PREFIX)) return false;
        address = address.substring(RabbitQueueFactory.PROTOCOL_PREFIX.length());
        return connectRabbitMQ(Data.getHost(address), Data.getPort(address, "-1"), Data.getUser(address, null), Data.getPassword(address, null));
    }
    
    public boolean connectRabbitMQ(String host, int port, String username, String password) {
        //boolean firsttry = false;
        if (this.rabbitMQ_host == null) {
            this.rabbitMQ_host = host;
            this.rabbitMQ_port = port;
            this.rabbitMQ_username = username;
            this.rabbitMQ_password = password;
            //firsttry = true;
        }
        try {
            QueueFactory<byte[]> qc = new RabbitQueueFactory(host, port, username, password, lazy);
            this.rabbitConnector = qc;
            Data.logger.info("Broker/Client: connected to the rabbitMQ broker at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            /*if (firsttry)*/ Data.logger.info("Broker/Client: trying to connect to the rabbitMQ broker at " + host + ":" + port + " failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean isRabbitMQConnected() {
        return this.rabbitConnector != null;
    }

    public boolean connectMCP(String host, int port) {
        //boolean firsttry = false;
        if (this.mcp_host == null) {
            this.mcp_host = host;
            this.mcp_port = port;
            //firsttry = true;
        }
        try {
            QueueFactory<byte[]> mcpqf = new MCPQueueFactory(this, host, port);
            mcpqf.getQueue(YaCyServices.indexer.name() + "_" + YaCyServices.indexer.getQueues()[0].name()).checkConnection();
            this.mcpConnector = mcpqf;
            Data.logger.info("Broker/Client: connected to a Queue over MCP at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            /*if (firsttry)*/ Data.logger.info("Broker/Client: trying to connect to a Queue over MCP at " + host + ":" + port + " failed");
            return false;
        }
    }

    private final static Pattern SPACE2 = Pattern.compile("  ");
    
    private final static String messagePP(byte[] message) {
        if (message == null) return "NULL";
        String m = new String(message, 0, Math.min(1000, message.length), StandardCharsets.UTF_8);
        m = m.replace('\n', ' ');
        while (true) {
            int l = m.length();
            m = SPACE2.matcher(m).replaceAll(" ");
            if (m.length() == l) break;
        }
        return m;
    }
    
    @Override
    public QueueFactory<byte[]> send(Services serviceName, GridQueue queueName, byte[] message) throws IOException {
        if (this.rabbitConnector == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitConnector == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            Data.logger.info("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(message));
            return this.rabbitConnector;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpConnector == null && this.mcp_host != null) {
        	    // try to connect again..
        	    connectMCP(this.mcp_host, this.mcp_port);
        	    if (this.mcpConnector == null) {
        	        Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
        	    }
        }
        if (this.mcpConnector != null) try {
            this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            Data.logger.info("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(message));
            return this.mcpConnector;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: send() on peer broker/local db");
        return super.send(serviceName, queueName, message);
    }

    @Override
    public MessageContainer<byte[]> receive(Services serviceName, GridQueue queueName, long timeout) throws IOException {
        if (this.rabbitConnector == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
      	if (this.rabbitConnector == null) {
      	    this.rabbitMQ_host = null;
      	} else try {
        	    Queue<byte[]> rabbitQueue = this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName));
        	    byte[] message = rabbitQueue.receive(timeout);
        	    if (message == null) return null; // this is not a failure, we actually have a connection but we do not get content because the queue may be empty
            MessageContainer<byte[]> mc = new MessageContainer<byte[]>(this.rabbitConnector, message);
            if (mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: received rabbitMQ service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
            return mc;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: receive rabbitMQ service '" + serviceName + "', queue '" + queueName + "',rabbitmq fail", e);
        }
        	if (this.mcpConnector == null && this.mcp_host != null) {
        	    // try to connect again..
        	    connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpConnector == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! receive mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        	if (this.mcpConnector != null) try {
            Queue<byte[]> mcpQueue = this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName));
            byte[] message = mcpQueue.receive(timeout);
            MessageContainer<byte[]> mc = new MessageContainer<byte[]>(this.mcpConnector, message);
            if (mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: receive mcp service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
            return mc;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: receive mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: receive() on peer broker/local db");
        MessageContainer<byte[]> mc = super.receive(serviceName, queueName, timeout);
        if (mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: received peer broker/local db service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
        return mc;  
    }

    @Override
    public AvailableContainer available(Services serviceName, GridQueue queueName) throws IOException {
        if (this.rabbitConnector == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitConnector == null) {
            this.rabbitMQ_host = null;
        } else try {
            AvailableContainer ac = new AvailableContainer(this.rabbitConnector, this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).available());
            return ac;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: available rabbitMQ service '" + serviceName + "', queue '" + queueName + "',rabbitmq fail", e);
        }
        if (this.mcpConnector == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpConnector == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! available mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpConnector != null) try {
            AvailableContainer ac = new AvailableContainer(this.mcpConnector, this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).available());
            return ac;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: available mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: available() on peer broker/local db");
        return super.available(serviceName, queueName);
    }
    
    @Override
    public QueueFactory<byte[]> clear(Services serviceName, GridQueue queueName) throws IOException {
        if (this.rabbitConnector == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitConnector == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).clear();
            Data.logger.info("Broker/Client: clear rabbitMQ service '" + serviceName + "', queue '" + queueName + "'");
            return this.rabbitConnector;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpConnector == null && this.mcp_host != null) {
                // try to connect again..
                connectMCP(this.mcp_host, this.mcp_port);
                if (this.mcpConnector == null) {
                    Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
                }
        }
        if (this.mcpConnector != null) try {
            this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).clear();
            Data.logger.info("Broker/Client: clear mcp service '" + serviceName + "', queue '" + queueName + "'");
            return this.mcpConnector;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: send() on peer broker/local db");
        return super.clear(serviceName, queueName);
    }
    
    public void close() {
        if (this.rabbitConnector != null) try {this.rabbitConnector.close();} catch (Throwable e) {}
        if (this.mcpConnector != null) try {this.mcpConnector.close();} catch (Throwable e) {}
        try {super.close();} catch (Throwable e) {}
    }
    
}
