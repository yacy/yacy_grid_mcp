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

    public final static String TARGET_LIMIT_MESSAGE = "message not delivered - target limitation";
    private QueueFactory<byte[]> rabbitQueueFactory;
    private QueueFactory<byte[]> mcpQueueFactory;

    private String rabbitMQ_host, rabbitMQ_username, rabbitMQ_password;
    private int rabbitMQ_port;
    private String mcp_host;
    private int mcp_port;
    private boolean lazy;
    private boolean autoAck;
    private int queueLimit, queueThrottling;

    /**
     * Make a grid-based broker
     * @param lazy if true, support lazy queues in rabbitmq, see http://www.rabbitmq.com/lazy-queues.html
     * @param basePath the local storage path of an db-based queue. This can also be NULL if no local queue is wanted
     */
    public GridBroker(File basePath, boolean lazy, boolean autoAck, int queueLimit, int queueThrottling) {
        super(basePath);
        this.rabbitQueueFactory = null;
        this.mcpQueueFactory = null;
        this.rabbitMQ_host = null;
        this.rabbitMQ_port = -1;
        this.rabbitMQ_username = null;
        this.rabbitMQ_password = null;
        this.mcp_host = null;
        this.mcp_port = -1;
        this.lazy = lazy;
        this.autoAck = autoAck;
        this.queueLimit = queueLimit;
        this.queueThrottling = queueThrottling;
    }

    public boolean isAutoAck() {
        return this.autoAck;
    }

    public int getQueueLimit() {
        return this.queueLimit;
    }

    public int getQueueThrottling() {
        return this.queueThrottling;
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
            QueueFactory<byte[]> qc = new RabbitQueueFactory(host, port, username, password, this.lazy, this.queueLimit);
            this.rabbitQueueFactory = qc;
            Data.logger.info("Broker/Client: connected to the rabbitMQ broker at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            /*if (firsttry)*/ Data.logger.info("Broker/Client: trying to connect to the rabbitMQ broker at " + host + ":" + port + " failed: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean isRabbitMQConnected() {
        return this.rabbitQueueFactory != null;
    }

    public boolean connectMCP(String host, int port) {
        this.mcp_host = host;
        this.mcp_port = port;
        try {
            QueueFactory<byte[]> mcpqf = new MCPQueueFactory(this, host, port);
            String queueName = YaCyServices.indexer.name() + "_" + YaCyServices.indexer.getSourceQueues()[0].name();
            mcpqf.getQueue(queueName).checkConnection();
            this.mcpQueueFactory = mcpqf;
            Data.logger.info("Broker/Client: connected to a Queue over MCP at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            /*if (firsttry)*/ Data.logger.info("Broker/Client: trying to connect to a Queue over MCP at " + host + ":" + port + " failed: " + e.getMessage());
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
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            Data.logger.info("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(message));
            return this.rabbitQueueFactory;
        } catch (IOException e) {
            String m = e.getMessage();
            if (m == null) m = e.getCause().getMessage();
            try {Thread.sleep(1000);} catch (InterruptedException ee) {}
            if (m.equals(TARGET_LIMIT_MESSAGE)) {
                // queue limitation is like running against a wall: don't do this at all (if you know there is a wall)
                // at least: If you insist in queue limitation don't do this too aggressive;
                // I recommend to do not to limit queues; instead do throttling
                try {Thread.sleep(3000);} catch (InterruptedException ee) {}
                throw e; // consider this as fatal to trigger throttling (hope throttling is done on every send-location
            }
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).send(message);
            Data.logger.info("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(message));
            return this.mcpQueueFactory;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: send() on peer broker/local db");
        return super.send(serviceName, queueName, message);
    }

    @Override
    public MessageContainer<byte[]> receive(Services serviceName, GridQueue queueName, long timeout, boolean autoAck) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            Queue<byte[]> rabbitQueue = this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName));
            MessageContainer<byte[]> mc = rabbitQueue.receive(timeout, autoAck);
            if (mc != null && mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: received rabbitMQ service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
            return mc;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: receive rabbitMQ service '" + serviceName + "', queue '" + queueName + "',rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! receive mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            Queue<byte[]> mcpQueue = this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName));
            MessageContainer<byte[]> mc = mcpQueue.receive(timeout, autoAck);
            if (mc != null && mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: receive mcp service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
            return mc;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: receive mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: receive() on peer broker/local db");
        MessageContainer<byte[]> mc = super.receive(serviceName, queueName, timeout, autoAck);
        if (mc != null && mc.getPayload() != null && mc.getPayload().length > 0) Data.logger.info("Broker/Client: received peer broker/local db service '" + serviceName + "', queue '" + queueName + "', message:" + messagePP(mc.getPayload()));
        return mc;  
    }

    @Override
    public QueueFactory<byte[]> acknowledge(Services serviceName, GridQueue queueName, long deliveryTag) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).acknowledge(deliveryTag);
            Data.logger.info("Broker/Client: acknowledged rabbitMQ service '" + serviceName + "', queue '" + queueName + "', deliveryTag " + deliveryTag);
            return this.rabbitQueueFactory;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: acknowledge rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).acknowledge(deliveryTag);
            Data.logger.info("Broker/Client: acknowledged mcp service '" + serviceName + "', queue '" + queueName + "', deliveryTag " + deliveryTag);
            return this.mcpQueueFactory;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: acknowledge mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: acknowledge() on peer broker/local db");
        return super.acknowledge(serviceName, queueName, deliveryTag);
    }

    @Override
    public QueueFactory<byte[]> reject(Services serviceName, GridQueue queueName, long deliveryTag) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).reject(deliveryTag);
            Data.logger.info("Broker/Client: rejected rabbitMQ service '" + serviceName + "', queue '" + queueName + "', deliveryTag " + deliveryTag);
            return this.rabbitQueueFactory;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: acknowledge rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).reject(deliveryTag);
            Data.logger.info("Broker/Client: rejected mcp service '" + serviceName + "', queue '" + queueName + "', deliveryTag " + deliveryTag);
            return this.mcpQueueFactory;
        } catch (IOException e) {
            Data.logger.debug("Broker/Client: acknowledge mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: reject() on peer broker/local db");
        return super.reject(serviceName, queueName, deliveryTag);
    }

    @Override
    public QueueFactory<byte[]> recover(Services serviceName, GridQueue queueName) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).recover();
            Data.logger.info("Broker/Client: recovered rabbitMQ service '" + serviceName + "', queue '" + queueName + "'");
            return this.rabbitQueueFactory;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: recover rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! recover mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).recover();
            Data.logger.info("Broker/Client: recovered mcp service '" + serviceName + "', queue '" + queueName + "'");
            return this.mcpQueueFactory;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: recover mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: recover() on peer broker/local db");
        return super.recover(serviceName, queueName);
    }

    @Override
    public AvailableContainer available(Services serviceName, GridQueue queueName) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            AvailableContainer ac = new AvailableContainer(this.rabbitQueueFactory, queueName.name, this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).available());
            return ac;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: available rabbitMQ service '" + serviceName + "', queue '" + queueName + "',rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpQueueFactory == null) {
                Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! available mcp service '" + serviceName + "', queue '" + queueName);
            }
        }
        if (this.mcpQueueFactory != null) try {
            AvailableContainer ac = new AvailableContainer(this.mcpQueueFactory, queueName.name, this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).available());
            return ac;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: available mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: available() on peer broker/local db");
        return super.available(serviceName, queueName);
    }

    @Override
    public QueueFactory<byte[]> clear(Services serviceName, GridQueue queueName) throws IOException {
        if (this.rabbitQueueFactory == null && this.rabbitMQ_host != null) {
            // try to connect again..
            connectRabbitMQ(this.rabbitMQ_host, this.rabbitMQ_port, this.rabbitMQ_username, this.rabbitMQ_password);
        }
        if (this.rabbitQueueFactory == null) {
            this.rabbitMQ_host = null;
        } else try {
            this.rabbitQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).clear();
            Data.logger.info("Broker/Client: clear rabbitMQ service '" + serviceName + "', queue '" + queueName + "'");
            return this.rabbitQueueFactory;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send rabbitMQ service '" + serviceName + "', queue '" + queueName + "', rabbitmq fail", e);
        }
        if (this.mcpQueueFactory == null && this.mcp_host != null) {
                // try to connect again..
                connectMCP(this.mcp_host, this.mcp_port);
                if (this.mcpQueueFactory == null) {
                    Data.logger.warn("Broker/Client: FATAL: connection to MCP lost! send mcp service '" + serviceName + "', queue '" + queueName);
                }
        }
        if (this.mcpQueueFactory != null) try {
            this.mcpQueueFactory.getQueue(serviceQueueName(serviceName, queueName)).clear();
            Data.logger.info("Broker/Client: clear mcp service '" + serviceName + "', queue '" + queueName + "'");
            return this.mcpQueueFactory;
        } catch (IOException e) {
            /*if (!e.getMessage().contains("timeout"))*/ Data.logger.debug("Broker/Client: send mcp service '" + serviceName + "', queue '" + queueName + "',mcp fail", e);
        }
        Data.logger.info("Broker/Client: send() on peer broker/local db");
        return super.clear(serviceName, queueName);
    }

    public void close() {
        if (this.rabbitQueueFactory != null) try {this.rabbitQueueFactory.close();} catch (Throwable e) {}
        if (this.mcpQueueFactory != null) try {this.mcpQueueFactory.close();} catch (Throwable e) {}
        try {super.close();} catch (Throwable e) {}
    }

}
