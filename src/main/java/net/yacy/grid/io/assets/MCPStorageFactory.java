/**
 *  MCPStorageFactory
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.http.JSONAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.info.StatusService;

public class MCPStorageFactory implements StorageFactory<byte[]> {

    private GridStorage storage;
    private String server;
    private int port;
    
    public MCPStorageFactory(GridStorage storage, String server, int port) {
        this.storage = storage;
        this.server = server;
        this.port = port;
    }
    
    @Override
    public String getHost() {
        return this.server;
    }

    @Override
    public boolean hasDefaultPort() {
        return this.port == -1 || this.port == YaCyServices.mcp.getDefaultPort();
    }

    @Override
    public int getPort() {
        return hasDefaultPort() ? YaCyServices.mcp.getDefaultPort() : this.port;
    }

    @Override
    public String getConnectionURL() {
        return "http://" + this.getHost() + ":" + ((this.hasDefaultPort() ? YaCyServices.mcp.getDefaultPort() : this.getPort()));
    }

    @Override
    public Storage<byte[]> getStorage() throws IOException {
        return new Storage<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                final Map<String, byte[]> params = new HashMap<>();
                String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                ServiceResponse sr = APIServer.getAPI(StatusService.NAME).serviceImpl(protocolhostportstub, params);
                if (!sr.getObject().has("system")) throw new IOException("MCP does not respond properly");
            }
            
            @Override
            public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
                final Map<String, byte[]> params = new HashMap<>();
                params.put("path", path.getBytes(StandardCharsets.UTF_8));
                params.put("asset", asset);
                String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                ServiceResponse sr = APIServer.getAPI(StoreService.NAME).serviceImpl(protocolhostportstub, params);
                JSONObject response = sr.getObject();
                if (response.has(JSONAPIHandler.SUCCESS_KEY) && response.getBoolean(JSONAPIHandler.SUCCESS_KEY)) {
                    connectMCP(response);
                    return MCPStorageFactory.this;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public Asset<byte[]> load(String path) throws IOException {
                final Map<String, byte[]> params = new HashMap<>();
                params.put("path", path.getBytes(StandardCharsets.UTF_8));
                
                String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                ServiceResponse sr = APIServer.getAPI(LoadService.NAME).serviceImpl(protocolhostportstub, params);
                return new Asset<byte[]>(MCPStorageFactory.this, sr.getByteArray());
            }

            @Override
            public void close() {
            }
            
            private void connectMCP(JSONObject response) {
                if (response.has(JSONAPIHandler.SERVICE_KEY)) {
                    String server = response.getString(JSONAPIHandler.SERVICE_KEY);
                    if (MCPStorageFactory.this.storage.connectFTP(server)) {
                        Data.logger.info("connected MCP storage at " + server);
                    } else {
                        Data.logger.error("failed to connect MCP storage at " + server);
                    }
                }
            }
            
            private IOException handleError(JSONObject response) {
                if (response.has(JSONAPIHandler.COMMENT_KEY)) {
                    return new IOException("cannot connect to MCP: " + response.getString(JSONAPIHandler.COMMENT_KEY));
                }
                return new IOException("bad response from MCP: no success and no comment key");
            }
            
        };
    }

    @Override
    public void close() {
        // this is stateless, do nothing
    }

}
