/**
 *  RabbitQueueFactory
 *  Copyright 14.01.2017 by Michael Peter Christen, @orbiterlab
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

import net.yacy.grid.tools.Logger;

/**
 * to monitor the rabbitMQ queue, open the admin console at
 * http://127.0.0.1:15672/
 * and log with admin/admin
 */
public class RabbitQueueFactory implements QueueFactory<byte[]> {

    private static int DEFAULT_PORT = 5672;
    private static String DEFAULT_EXCHANGE = "";
    public static String PROTOCOL_PREFIX = "amqp://";


    private final String server, username, password;
    private final int port;
    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private Map<String, Queue<byte[]>> queues;
    private final AtomicBoolean lazy;
    private final AtomicInteger queueLimit;

    /**
     * create a queue factory for a rabbitMQ message server
     * @param server the host name of the rabbitMQ server
     * @param port a port for the access to the rabbitMQ server. If given -1, then the default port will be used
     * @param username
     * @param password
     * @param lazy
     * @param queueLimit maximum number of entries for the queue, 0 = unlimited
     * @throws IOException
     */
    public RabbitQueueFactory(final String server, final int port, final String username, final String password, final boolean lazy, final int queueLimit) throws IOException {
        this.server = server;
        this.port = port;
        this.username = username;
        this.password = password;
        this.lazy = new AtomicBoolean(lazy);
        this.queueLimit = new AtomicInteger(queueLimit);
        this.connection = null;
        this.queues = new ConcurrentHashMap<>();
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setAutomaticRecoveryEnabled(false); // false -> SIC! - when leaving this 'true', old connections will be reused even if a old connection is closed and replace with a new one, resulting is "already closed exception".
        this.connectionFactory.setHost(this.server);
        if (this.port > 0) this.connectionFactory.setPort(this.port);
        if (this.username != null && this.username.length() > 0) this.connectionFactory.setUsername(this.username);
        if (this.password != null && this.password.length() > 0) this.connectionFactory.setPassword(this.password);
    }

    private Connection getConnection() throws IOException {
        if (this.connection != null && this.connection.isOpen()) return this.connection;
        try {
            this.connection = this.connectionFactory.newConnection();
            //Map<String, Object> map = this.connection.getServerProperties();
            if (!this.connection.isOpen()) throw new IOException("no connection");
            return this.connection;
        } catch (final TimeoutException e) {
            throw new IOException(e.getMessage());
        }
    }

    private Channel getChannel() throws IOException {
        getConnection();
        final Channel channel = this.connection.createChannel();
        if (!channel.isOpen()) throw new IOException("no channel");
        return channel;
    }

    @Override
    public String getConnectionURL() {
        return PROTOCOL_PREFIX +
               (this.username != null && this.username.length() > 0 ? this.username + (this.password != null && this.password.length() > 0 ? ":" + this.password : "") + "@" : "") +
               this.getHost() + ((this.hasDefaultPort() ? "" : ":" + this.getPort()));
    }

    @Override
    public String getHost() {
        return this.server;
    }

    @Override
    public boolean hasDefaultPort() {
        return this.port == -1 || this.port == DEFAULT_PORT;
    }

    @Override
    public int getPort() {
        return hasDefaultPort() ? DEFAULT_PORT : this.port;
    }

    @Override
    public Queue<byte[]> getQueue(final String queueName) throws IOException {
        if (this.queues == null) return null;
        Queue<byte[]> queue = this.queues.get(queueName);
        if (queue != null) return queue;
        synchronized (this) {
            queue = this.queues.get(queueName);
            if (queue != null) return queue;
            queue = new RabbitMessageQueue(queueName);
            this.queues.put(queueName, queue);
            return queue;
        }
    }

    private class RabbitMessageQueue extends AbstractQueue<byte[]> implements Queue<byte[]> {
        private final String queueName;
        private final SortedMap<Long, BlockingQueue<Boolean>> unconfirmedSet;
        private Channel channel;
        public RabbitMessageQueue(final String queueName) throws IOException {
            this.queueName = queueName;
            this.unconfirmedSet = Collections.synchronizedSortedMap(new TreeMap<>());
            connect();
        }

        private void connect() throws IOException {
            final Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-queue-mode", RabbitQueueFactory.this.lazy.get() ? "lazy" : "default"); // we want to minimize memory usage; see http://www.rabbitmq.com/lazy-queues.html
            if (RabbitQueueFactory.this.queueLimit.get() > 0) {
                arguments.put("x-max-length", RabbitQueueFactory.this.queueLimit.get());
                arguments.put("x-overflow", "reject-publish");
            }
            this.channel = RabbitQueueFactory.this.getChannel();
            try {
                this.channel.queueDeclare(this.queueName, true, false, false, arguments);
            } catch (final Throwable e) {
                // we first try to delete the old queue, but only if it is not used and if empty
                try {
                    this.channel = RabbitQueueFactory.this.connection.createChannel();
                    this.channel.queueDelete(this.queueName, true, true);
                } catch (final Throwable ee) {}

                // try again
                try {
                    this.channel = RabbitQueueFactory.this.connection.createChannel();
                    this.channel.queueDeclare(this.queueName, true, false, false, arguments);
                } catch (final Throwable ee) {
                    // that did not work. Try to modify the call to match with the previous queueDeclare
                    final String ec = ee.getCause() == null ? ee.getMessage() : ee.getCause().getMessage();
                    if (ec != null && ec.contains("'signedint' but current is none")) {
                        arguments.remove("x-max-length");
                        arguments.remove("x-overflow");
                    }
                    //arguments.put("x-queue-mode", lazy.get() ? "default" : "lazy");
                    try {
                        this.channel = RabbitQueueFactory.this.connection.createChannel();
                        this.channel.queueDeclare(this.queueName, true, false, false, arguments);
                    } catch (final Throwable eee) {
                        throw new IOException(eee.getMessage());
                    }
                }
            }
            this.channel.confirmSelect(); // declare that the channel sends confirmations
            this.channel.addConfirmListener(
                new ConfirmCallback() { // ack
                    @Override
                    public void handle(final long seqNo, final boolean multiple) throws IOException {
                        if (multiple) {
                            final Map<Long, BlockingQueue<Boolean>> m = RabbitMessageQueue.this.unconfirmedSet.headMap(seqNo + 1);
                            m.forEach((s, b) -> b.add(Boolean.TRUE));
                            m.clear();
                        } else {
                            final BlockingQueue<Boolean> b = RabbitMessageQueue.this.unconfirmedSet.remove(seqNo);
                            assert b != null;
                            if (b != null) b.add(Boolean.TRUE);
                        }
                    }},
                new ConfirmCallback() { // nack
                    @Override
                    public void handle(final long seqNo, final boolean multiple) throws IOException {
                        if (multiple) {
                            final Map<Long, BlockingQueue<Boolean>> m = RabbitMessageQueue.this.unconfirmedSet.headMap(seqNo + 1);
                            m.forEach((s, b) -> b.add(Boolean.FALSE));
                            m.clear();
                        } else {
                            final BlockingQueue<Boolean> b = RabbitMessageQueue.this.unconfirmedSet.remove(seqNo);
                            assert b != null;
                            if (b != null) b.add(Boolean.FALSE);
                        }
                    }}
            );
        }

        @Override
        public void checkConnection() throws IOException {
            available();
        }

        @Override
        public Queue<byte[]> send(final byte[] message) throws IOException {
            try {
                return sendInternal(message);
            } catch (final IOException e) {
                if (e.getMessage().equals(GridBroker.TARGET_LIMIT_MESSAGE)) throw e;
                // try again
                Logger.warn(this.getClass(), "RabbitQueueFactory.send: re-connecting broker");
                connect() ;
                return sendInternal(message);
            }
        }
        private Queue<byte[]> sendInternal(final byte[] message) throws IOException {
            final BlockingQueue<Boolean> semaphore = new ArrayBlockingQueue<>(1);
            final long seqNo = this.channel.getNextPublishSeqNo();
            this.unconfirmedSet.put(seqNo, semaphore);
            this.channel.basicPublish(DEFAULT_EXCHANGE, this.queueName, MessageProperties.PERSISTENT_BASIC, message);
            // wait for confirmation
            try {
                final Boolean delivered = semaphore.poll(10, TimeUnit.SECONDS);
                if (delivered == null) throw new IOException("message sending timeout");
                if (delivered) return this;
                throw new IOException(GridBroker.TARGET_LIMIT_MESSAGE);
            } catch (final InterruptedException x) {
                this.unconfirmedSet.remove(seqNo); // prevent a memory leak
                throw new IOException("message sending interrupted");
            }
        }

        @Override
        public MessageContainer<byte[]> receive(long timeout, final boolean autoAck) throws IOException {
            if (timeout <= 0) timeout = Long.MAX_VALUE;
            final long termination = timeout <= 0 || timeout == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + timeout;
            Throwable ee = null;
            while (System.currentTimeMillis() < termination) {
                ee = null;
                try {
                    final GetResponse response = this.channel.basicGet(this.queueName, autoAck);
                    if (response != null) {
                        final Envelope envelope = response.getEnvelope();
                        final long deliveryTag = envelope.getDeliveryTag();
                        //channel.basicAck(deliveryTag, false);
                        return new MessageContainer<byte[]>(RabbitQueueFactory.this, response.getBody(), deliveryTag);
                    }
                    //Logger.warn(this.getClass(), "receive failed: response empty");
                } catch (final Throwable e) {
                    Logger.warn(this.getClass(), "receive failed: " + e.getMessage(), e);
                    connect() ;
                    ee = e;
                    //autoAck = ! autoAck;
                }
                try {Thread.sleep(1000);} catch (final InterruptedException e) {return null;}
            }
            if (ee == null) return null;
            throw new IOException(ee.getMessage());
        }

        @Override
        public void acknowledge(final long deliveryTag) throws IOException {
            try {
                this.channel.basicAck(deliveryTag, false);
            } catch (IOException | AlreadyClosedException e) {
                // try again
                Logger.warn(this.getClass(), "RabbitQueueFactory.acknowledge: re-connecting broker");
                connect() ;
                this.channel.basicAck(deliveryTag, false);
            }
        }

        @Override
        public void reject(final long deliveryTag) throws IOException {
            try {
                this.channel.basicReject(deliveryTag, true);
            } catch (IOException | AlreadyClosedException e) {
                // try again
                Logger.warn(this.getClass(), "RabbitQueueFactory.reject: re-connecting broker");
                connect() ;
                this.channel.basicReject(deliveryTag, false);
            }
        }

        @Override
        public void recover() throws IOException {
            try {
                this.channel.basicRecover(true);
            } catch (IOException | AlreadyClosedException e) {
                // try again
                Logger.warn(this.getClass(), "RabbitQueueFactory.recover: re-connecting broker");
                connect() ;
                this.channel.basicRecover(true);
            }
        }

        @Override
        public long available() throws IOException {
            try {
                return availableInternal();
            } catch (IOException | AlreadyClosedException e) {
                // try again
                Logger.warn(this.getClass(), "RabbitQueueFactory.available: re-connecting broker");
                connect() ;
                return availableInternal();
            }
        }
        private int availableInternal() throws IOException {
            //int a = channel.queueDeclarePassive(this.queueName).getMessageCount();
            final int b = (int) this.channel.messageCount(this.queueName);
            //assert a == b;
            return b;
        }

        @Override
        public void close() throws IOException {
            if (this.channel != null) try {
                this.channel.close();
            } catch (IOException | TimeoutException e) {
            }
        }
    }

    @Override
    public void close() {
        //for (final Queue<byte[]> q: this.queues.values()) {try {q.close();} catch (final IOException e) {}}
        //
        // ATTENTION:
        // We do not close the queues because RabbitMQ tries to recover old queues even if a new connection is initiated.
        // Am exception will occur in case that we make a new connection and use old closed queues, stating that the queue was already closed.
        // There must be a static status inside the RabbitMQ client which remembers all queues, even if they have been closed.
        //
        try {this.connection.close();} catch (final IOException e) {}
        this.queues.clear();
        this.queues = null;
        this.connection = null;
    }

    public static void main(final String[] args) {
        RabbitQueueFactory qc;
        try {
            qc = new RabbitQueueFactory("127.0.0.1", -1, "guest", "guest", true, 0);
            qc.getQueue("test").send("Hello World".getBytes());
            System.out.println(new String(qc.getQueue("test2").receive(60000, true).getPayload()));
            qc.close();
        } catch (final IOException e) {
            Logger.warn(e);
        }
    }
}
