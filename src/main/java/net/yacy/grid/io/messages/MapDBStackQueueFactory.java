/**
 *  MapDBStackQueueFactory
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.yacy.grid.io.db.MapDBTreeMap;
import net.yacy.grid.io.db.MapStack;

/**
 * Factory for a queue using a stack
 */
public class MapDBStackQueueFactory implements QueueFactory<byte[]> {

    private File location;
    private Map<String, StackQueue<byte[]>> queues;
    
    /**
     * initialize a stack factory based on a file stack
     * @param storageLocationPath the path where the stacks shall be stored
     */
    public MapDBStackQueueFactory(File storageLocationPath) {
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
    public Queue<byte[]> getQueue(String queueName) throws IOException {
        StackQueue<byte[]> queue = queues.get(queueName);
        if (queue != null) return queue;
        synchronized (this) {
            queue = queues.get(queueName);
            if (queue != null) return queue;
            queue = new StackQueue<byte[]>(new MapStack<byte[]>(MapDBTreeMap.newLongMap(new File(this.location, queueName))));
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

    public static void main(String[] args) {
    	File location = new File("/tmp/queuetest");
    	location.mkdirs();
    	try {
			StackQueue<byte[]> queue = new StackQueue<byte[]>(new MapStack<byte[]>(MapDBTreeMap.newLongMap(new File(location, "testqueue"))));
			for (int i = 0; i < 10; i++) {
				queue.send(("x" + i).getBytes());
			}
			queue.close();
			queue = new StackQueue<byte[]>(new MapStack<byte[]>(MapDBTreeMap.newLongMap(new File(location, "testqueue"))));
			while (queue.available() > 0) {
				System.out.println(new String(queue.receive(1000)));
			}
			queue.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
}
