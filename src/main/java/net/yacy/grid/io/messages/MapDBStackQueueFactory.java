/**
 *  MapDBStackQueueFactory
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.yacy.grid.io.db.MapDBSortedMap;
import net.yacy.grid.io.db.MapStack;
import net.yacy.grid.io.db.Stack;
import net.yacy.grid.tools.Logger;

/**
 * Factory for a queue using a stack
 */
public class MapDBStackQueueFactory implements QueueFactory {

    private final File location;
    private final Map<String, StackQueue> queues;

    /**
     * initialize a stack factory based on a file stack
     * @param storageLocationPath the path where the stacks shall be stored
     */
    public MapDBStackQueueFactory(final File storageLocationPath) {
        this.location = storageLocationPath;
        this.location.mkdirs();
        this.queues = new ConcurrentHashMap<>();
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public boolean hasDefaultPort() {
        return false;
    }

    @Override
    public int getPort() {
        return 1;
    }

    @Override
    public String getConnectionURL() {
        return null;
    }

    /**
     * Get the connection to a queue. The connection is either established initially
     * or created as a new connection. If the queue did not exist, it will exist automatically
     * after calling the method
     * @param queueName
     * @return the Queue
     * @throws IOException
     */
    @Override
    public Queue getQueue(final String queueName) throws IOException {
        StackQueue queue = this.queues.get(queueName);
        if (queue != null) return queue;
        synchronized (this) {
            queue = this.queues.get(queueName);
            if (queue != null) return queue;
            queue = new StackQueue(new MapStack<byte[]>(new MapDBSortedMap(new File(this.location, queueName))));
            this.queues.put(queueName, queue);
            return queue;
        }
    }

    /**
     * Close the Factory
     */
    @Override
    public void close() {
        this.queues.values().forEach(queue -> queue.close());
    }

    public class StackQueue extends AbstractQueue implements Queue {

        private final Stack<byte[]> stack;
        private final Semaphore semaphore;

        public StackQueue(final Stack<byte[]> backedStack) throws IOException {
            this.stack = backedStack;
            this.semaphore = new Semaphore(this.stack.size(), true);
        }

        @Override
        public void checkConnection() throws IOException {
            available();
        }

        @Override
        public Queue send(final byte[] message) throws IOException {
            this.stack.push(message);
            this.semaphore.release();
            return this;
        }

        @Override
        public MessageContainer receive(final long timeout, final boolean autoAck) throws IOException {
            try {
                if (timeout > 0) {
                    if (!this.semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS)) return null;
                } else {
                    this.semaphore.acquire();
                }
                return new MessageContainer(MapDBStackQueueFactory.this, this.stack.pot(), 0);
            } catch (final InterruptedException e) {
                Logger.debug(this.getClass(), "StackQueue: receive interrupted", e);
            }
            return null;
        }

        @Override
        public void acknowledge(final long deliveryTag) throws IOException {
            // do nothing, this class does not provide a message acknowledge function
        }

        @Override
        public void reject(final long deliveryTag) throws IOException {
            // do nothing, this class does not provide a message reject function
        }

        @Override
        public void recover() throws IOException {
            // do nothing, this class does not provide a message acknowledge function
        }

        @Override
        public long available() throws IOException {
            return this.semaphore.availablePermits();
        }

        @Override
        public void close() {
            try {
                this.stack.close();
            } catch (final IOException e) {
                Logger.debug(this.getClass(), "StackQueue: close error", e);
            }
        }
    }

}
