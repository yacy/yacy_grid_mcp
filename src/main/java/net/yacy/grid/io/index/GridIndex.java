/**
 *  GridIndex
 *  Copyright 5.3.2018 by Michael Peter Christen, @0rb1t3r
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

import org.json.JSONObject;

import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.JSONList;

public class GridIndex implements Index {

    private ElasticIndexFactory elasticIndexFactory;
    private MCPIndexFactory mcpIndexFactory;

    private String elastic_address;
    private String mcp_host;
    private int mcp_port;
    
    public GridIndex() {
        this.elastic_address = null;
        this.elasticIndexFactory = null;
        this.mcpIndexFactory = null;
        this.mcp_host = null;
        this.mcp_port = -1;
    }
    
    public boolean connectElasticsearch(String address) {
        if (!address.startsWith(ElasticIndexFactory.PROTOCOL_PREFIX)) return false;
        address = address.substring(ElasticIndexFactory.PROTOCOL_PREFIX.length());
        int p = address.indexOf('/');
        String cluster = "";
        if (p >= 0) {
            cluster = address.substring(p + 1);
            address = address.substring(0, p);
        }
        if (address.length() == 0) return false;
        try {
            this.elasticIndexFactory = new ElasticIndexFactory(address, cluster);
            Data.logger.info("Index/Client: connected to elasticsearch at " + address);
            return true;
        } catch (IOException e) {
            Data.logger.info("Index/Client: trying to connect to elasticsearch at " + address + " failed", e);
            return false;
        }
    }

    public ElasticsearchClient getElasticClient() {
        return this.elasticIndexFactory.getClient();
    }
    
    public Index getElasticIndex() throws IOException {
        return this.elasticIndexFactory.getIndex();
    }
    
    public boolean connectMCP(String host, int port) {
        if (this.mcp_host == null) {
            this.mcp_host = host;
            this.mcp_port = port;
        }
        this.mcpIndexFactory = new MCPIndexFactory(this, host, port);
        Data.logger.info("Index/Client: connected to an index over MCP at " + host + ":" + port);
        return true;
    }
    
    @Override
    public IndexFactory checkConnection() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            this.elasticIndexFactory.getIndex().add(indexName, typeName, id, object);
            Data.logger.info("Index/Client: add elastic service '" + elastic_address + "', object with id:" + id);
            return this.elasticIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            this.mcpIndexFactory.getIndex().add(indexName, typeName, id, object);
            Data.logger.info("Index/Client: add mcp service '" + mcp_host + "', object with id:" + id);
            return this.mcpIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: add mcp service: no factory found!");
        return null;
    }

    @Override
    public boolean exist(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            boolean exist = this.elasticIndexFactory.getIndex().exist(indexName, typeName, id);
            Data.logger.info("Index/Client: exist elastic service '" + elastic_address + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: exist elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            boolean exist = this.mcpIndexFactory.getIndex().exist(indexName, typeName, id);
            Data.logger.info("Index/Client: exist mcp service '" + mcp_host + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: exist mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: exist mcp service: no factory found!");
        return false;
    }

    @Override
    public long count(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            long count = this.elasticIndexFactory.getIndex().count(indexName, typeName, language, query);
            Data.logger.info("Index/Client: count elastic service '" + elastic_address + "', object with query:" + query);
            return count;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: count elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            long count = this.mcpIndexFactory.getIndex().count(indexName, typeName, language, query);
            Data.logger.info("Index/Client: count mcp service '" + mcp_host + "', object with query:" + query);
            return count;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: count mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: count mcp service: no factory found!");
        return 0;
    }

    @Override
    public JSONObject query(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            JSONObject json = this.elasticIndexFactory.getIndex().query(indexName, typeName, id);
            Data.logger.info("Index/Client: query elastic service '" + elastic_address + "', object with id:" + id);
            return json;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            JSONObject json = this.mcpIndexFactory.getIndex().query(indexName, typeName, id);
            Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with id:" + id);
            return json;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: query mcp service: no factory found!");
        return null;
    }

    @Override
    public JSONList query(String indexName, String typeName, QueryLanguage language, String query, int start, int count) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            JSONList list = this.elasticIndexFactory.getIndex().query(indexName, typeName, language, query, start, count);
            Data.logger.info("Index/Client: query elastic service '" + elastic_address + "', object with query:" + query);
            return list;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            JSONList list = this.mcpIndexFactory.getIndex().query(indexName, typeName, language, query, start, count);
            Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with query:" + query);
            return list;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: query mcp service: no factory found!");
        return null;
    }

    @Override
    public boolean delete(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            boolean deleted = this.elasticIndexFactory.getIndex().delete(indexName, typeName, id);
            Data.logger.info("Index/Client: delete elastic service '" + elastic_address + "', object with id:" + id);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            boolean deleted = this.mcpIndexFactory.getIndex().delete(indexName, typeName, id);
            Data.logger.info("Index/Client: delete mcp service '" + mcp_host + "', object with id:" + id);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: delete mcp service: no factory found!");
        return false;
    }

    @Override
    public long delete(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            // try to connect again..
            connectElasticsearch(this.elastic_address);
        }
        if (this.elasticIndexFactory == null) {
            this.elastic_address = null;
        } else try {
            long deleted = this.elasticIndexFactory.getIndex().delete(indexName, typeName, language, query);
            Data.logger.info("Index/Client: delete elastic service '" + elastic_address + "', object with query:" + query);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete elastic service '" + elastic_address + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            // try to connect again..
            connectMCP(this.mcp_host, this.mcp_port);
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            long deleted = this.mcpIndexFactory.getIndex().delete(indexName, typeName, language, query);
            Data.logger.info("Index/Client: delete mcp service '" + mcp_host + "', object with query:" + query);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete mcp service '" + mcp_host + "',mcp fail", e);
        }
        Data.logger.info("Index/Client: delete mcp service: no factory found!");
        return 0;
    }

    @Override
    public void close() {
    }

}
