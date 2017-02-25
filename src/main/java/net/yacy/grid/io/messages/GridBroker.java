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

import net.yacy.grid.mcp.Data;

public class GridBroker extends PeerBroker implements Broker<byte[]> {

    private QueueFactory<byte[]> rabbitConnector;
    private QueueFactory<byte[]> mcpConnector;

    public GridBroker(File basePath) {
        super(basePath);
        this.rabbitConnector = null;
        this.mcpConnector = null;
    }
    
    public static String serviceQueueName(String serviceName, String queueName) {
        return serviceName + '_' + queueName;
    }

    public boolean connectRabbitMQ(String protocolhoststub) {
        if (!protocolhoststub.startsWith(RabbitQueueFactory.PROTOCOL_PREFIX)) return false;
        protocolhoststub = protocolhoststub.substring(RabbitQueueFactory.PROTOCOL_PREFIX.length());
        int p = protocolhoststub.indexOf(':');
        if (p < 0)
            return connectRabbitMQ(protocolhoststub, -1);
        else 
            return connectRabbitMQ(protocolhoststub.substring(0,  p), Integer.parseInt(protocolhoststub.substring(p + 1)));
    }
    
    public boolean connectRabbitMQ(String host, int port) {
        try {
            QueueFactory<byte[]> qc = new RabbitQueueFactory(host, port, null, null);
            this.rabbitConnector = qc;
            return true;
        } catch (IOException e) {
            Data.logger.debug("trying to connect to the rabbitMQ broker at " + host + ":" + port + " failed");
            return false;
        }
    }
    
    public boolean isRabbitMQConnected() {
        return this.rabbitConnector != null;
    }

    public boolean connectMCP(String host, int port) {
        try {
            QueueFactory<byte[]> mcpqf = new MCPQueueFactory(this, host, port);
            mcpqf.getQueue("test_test").checkConnection();
            this.mcpConnector = mcpqf;
            return true;
        } catch (IOException e) {
            Data.logger.debug("trying to connect to a Queue over MCP at " + host + ":" + port + " failed");
            return false;
        }
    }

    @Override
    public QueueFactory<byte[]> send(String serviceName, String queueName, byte[] message) throws IOException {
        if (this.rabbitConnector != null) try {
            this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            return this.rabbitConnector;
        } catch (IOException e) {
            Data.logger.debug("rabbitmq fail", e);
        }
        if (this.mcpConnector != null) try {
            this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            return this.mcpConnector;
        } catch (IOException e) {
            Data.logger.debug("mcp fail", e);
        }
        return super.send(serviceName, queueName, message);
    }

    @Override
    public MessageContainer<byte[]> receive(String serviceName, String queueName, long timeout) throws IOException {
        if (this.rabbitConnector != null) try {
            return new MessageContainer<byte[]>(this.rabbitConnector, this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).receive(timeout));
        } catch (IOException e) {}
        if (this.mcpConnector != null) try {
            return new MessageContainer<byte[]>(this.mcpConnector, this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).receive(timeout));
        } catch (IOException e) {}
        return super.receive(serviceName, queueName, timeout);
    }

    @Override
    public AvailableContainer available(String serviceName, String queueName) throws IOException {
        if (this.rabbitConnector != null) try {
            return new AvailableContainer(this.rabbitConnector, this.rabbitConnector.getQueue(serviceQueueName(serviceName, queueName)).available());
        } catch (IOException e) {}
        if (this.mcpConnector != null) try {
            return new AvailableContainer(this.mcpConnector, this.mcpConnector.getQueue(serviceQueueName(serviceName, queueName)).available());
        } catch (IOException e) {}
        return super.available(serviceName, queueName);
    }
    
    public void close() {
        if (this.rabbitConnector != null) this.rabbitConnector.close();
        if (this.mcpConnector != null) this.mcpConnector.close();
        super.close();
    }
    
}
