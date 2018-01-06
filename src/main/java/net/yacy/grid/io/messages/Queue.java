/**
 *  Queue
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

import java.io.IOException;

/**
 * Interface for a Message Queue
 */
public interface Queue<A> {

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
    public Queue<A> send(A message) throws IOException;
    
    /**
     * receive a message from the queue. The method blocks until a message is available
     * @param timeout for blocking in milliseconds. if negative the method blocks forever
     * or until a message is submitted. 
     * @return the message or null if a timeout occurred
     * @throws IOException
     */
    public A receive(long timeout) throws IOException;
    
    /**
     * check how many messages are in the queue
     * @return the number of messages that can be loaded with receive()
     * @throws IOException
     */
    public int available() throws IOException;

    /**
     * clear a queue
     * @throws IOException
     */
    public void clear() throws IOException;
}
