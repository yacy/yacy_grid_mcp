/**
 *  StorageFactory
 *  Copyright 28.1.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.assets;

import java.io.IOException;

public interface StorageFactory<A> {

    /**
     * if the storage is defined using a service, this method provides the service address or name
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
     * the protocol, host name and port of a StorageFactory can be addressed in a single url stub
     * @return the connection url stub
     */
    public String getConnectionURL();
    
    /**
     * Get the connection to a storage location. The connection is either established initially
     * or created as a new connection. If the stsorage location did not exist, it will exist automatically
     * after calling the method
     * @return the Storage
     * @throws IOException
     */
    public Storage<A> getStorage() throws IOException;
    
    /**
     * Close the Storage
     */
    public void close();
}
