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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.json.JSONObject;

import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.JSONList;

public class GridIndex implements Index {

    public final static String CRAWLSTART_INDEX_NAME = "crawlstart";
    public final static String CRAWLER_INDEX_NAME    = "crawler";
    public final static String QUERY_INDEX_NAME      = "query";
    public final static String WEB_INDEX_NAME        = "web";

    // in elastic 6.x only one type is allowed for indexes! The type concept shall be removed in 8.x
    public final static String WEB_TYPE_NAME         = "web"; // for historical reasons we use that type for "web" indexes.
    public final static String EVENT_TYPE_NAME       = "event"; // "event" shall be used for all other indexes

    private ElasticIndexFactory elasticIndexFactory;
    private MCPIndexFactory mcpIndexFactory;

    private String elastic_address;
    private String mcp_host;
    private int mcp_port;
    private boolean shallRun;

    public GridIndex() {
        this.elastic_address = null;
        this.elasticIndexFactory = null;
        this.mcpIndexFactory = null;
        this.mcp_host = null;
        this.mcp_port = -1;
        this.shallRun = true;
    }
    
    public boolean isConnected() {
    	return this.elastic_address != null && this.elasticIndexFactory != null;
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

        // if we reach this point, we try (forever) until we get a connection to elasticsearch
        loop: while (this.shallRun) {
            try {
                this.elasticIndexFactory = new ElasticIndexFactory(address, cluster);
                Data.logger.info("Index/Client: connected to elasticsearch at " + address);
                this.elastic_address = address;
                return true;
            } catch (IOException e) {
                Data.logger.info("Index/Client: trying to connect to elasticsearch at " + address + " failed", e);
                try {Thread.sleep(5000);} catch (InterruptedException e1) {}
                continue loop;
            }
        }
        return false;
    }

    /**
     * Getting the elastic client:
     * this should considered as a low-level function that only MCP-internal classes may call.
     * All other packages must use a "Index" object.
     * @return
     */
    public ElasticsearchClient getElasticClient() {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        return this.elasticIndexFactory.getClient();
    }

    public Index getElasticIndex() throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        return this.elasticIndexFactory.getIndex();
    }

    public boolean connectMCP(String host, int port) {
        this.mcp_host = host;
        this.mcp_port = port;
        this.mcpIndexFactory = new MCPIndexFactory(this, host, port);
        Data.logger.info("Index/Client: connected to an index over MCP at " + host + ":" + port);
        return true;
    }

    @Override
    public IndexFactory checkConnection() throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) {
            return this.elasticIndexFactory;
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                throw new IOException("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) {
            this.mcpIndexFactory.getIndex().checkConnection();
        	return this.mcpIndexFactory;
        }
        throw new IOException("Index/Client: add mcp service: no factory found!");
    }

    @Override
    public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            this.elasticIndexFactory.getIndex().add(indexName, typeName, id, object);
            //Data.logger.info("Index/Client: add elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return this.elasticIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            this.mcpIndexFactory.getIndex().add(indexName, typeName, id, object);
            //Data.logger.info("Index/Client: add mcp service '" + mcp_host + "', object with id:" + id);
            return this.mcpIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add mcp service '" + mcp_host + "',mcp fail", e);
        }
        throw new IOException("Index/Client: add mcp service: no factory found!");
    }

    @Override
    public IndexFactory addBulk(String indexName, String typeName, final Map<String, JSONObject> objects) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            this.elasticIndexFactory.getIndex().addBulk(indexName, typeName, objects);
            //Data.logger.info("Index/Client: add elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return this.elasticIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            this.mcpIndexFactory.getIndex().addBulk(indexName, typeName, objects);
            //Data.logger.info("Index/Client: add mcp service '" + mcp_host + "', object with id:" + id);
            return this.mcpIndexFactory;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: add mcp service '" + mcp_host + "',mcp fail", e);
        }
        throw new IOException("Index/Client: add mcp service: no factory found!");
    }

    @Override
    public boolean exist(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            boolean exist = this.elasticIndexFactory.getIndex().exist(indexName, typeName, id);
            //Data.logger.info("Index/Client: exist elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: exist elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            boolean exist = this.mcpIndexFactory.getIndex().exist(indexName, typeName, id);
            //Data.logger.info("Index/Client: exist mcp service '" + mcp_host + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: exist mcp service '" + mcp_host + "',mcp fail", e);
        }
        throw new IOException("Index/Client: exist mcp service: no factory found!");
    }

    @Override
    public Set<String> existBulk(String indexName, String typeName, Collection<String> ids) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            Set<String> exist = this.elasticIndexFactory.getIndex().existBulk(indexName, typeName, ids);
            //Data.logger.info("Index/Client: exist elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: existBulk elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            Set<String> exist = this.mcpIndexFactory.getIndex().existBulk(indexName, typeName, ids);
            //Data.logger.info("Index/Client: exist mcp service '" + mcp_host + "', object with id:" + id);
            return exist;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: existBulk mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: existBulk mcp service: no factory found!");
    }

    @Override
    public long count(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            long count = this.elasticIndexFactory.getIndex().count(indexName, typeName, language, query);
            //Data.logger.info("Index/Client: count elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with query:" + query);
            return count;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: count elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            long count = this.mcpIndexFactory.getIndex().count(indexName, typeName, language, query);
            //Data.logger.info("Index/Client: count mcp service '" + mcp_host + "', object with query:" + query);
            return count;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: count mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: count mcp service: no factory found!");
    }

    @Override
    public JSONObject query(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            JSONObject json = this.elasticIndexFactory.getIndex().query(indexName, typeName, id);
            //Data.logger.info("Index/Client: query elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return json;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/3 elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            JSONObject json = this.mcpIndexFactory.getIndex().query(indexName, typeName, id);
            //Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with id:" + id);
            return json;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/3 mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: query/3 mcp service: no factory found!");
    }

    @Override
    public Map<String, JSONObject> queryBulk(String indexName, String typeName, Collection<String> ids) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            Map<String, JSONObject> map = this.elasticIndexFactory.getIndex().queryBulk(indexName, typeName, ids);
            //Data.logger.info("Index/Client: query elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return map;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: queryBulk/3 elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            Map<String, JSONObject> map = this.mcpIndexFactory.getIndex().queryBulk(indexName, typeName, ids);
            //Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with id:" + id);
            return map;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: queryBulk/3 mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: queryBulk/3 mcp service: no factory found!");
    }

    @Override
    public JSONList query(String indexName, String typeName, QueryLanguage language, String query, int start, int count) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            JSONList list = this.elasticIndexFactory.getIndex().query(indexName, typeName, language, query, start, count);
            //Data.logger.info("Index/Client: query elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with query:" + query);
            return list;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/6 elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            JSONList list = this.mcpIndexFactory.getIndex().query(indexName, typeName, language, query, start, count);
            //Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with query:" + query);
            return list;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/6 mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: query/6 mcp service: no factory found!");
    }

    @Override
    public JSONObject query(final String indexName, String typeName, final QueryBuilder queryBuilder, final QueryBuilder postFilter, final Sort sort, final HighlightBuilder hb, int timezoneOffset, int from, int resultCount, int aggregationLimit, boolean explain, WebMapping... aggregationFields) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
        	JSONObject queryResult = this.elasticIndexFactory.getIndex().query(indexName, typeName, queryBuilder, postFilter, sort, hb, timezoneOffset, from, resultCount, aggregationLimit, explain, aggregationFields);
            //Data.logger.info("Index/Client: query elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with query:" + query);
            return queryResult;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/12 elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
        	JSONObject queryResult = this.mcpIndexFactory.getIndex().query(indexName, typeName, queryBuilder, postFilter, sort, hb, timezoneOffset, from, resultCount, aggregationLimit, explain, aggregationFields);
            //Data.logger.info("Index/Client: query mcp service '" + mcp_host + "', object with query:" + query);
            return queryResult;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: query/12 mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: query/12 mcp service: no factory found!");
    }

    @Override
    public boolean delete(String indexName, String typeName, String id) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            boolean deleted = this.elasticIndexFactory.getIndex().delete(indexName, typeName, id);
            //Data.logger.info("Index/Client: delete elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with id:" + id);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            boolean deleted = this.mcpIndexFactory.getIndex().delete(indexName, typeName, id);
            //Data.logger.info("Index/Client: delete mcp service '" + mcp_host + "', object with id:" + id);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: delete mcp service: no factory found!");
    }

    @Override
    public long delete(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
        if (this.elasticIndexFactory == null && this.elastic_address != null) {
            connectElasticsearch(this.elastic_address); // try to connect again..
        }
        if (this.elasticIndexFactory != null) try {
            long deleted = this.elasticIndexFactory.getIndex().delete(indexName, typeName, language, query);
            //Data.logger.info("Index/Client: delete elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', object with query:" + query);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete elastic service '" + this.elasticIndexFactory.getConnectionURL() + "', elastic fail", e);
        }
        if (this.mcpIndexFactory == null && this.mcp_host != null) {
            connectMCP(this.mcp_host, this.mcp_port); // try to connect again..
            if (this.mcpIndexFactory == null) {
                Data.logger.warn("Index/Client: FATAL: connection to MCP lost!");
            }
        }
        if (this.mcpIndexFactory != null) try {
            long deleted = this.mcpIndexFactory.getIndex().delete(indexName, typeName, language, query);
            //Data.logger.info("Index/Client: delete mcp service '" + mcp_host + "', object with query:" + query);
            return deleted;
        } catch (IOException e) {
            Data.logger.debug("Index/Client: delete mcp service '" + mcp_host + "', mcp fail", e);
        }
        throw new IOException("Index/Client: delete mcp service: no factory found!");
    }

    @Override
    public void close() {
        this.shallRun = false;
        if (this.elasticIndexFactory != null) this.elasticIndexFactory.close();
    }

}
