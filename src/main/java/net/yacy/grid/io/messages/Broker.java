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

import net.yacy.grid.Services;

/**
 * Grid-Service Broker Interface for grid-wide messages.
 * 
 * This interface is implemented by
 * - the GridBroker: this class uses a locally connected RabbitMQ or an connection to another MCP
 * - the PeerBroker: this class implements a queue based on a local database in the peer.
 * 
 * @param <A> the object passed to and from the broker
 */
public interface Broker<A> extends Closeable {

    /**
     * send a message to the broker
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @param message the message to be posted at the broker
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> send(Services service, GridQueue queue, byte[] message) throws IOException;

    /**
     * send a message to the broker
     * @param service the name of the grid service
     * @param queues the queues of the service
     * @param shardingMethod the selected sharding method
     * @param priorityDimensions number of queues for each dimension; sum must be actual number of queue names
     * @param priority the wanted priority
     * @param hashingKey a hashing key for the message to be send
     * @param message the message to be posted at the broker
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> send(Services service, GridQueue[] queues, ShardingMethod shardingMethod, int[] priorityDimensions, int priority, String hashingKey, byte[] message) throws IOException;

    /**
     * get the best queue for a given sharding method, service, queues and a hashing key
     * @param service the name of the grid service
     * @param queues the queues of the service
     * @param shardingMethod the selected sharding method
     * @param priorityDimensions number of queues for each dimension; sum must be actual number of queue names
     * @param priority the wanted priority
     * @param hashingKey a hashing key for the message to be send
     * @return
     * @throws IOException
     */
    public GridQueue queueName(final Services service, final GridQueue[] queues, final ShardingMethod shardingMethod, int[] priorityDimensions, int priority, final String hashingKey) throws IOException;

    /**
     * receive a message from the broker. This method blocks until a message is available
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @param timeout the maximum time to wait for a message. if zero or negative, the method blocks forever or until a message arrives
     * @return the message inside a message container
     * @throws IOException
     */
    public MessageContainer<A> receive(Services service, GridQueue queue, long timeout, boolean autoAck) throws IOException;

    /**
     * acknowledge a message. This MUST be used to remove a message from the broker if
     * receive() was used with autoAck=false.
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @param deliveryTag the tag as reported by receive()
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> acknowledge(Services service, GridQueue queue, long deliveryTag) throws IOException;

    /**
     * Messages which had been received with autoAck=false but were not acknowledged with
     * the acknowledge() method are neither dequeued nor available for another receive.
     * They can only be accessed using a recover call; this moves all not-acknowledge messages
     * back to the queue to be available again for receive.
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> recover(Services service, GridQueue queue) throws IOException;

    /**
     * count the number of available messages on the broker
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @return the number of pending messages which can be received inside a container
     * @throws IOException
     */
    public AvailableContainer available(Services service, GridQueue queue) throws IOException;

    /**
     * count the number of available messages on the broker
     * @param service the name of the grid service
     * @param queues the queues of the service
     * @return an array of the number of pending messages which can be received inside a container
     * @throws IOException
     */
    public AvailableContainer[] available(Services service, GridQueue[] queues) throws IOException;

    /**
     * send a message to the broker
     * @param service the name of the grid service
     * @param queue the queue of the service
     * @param shardingMethod the selected sharding method
     * @param hashingKey a hashing key for the message to be send
     * @param message the message to be posted at the broker
     * @return the Queue Factory which was used to create this broker
     * @throws IOException
     */
    public QueueFactory<A> clear(Services service, GridQueue queue) throws IOException;

}
