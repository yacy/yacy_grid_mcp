/**
 *  LocalBroker
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

public class PeerBroker implements Broker<byte[]> {

    private File basePath;
    private Map<String, QueueFactory<byte[]>> clientConnector;
    
    public PeerBroker(File basePath) {
        this.basePath = basePath;
        this.clientConnector = new ConcurrentHashMap<>();
    }
    
    private QueueFactory<byte[]> getConnector(String serviceName) {
        QueueFactory<byte[]> c = this.clientConnector.get(serviceName);
        if (c != null)  return c;
        synchronized (this) {
            c = this.clientConnector.get(serviceName);
            if (c != null)  return c;
            File clientPath = new File(this.basePath, serviceName);
            clientPath.mkdirs();
            c = new MapDBStackQueueFactory(clientPath);
            this.clientConnector.put(serviceName, c);
        }
        return c;
    }

    @Override
    public QueueFactory<byte[]> send(String serviceName, String queueName, byte[] message) throws IOException {
        QueueFactory<byte[]> factory = getConnector(serviceName);
        factory.getQueue(queueName).send(message);
        return factory;
    }

    @Override
    public MessageContainer<byte[]> receive(String serviceName, String queueName, long timeout) throws IOException {
        QueueFactory<byte[]> factory = getConnector(serviceName);
        byte[] message = factory.getQueue(queueName).receive(timeout);
        return new MessageContainer<byte[]>(factory, message == null ? null : message);
    }

    @Override
    public AvailableContainer available(String serviceName, String queueName) throws IOException {
        QueueFactory<byte[]> factory = getConnector(serviceName);
        return new AvailableContainer(factory, getConnector(serviceName).getQueue(queueName).available());
    }

    @Override
    public void close() {
        this.clientConnector.values().forEach(connector -> connector.close());
    }
    
}
