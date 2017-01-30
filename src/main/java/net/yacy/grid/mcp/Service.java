/**
 *  Service
 *  Copyright 16.01.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.mcp;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The services enum contains information about all services implemented in this
 * grid element. It is the name of the servlets and their result object set.
 * This enum class should be replaced with a Swagger definition.
 */
public enum Service {

    Services(new String[]{
            "network",     // the name of the network
            "service",     // the YaCy Grid type of the service
            "name",        // an identifier which the owner of the service can assign as they want
            "host",        // host-name or IP address of the service
            "port",        // port on on the server where the service is running
            "publichost",  // host name which the service names as public access point, may be different from the recognized host access (backward routing may be different)
            "lastseen",    // ISO 8601 Time of the latest contact of the mcp to the service. Empty if the service has never been seen.
            "lastping"     // ISO 8601 Time of the latest contact of the service to the mcp
    });
    
    private Set<String> fields;
    
    Service(String[] fields) {
        this.fields = new LinkedHashSet<String>();
        for (String field: fields) this.fields.add(field);
    }

}
