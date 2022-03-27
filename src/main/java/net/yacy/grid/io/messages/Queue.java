/**
 *  Queue
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

/**
 * Interface for a Message Queue
 */
public interface Queue {

    /**
     * check the connection
     * @throws IOException in case that the connection is invalid
     */
    public void checkConnection() throws IOException;

    /**
     * send a message to the queue
     * @param message
     * @return the Queue
     * @throws IOException
     */
    public Queue send(byte[] message) throws IOException;

    /**
     * receive a message from the queue. The method blocks until a message is available
     * @param timeout for blocking in milliseconds. if negative the method blocks forever
     * or until a message is submitted.
     * @oaram autoAck if true the received message is autoAck'ed. If false, the message must be acknowledged to free up resources
     * @return the message or null if a timeout occurred
     * @throws IOException
     */
    public MessageContainer receive(long timeout, boolean autoAck) throws IOException;

    /**
     * acknowledge a message. This MUST be used to remove a message from the broker if
     * receive() was used with autoAck=false.
     * @param deliveryTag the tag as reported by receive()
     * @throws IOException
     */
    public void acknowledge(long deliveryTag) throws IOException;

    /**
     * reject a message. This MUST be used to return a message to the broker if
     * receive() was used with autoAck=false.
     * @param deliveryTag the tag as reported by receive()
     * @throws IOException
     */
    public void reject(long deliveryTag) throws IOException;

    /**
     * Messages which had been received with autoAck=false but were not acknowledged with
     * the acknowledge() method are neither dequeued nor available for another receive.
     * They can only be accessed using a recover call; this moves all not-acknowledge messages
     * back to the queue to be available again for receive.
     * @throws IOException
     */
    public void recover() throws IOException;

    /**
     * check how many messages are in the queue
     * @return the number of messages that can be loaded with receive()
     * @throws IOException
     */
    public long available() throws IOException;

    /**
     * clear a queue
     * @throws IOException
     */
    public void clear() throws IOException;

    /**
     * close a queue
     * @throws IOException
     */
    public void close() throws IOException;
}
