/**
 *  Broker
 *  Copyright 15.01.2017 by Michael Peter Christen, @0rb1t3r
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

import java.io.Closeable;
import java.io.IOException;

public interface Broker<A> extends Closeable {

    /**
     * send a message to the broker
     * @param serviceName the name of the grid service
     * @param queueName the queue name of the service
     * @param message the message to be posted at the broker
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> send(String serviceName, String queueName, byte[] message) throws IOException;
    
    /**
     * receive a message from the broker. This method blocks until a message is available
     * @param serviceName the name of the grid service
     * @param queueName the queue name of the service
     * @param timeout the maximum time to wait for a message. if zero or negative, the method blocks forever or until a message arrives
     * @return the message inside a message container
     * @throws IOException
     */
    public MessageContainer<A> receive(String serviceName, String queueName, long timeout) throws IOException;

    /**
     * count the number of available messages on the broker
     * @param serviceName the name of the grid service
     * @param queueName the queue name of the service
     * @return the number of pending messages which can be received inside a container
     * @throws IOException
     */
    public AvailableContainer available(String serviceName, String queueName) throws IOException;
    
}
