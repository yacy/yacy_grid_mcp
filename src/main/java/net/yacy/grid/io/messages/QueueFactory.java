/**
 *  QueueConnector
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
 * A factory for a queue.
 */
public interface QueueFactory {

    /**
     * if the queue is defined using a service, this method provides the service address or name
     * @return the server name or IP or null, if none is defined or used
     */
    public String getHost();

    /**
     * the remote service runs on a specific port. If that port is the default port this method returns true
     * @return true if the remote service uses the default port or also true if none is used
     */
    public boolean hasDefaultPort();

    /**
     * the remote service runs on a specific port.
     * @return the remote service port
     */
    public int getPort();


    /**
     * the protocol, host name and port of a QueueFactory can be addressed in a single url stub
     * @return the connection url stub
     */
    public String getConnectionURL();

    /**
     * Get the connection to a queue. The connection is either established initially
     * or created as a new connection. If the queue did not exist, it will exist automatically
     * after calling the method
     * @param queueName name of the queue
     * @return the Queue
     * @throws IOException
     */
    public Queue getQueue(String queueName) throws IOException;

    /**
     * Close the Factory
     */
    public void close();

}
