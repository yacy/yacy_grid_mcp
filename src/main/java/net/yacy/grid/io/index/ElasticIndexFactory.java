/**
 *  ElasticIndexFactory
 *  Copyright 04.03.2018 by Michael Peter Christen, @0rb1t3r
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.common.unit.Fuzziness;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.Classification;
import net.yacy.grid.tools.JSONList;

public class ElasticIndexFactory implements IndexFactory {

    public final static String PROTOCOL_PREFIX = "elastic://";

    private ElasticsearchClient elasticsearchClient = null;
    private String elasticsearchAddress;
    private String elasticsearchClusterName;
    private Index index;

    public ElasticIndexFactory(String elasticsearchAddress, String elasticsearchClusterName) throws IOException {
        if (elasticsearchAddress == null || elasticsearchAddress.length() == 0) throw new IOException("the elasticsearch Address must be given");

        this.elasticsearchAddress = elasticsearchAddress;
        this.elasticsearchClusterName = elasticsearchClusterName;

        // create elasticsearch connection
        this.elasticsearchClient = new ElasticsearchClient(new String[]{this.elasticsearchAddress}, this.elasticsearchClusterName.length() == 0 ? null : this.elasticsearchClusterName);
        Data.logger.info("Connected elasticsearch at " + Data.getHost(this.elasticsearchAddress));

        Path mappingsPath = Paths.get("conf","mappings");
        if (mappingsPath.toFile().exists()) {
            for (File f: mappingsPath.toFile().listFiles()) {
                if (f.getName().endsWith(".json")) {
                    String indexName = f.getName();
                    indexName = indexName.substring(0, indexName.length() - 5); // cut off ".json"
                    try {
                        this.elasticsearchClient.createIndexIfNotExists(indexName, 1 /*shards*/, 1 /*replicas*/);
                        JSONObject mo = new JSONObject(new JSONTokener(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)));
                        mo = mo.getJSONObject("mappings").getJSONObject("_default_");
                        this.elasticsearchClient.setMapping(indexName, mo.toString());
                        Data.logger.info("initiated mapping for index " + indexName);
                    } catch (IOException | NoNodeAvailableException e) {
                        this.elasticsearchClient = null; // index not available
                        Data.logger.info("Failed creating mapping for index " + indexName, e);
                    }
                }
            }
        }

        // create index
        this.index = new Index() {

            @Override
            public IndexFactory checkConnection() throws IOException {
                return ElasticIndexFactory.this;
            }

            @Override
            public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
                ElasticIndexFactory.this.elasticsearchClient.writeMap(indexName, typeName, id, object.toMap());
                return ElasticIndexFactory.this;
            }

            @Override
            public boolean exist(String indexName, String typeName, String id) throws IOException {
                return ElasticIndexFactory.this.elasticsearchClient.exist(indexName, typeName, id);
            }

            @Override
            public long count(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                return ElasticIndexFactory.this.elasticsearchClient.count(qb, indexName, typeName);
            }

            @Override
            public JSONObject query(String indexName, String typeName, String id) throws IOException {
                Map<String, Object> map = ElasticIndexFactory.this.elasticsearchClient.readMap(indexName, typeName, id);
                if (map == null) return null;
                return new JSONObject(map);
            }

            @Override
            public JSONList query(String indexName, String typeName, QueryLanguage language, String query, int start, int count) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                ElasticsearchClient.Query q = ElasticIndexFactory.this.elasticsearchClient.query(indexName, null, qb, null, Sort.DEFAULT, null, 0, start, count, 0, false);
                List<Map<String, Object>> result = q.results;
                JSONList list = new JSONList();
                for (int hitc = 0; hitc < result.size(); hitc++) {
                    Map<String, Object> map = result.get(hitc);
                    list.add(new JSONObject(map));
                }
                return list;
            }

            @Override
            public boolean delete(String indexName, String typeName, String id) throws IOException {
                return ElasticIndexFactory.this.elasticsearchClient.delete(indexName, typeName, id);
            }

            @Override
            public long delete(String indexName, String typeName, QueryLanguage language, String query) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                return ElasticIndexFactory.this.elasticsearchClient.deleteByQuery(indexName, typeName, qb);
            }

            @Override
            public void close() {
            }

            private QueryBuilder getQuery(QueryLanguage language, String query) {
                QueryBuilder qb = QueryBuilders.queryStringQuery("");
                if (language == QueryLanguage.elastic) {
                    QueryStringQueryBuilder qsqb = QueryBuilders.queryStringQuery(query);
                    qsqb.useDisMax(false); // we want a boolean query here
                    qsqb.defaultField(WebMapping.text_t.name());
                    qsqb.defaultOperator(Operator.AND);
                    qsqb.fuzziness(Fuzziness.ZERO);
                    qb = qsqb;
                }
                if (language == QueryLanguage.gsa || language == QueryLanguage.yacy) {
                    qb = new YaCyQuery(query, null, Classification.ContentDomain.ALL, 0).queryBuilder;
                }
                return qb;
            }

        };
    }

    public ElasticsearchClient getClient() {
        return this.elasticsearchClient;
    }

    @Override
    public String getConnectionURL() {
        return PROTOCOL_PREFIX + this.elasticsearchAddress + "/" + this.elasticsearchClusterName;
    }

    @Override
    public String getHost() {
        return Data.getHost(this.elasticsearchAddress);
    }

    @Override
    public boolean hasDefaultPort() {
        return true;
    }

    @Override
    public int getPort() {
        return 9300;
    }

    @Override
    public Index getIndex() throws IOException {
        return this.index;
    }

    @Override
    public void close() {
        this.elasticsearchClient.close();
    }


}
