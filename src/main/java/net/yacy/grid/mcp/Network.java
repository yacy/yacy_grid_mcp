/**
 *  Network
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

package net.yacy.grid.mcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.http.APIServer;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.api.info.ServicesService;

/**
 * grid management class which provides connection to the MCP
 */
public class Network {

    private final Set<String> mcpActiveAddresses = new LinkedHashSet<>();
    private final Set<String> mcpPassiveAddresses = new LinkedHashSet<>();
    
    private Map<String, Peer> peers;
    
    public Network() {
        this.peers = new ConcurrentHashMap<>();
    }
    
    public Network addMCP(String hostprotocolstub) {
        mcpActiveAddresses.add(hostprotocolstub);
        return this;
    }
    
    public Network removeMCP(String hostprotocolstub) {
        if (mcpActiveAddresses.remove(hostprotocolstub)) mcpPassiveAddresses.add(hostprotocolstub);
        return this;
    }
    
    public Network removeMCP(Set<String> stubs) {
       stubs.forEach(host -> removeMCP(host));
       return this;
    }
    
    @SuppressWarnings("unchecked")
    public List<Peer> getMCPServiceList() {
        Set<String> failstubs = new HashSet<>();
        JSONArray ja = null;
        loop: for (Set<String> a: new Set[]{mcpActiveAddresses, mcpPassiveAddresses}) {
            for (String hostprotocolstub: a) {
                try {
                    ServiceResponse response = APIServer.getAPI(ServicesService.NAME).serviceImpl(hostprotocolstub, new JSONObject());
                    ja = response.getArray();
                    break loop;
                } catch (IOException e) {
                    failstubs.add(hostprotocolstub);
                }
            }
        }
        removeMCP(failstubs);
        List<Peer> list = new ArrayList<>();
        if (ja == null) return list;
        ja.forEach(j -> list.add(new Peer((JSONObject) j)));
        return list;
    }
    
}
