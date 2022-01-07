/**
 *  MCPQueueFactory
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

package net.yacy.grid.io.messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.mcp.api.messages.AcknowledgeService;
import net.yacy.grid.mcp.api.messages.AvailableService;
import net.yacy.grid.mcp.api.messages.ReceiveService;
import net.yacy.grid.mcp.api.messages.RecoverService;
import net.yacy.grid.mcp.api.messages.RejectService;
import net.yacy.grid.mcp.api.messages.SendService;
import net.yacy.grid.tools.Logger;

public class MCPQueueFactory implements QueueFactory<byte[]> {

    private GridBroker broker;
    private String server;
    private int port;

    public MCPQueueFactory(GridBroker broker, String server, int port) {
        this.broker = broker;
        this.server = server;
        this.port = port;
    }

    @Override
    public String getConnectionURL() {
        return "http://" + this.getHost() + ":" + ((this.hasDefaultPort() ? YaCyServices.mcp.getDefaultPort() : this.getPort()));
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
    public Queue<byte[]> getQueue(String serviceQueueName) throws IOException {
        final int p = serviceQueueName.indexOf('_');
        if (p <= 0) return null;
        final JSONObject params = new JSONObject(true);
        params.put("serviceName", serviceQueueName.substring(0, p));
        params.put("queueName", serviceQueueName.substring(p + 1));
        return new AbstractQueue<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                String protocolhostportstub = MCPQueueFactory.this.getConnectionURL();
                APIHandler apiHandler = APIServer.getAPI(StatusService.NAME);
                ServiceResponse sr = apiHandler.serviceImpl(protocolhostportstub, params);
                if (!sr.getObject().has("system")) throw new IOException("MCP does not respond properly");
                available(); // check on service level again
            }

            @Override
            public Queue<byte[]> send(byte[] message) throws IOException {
                params.put("message", new String(message, StandardCharsets.UTF_8));
                JSONObject response = getResponse(APIServer.getAPI(SendService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return this;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public MessageContainer<byte[]> receive(long timeout, boolean autoAck) throws IOException {
                params.put("timeout", Long.toString(timeout));
                params.put("autoAck", Boolean.toString(autoAck));
                JSONObject response = getResponse(APIServer.getAPI(ReceiveService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    if (response.has(ObjectAPIHandler.MESSAGE_KEY)) {
                        String message = response.getString(ObjectAPIHandler.MESSAGE_KEY);
                        long deliveryTag = response.optLong(ObjectAPIHandler.DELIVERY_TAG);
                        return new MessageContainer<byte[]>(MCPQueueFactory.this, message == null ? null : message.getBytes(StandardCharsets.UTF_8), deliveryTag);
                    }
                    throw new IOException("bad response from MCP: success but no message key");
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public void acknowledge(long deliveryTag) throws IOException {
                params.put("deliveryTag", Long.toString(deliveryTag));
                JSONObject response = getResponse(APIServer.getAPI(AcknowledgeService.NAME));
                if (success(response)) {
                    connectMCP(response);
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public void reject(long deliveryTag) throws IOException {
                params.put("deliveryTag", Long.toString(deliveryTag));
                JSONObject response = getResponse(APIServer.getAPI(RejectService.NAME));
                if (success(response)) {
                    connectMCP(response);
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public void recover() throws IOException {
                JSONObject response = getResponse(APIServer.getAPI(RecoverService.NAME));
                if (success(response)) {
                    connectMCP(response);
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public long available() throws IOException {
                JSONObject response = getResponse(APIServer.getAPI(AvailableService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    if (response.has(ObjectAPIHandler.AVAILABLE_KEY)) {
                        int available = response.getInt(ObjectAPIHandler.AVAILABLE_KEY);
                        return available;
                    }
                    throw new IOException("bad response from MCP: success but no message key");
                } else {
                    throw handleError(response);
                }
            }
            private JSONObject getResponse(APIHandler handler) throws IOException {
                String protocolhostportstub = MCPQueueFactory.this.getConnectionURL();
                ServiceResponse sr = handler.serviceImpl(protocolhostportstub, params);
                return sr.getObject();
            }
            private boolean success(JSONObject response) {
                return response.has(ObjectAPIHandler.SUCCESS_KEY) && response.getBoolean(ObjectAPIHandler.SUCCESS_KEY);
            }
            private void connectMCP(JSONObject response) {
                if (response.has(ObjectAPIHandler.SERVICE_KEY)) {
                    String broker = response.getString(ObjectAPIHandler.SERVICE_KEY);
                    if (MCPQueueFactory.this.broker.connectRabbitMQ(broker)) {
                        Logger.info(this.getClass(), "connected MCP broker at " + broker);
                    } else {
                        Logger.error(this.getClass(), "failed to connect MCP broker at " + broker);
                    }
                }
            }
            private IOException handleError(JSONObject response) {
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

}
