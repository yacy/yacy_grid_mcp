/**
 *  MCPStorageFactory
 *  Copyright 28.1.2017 by Michael Peter Christen, @orbiterlab
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
import java.util.Random;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Configuration;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.tools.Logger;

public class MCPStorageFactory implements StorageFactory<byte[]> {

    private final GridStorage storage;
    private final String server;
    private final int port;
    private String remoteSystem;
    private final boolean active;

    public MCPStorageFactory(final GridStorage storage, final String server, final int port, final boolean active) {
        this.storage = storage;
        this.server = server;
        this.port = port;
        this.remoteSystem = "?";
        this.active = active;
    }

    @Override
    public String getSystem() {
        return "mcp" + "/" + this.remoteSystem;
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
                final String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                final ServiceResponse sr = Service.instance.config.getAPI(StatusService.NAME).serviceImpl(protocolhostportstub, params);
                if (!sr.getObject().has("status")) throw new IOException("MCP does not respond properly");
            }

            @Override
            public StorageFactory<byte[]> store(final String path, final byte[] asset) throws IOException {
                final Map<String, byte[]> params = new HashMap<>();
                params.put("path", path.getBytes(StandardCharsets.UTF_8));
                params.put("asset", asset);
                final String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                final ServiceResponse sr = Service.instance.config.getAPI(StoreService.NAME).serviceImpl(protocolhostportstub, params);
                final JSONObject response = sr.getObject();
                if (response.has(ObjectAPIHandler.SUCCESS_KEY) && response.getBoolean(ObjectAPIHandler.SUCCESS_KEY)) {
                    connectMCP(response);
                    return MCPStorageFactory.this;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public Asset<byte[]> load(final String path) throws IOException {
                final Map<String, byte[]> params = new HashMap<>();
                params.put("path", path.getBytes(StandardCharsets.UTF_8));

                final String protocolhostportstub = MCPStorageFactory.this.getConnectionURL();
                final ServiceResponse sr = Service.instance.config.getAPI(LoadService.NAME).serviceImpl(protocolhostportstub, params);
                return new Asset<byte[]>(MCPStorageFactory.this, sr.getByteArray());
            }

            @Override
            public void close() {
            }

            private void connectMCP(final JSONObject response) {
                if (response.has(ObjectAPIHandler.SERVICE_KEY)) {
                    final String server = response.getString(ObjectAPIHandler.SERVICE_KEY);
                    final int p = server.indexOf("://");
                    if (p > 0) MCPStorageFactory.this.remoteSystem = server.substring(0, p);
                    if (MCPStorageFactory.this.storage != null) {
                        if (MCPStorageFactory.this.storage.connectS3(server, MCPStorageFactory.this.active)) {
                            Logger.info(this.getClass(), "MCPStorageFactory.connectMCP connected S3 storage at " + server);
                        } else if (MCPStorageFactory.this.storage.connectFTP(server, MCPStorageFactory.this.active)) {
                            Logger.info(this.getClass(), "MCPStorageFactory.connectMCP connected FTP storage at " + server);
                        } else {
                            Logger.error(this.getClass(), "MCPStorageFactory.connectMCP failed to connect MCP storage at " + server);
                        }
                    }
                }
            }

            private IOException handleError(final JSONObject response) {
                if (response.has(ObjectAPIHandler.COMMENT_KEY)) {
                    return new IOException("cannot connect to MCP: " + response.getString(ObjectAPIHandler.COMMENT_KEY));
                }
                return new IOException("bad response from MCP: no success and no comment key");
            }

        };
    }

    @Override
    public void close() {
        // this is stateless, do nothing
    }

    public static void main(final String args[]) {
        // burn-in test
        final Configuration data = new Configuration("data", true, YaCyServices.mcp);
        final int threads = 16;
        final MCPStorageFactory storage = new MCPStorageFactory(null, "127.0.0.1", 8100, true);
        final Random random = new Random(System.currentTimeMillis());
        final Thread[] u = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            u[t] = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        final byte[] asset = new byte[1000 + random.nextInt(50000)];
                        random.nextBytes(asset);
                        final String path = "test/" + Math.abs(random.nextLong());
                        try {
                            final long x0 = System.currentTimeMillis();
                            storage.getStorage().store(path, asset);
                            final long x1 = System.currentTimeMillis();
                            System.out.println("stored " + asset.length + " bytes to asset " + path + " in " + (x1 - x0) + " milliseconds");
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            u[t].start();
        }
        for (final Thread t: u)
            try {
                t.join();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        data.close();
    }

}
