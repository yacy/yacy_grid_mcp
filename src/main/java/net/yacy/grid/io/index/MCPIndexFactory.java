/**
 *  MCPIndexFactory
 *  Copyright 05.03.2018 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.index;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.api.index.AddService;
import net.yacy.grid.mcp.api.index.CheckService;
import net.yacy.grid.mcp.api.index.CountService;
import net.yacy.grid.mcp.api.index.DeleteService;
import net.yacy.grid.mcp.api.index.ExistService;
import net.yacy.grid.mcp.api.index.QueryService;
import net.yacy.grid.tools.JSONList;

public class MCPIndexFactory implements IndexFactory {

    private GridIndex index;
    private String server;
    private int port;

    public MCPIndexFactory(GridIndex index, String server, int port) {
        this.index = index;
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
    public Index getIndex() throws IOException {
        final JSONObject params = new JSONObject(true);

        return new Index() {

            private JSONObject getResponse(APIHandler handler) throws IOException {
                String protocolhostportstub = MCPIndexFactory.this.getConnectionURL();
                ServiceResponse sr = handler.serviceImpl(protocolhostportstub, params);
                return sr.getObject();
            }
            private boolean success(JSONObject response) {
                return response.has(ObjectAPIHandler.SUCCESS_KEY) && response.getBoolean(ObjectAPIHandler.SUCCESS_KEY);
            }
            private void connectMCP(JSONObject response) {
                if (response.has(ObjectAPIHandler.SERVICE_KEY)) {
                    String elastic = response.getString(ObjectAPIHandler.SERVICE_KEY);
                    if (MCPIndexFactory.this.index.connectElasticsearch(elastic)) {
                        Data.logger.info("connected MCP index at " + elastic);
                    } else {
                        Data.logger.error("failed to connect MCP index at " + elastic);
                    }
                }
            }
            private IOException handleError(JSONObject response) {
                if (response.has(ObjectAPIHandler.COMMENT_KEY)) {
                    return new IOException("cannot connect to MCP: " + response.getString(ObjectAPIHandler.COMMENT_KEY));
                }
                return new IOException("bad response from MCP: no success and no comment key");
            }

            @Override
            public IndexFactory checkConnection() throws IOException {
                String protocolhostportstub = MCPIndexFactory.this.getConnectionURL();
                APIHandler apiHandler = APIServer.getAPI(CheckService.NAME);
                ServiceResponse sr = apiHandler.serviceImpl(protocolhostportstub, params);
                JSONObject response = sr.getObject();
                if (success(response)) {
                    connectMCP(response);
                    return MCPIndexFactory.this;
                } else {
                    throw new IOException("MCP does not respond properly");
                }
            }

            @Override
            public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("id", id);
                params.put("object", object.toString());
                JSONObject response = getResponse(APIServer.getAPI(AddService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return MCPIndexFactory.this;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public IndexFactory addBulk(String indexName, String typeName, final Map<String, JSONObject> objects) throws IOException {
                // We do not introduce a new protocol here. Instead we use the add method.
                // This is not a bad design because grid clients will learn how to use
                // the native elasticsearch interface to do this in a better way.
                for (Map.Entry<String, JSONObject> entry: objects.entrySet()) {
                    add(indexName, typeName, entry.getKey(), entry.getValue());
                }
                return MCPIndexFactory.this;
            }

            @Override
            public boolean exist(String indexName, String typeName, String id) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("id", id);
                JSONObject response = getResponse(APIServer.getAPI(ExistService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return response.has("exists") && response.getBoolean("exists");
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public Set<String> existBulk(String indexName, String typeName, Collection<String> ids) throws IOException {
                // We do not introduce a new protocol here. Instead we use the exist method.
                // This is not a bad design because grid clients will learn how to use
                // the native elasticsearch interface to do this in a better way.
                Set<String> exists = new HashSet<>();
                for (String id: ids) {
                    if (exist(indexName, typeName, id)) exists.add(id);
                }
                return exists;
            }

            @Override
            public long count(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("language", language.name());
                params.put("query", query);
                JSONObject response = getResponse(APIServer.getAPI(CountService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return response.has("count") ? response.getLong("count") : 0;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public JSONObject query(String indexName, String typeName, String id) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("id", id);
                JSONObject response = getResponse(APIServer.getAPI(QueryService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    if (!response.has("list")) return null;
                    JSONArray list = response.getJSONArray("list");
                    if (list.length() == 0) return null;
                    return list.getJSONObject(0);
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public Map<String, JSONObject> queryBulk(String indexName, String typeName, Collection<String> ids) throws IOException {
                // We do not introduce a new protocol here. Instead we use the query method.
                // This is not a bad design because grid clients will learn how to use
                // the native elasticsearch interface to do this in a better way.
                Map<String, JSONObject> result = new HashMap<>();
                for (String id: ids) {
                    try {
                        JSONObject j = query(indexName, typeName, id);
                        result.put(id, j);
                    } catch (IOException e) {}
                }
                return result;
            }

            @Override
            public JSONList query(String indexName, String typeName, QueryLanguage language, String query, int start, int count) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("language", language.name());
                params.put("query", query);
                JSONObject response = getResponse(APIServer.getAPI(QueryService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    JSONList list = new JSONList();
                    if (!response.has("list")) return list;
                    JSONArray l = response.getJSONArray("list");
                    if (l.length() == 0) return list;
                    for (int i = 0; i < l.length(); i++) list.add(l.getJSONObject(i));
                    return list;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public JSONObject query(final String indexName, String typeName, final QueryBuilder queryBuilder, final QueryBuilder postFilter, final Sort sort, final HighlightBuilder hb, int timezoneOffset, int from, int resultCount, int aggregationLimit, boolean explain, WebMapping... aggregationFields) throws IOException {
                throw new IOException("method not implemented"); // TODO implement this!
            }

            @Override
            public boolean delete(String indexName, String typeName, String id) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("id", id);
                JSONObject response = getResponse(APIServer.getAPI(DeleteService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return response.has("deleted") && response.getBoolean("deleted");
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public long delete(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
                params.put("index", indexName);
                params.put("type", typeName);
                params.put("language", language.name());
                params.put("query", query);
                JSONObject response = getResponse(APIServer.getAPI(DeleteService.NAME));

                // read the broker to store the service definition of the remote queue, if exists
                if (success(response)) {
                    connectMCP(response);
                    return response.has("count") ? response.getLong("count") : 0;
                } else {
                    throw handleError(response);
                }
            }

            @Override
            public void close() {
            }

        };
    }

    @Override
    public void close() {
        // this is stateless, do nothing
    }

}
