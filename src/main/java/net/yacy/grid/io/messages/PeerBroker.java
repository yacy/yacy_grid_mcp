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

import net.yacy.grid.QueueName;
import net.yacy.grid.Services;

public class PeerBroker extends AbstractBroker<byte[]> implements Broker<byte[]> {

    private File basePath;
    private Map<Services, QueueFactory<byte[]>> clientConnector;
    
    public PeerBroker(File basePath) {
        this.basePath = basePath;
        this.clientConnector = new ConcurrentHashMap<>();
    }
    
    private QueueFactory<byte[]> getConnector(Services service) {
        QueueFactory<byte[]> c = this.clientConnector.get(service);
        if (c != null)  return c;
        synchronized (this) {
            c = this.clientConnector.get(service);
            if (c != null)  return c;
            File clientPath = new File(this.basePath, service.name());
            clientPath.mkdirs();
            c = new MapDBStackQueueFactory(clientPath);
            this.clientConnector.put(service, c);
        }
        return c;
    }

    @Override
    public QueueFactory<byte[]> send(Services service, QueueName queueName, byte[] message) throws IOException {
        QueueFactory<byte[]> factory = getConnector(service);
        factory.getQueue(queueName.name()).send(message);
        return factory;
    }

    @Override
    public MessageContainer<byte[]> receive(Services service, QueueName queueName, long timeout) throws IOException {
        QueueFactory<byte[]> factory = getConnector(service);
        Queue<byte[]> mq = factory.getQueue(queueName.name());
        byte[] message = mq.receive(timeout);
        return new MessageContainer<byte[]>(factory, message == null ? null : message);
    }

    @Override
    public AvailableContainer available(Services service, QueueName queueName) throws IOException {
        QueueFactory<byte[]> factory = getConnector(service);
        return new AvailableContainer(factory, getConnector(service).getQueue(queueName.name()).available());
    }

    @Override
    public void close() {
        this.clientConnector.values().forEach(connector -> {
            try {connector.close();} catch (Throwable e) {}
        });
    }
    
}
