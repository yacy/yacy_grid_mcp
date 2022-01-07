/**
 *  ElasticIndexFactory
 *  Copyright 04.03.2018 by Michael Peter Christen, @orbiterlab
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.yacy.grid.io.index.ElasticsearchClient.BulkEntry;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.Classification;
import net.yacy.grid.tools.JSONList;
import net.yacy.grid.tools.Logger;

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
        Logger.info(this.getClass(), "Connected elasticsearch at " + Data.getHost(this.elasticsearchAddress));

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
                        Logger.info(this.getClass(), "initiated mapping for index " + indexName);
                    } catch (IOException | NoNodeAvailableException e) {
                        this.elasticsearchClient = null; // index not available
                        Logger.warn(this.getClass(), "Failed creating mapping for index " + indexName, e);
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
            public IndexFactory addBulk(String indexName, String typeName, final Map<String, JSONObject> objects) throws IOException {
                if (objects.size() > 0) {
                    List<BulkEntry> entries = new ArrayList<>();
                    objects.forEach((id, obj) -> {
                        entries.add(new BulkEntry(id, typeName, null, obj.toMap()));
                    });
                    ElasticIndexFactory.this.elasticsearchClient.writeMapBulk(indexName, entries);
                }
                return ElasticIndexFactory.this;
            }

            @Override
            public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
                ElasticIndexFactory.this.elasticsearchClient.writeMap(indexName, typeName, id, object.toMap());
                return ElasticIndexFactory.this;
            }

            @Override
            public boolean exist(String indexName, String id) throws IOException {
                return ElasticIndexFactory.this.elasticsearchClient.exist(indexName, id);
            }

            @Override
            public Set<String> existBulk(String indexName, Collection<String> ids) throws IOException {
                return ElasticIndexFactory.this.elasticsearchClient.existBulk(indexName, ids);
            }

            @Override
            public long count(String indexName, QueryLanguage language, String query) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                return ElasticIndexFactory.this.elasticsearchClient.count(qb, indexName);
            }

            @Override
            public JSONObject query(String indexName, String id) throws IOException {
                Map<String, Object> map = ElasticIndexFactory.this.elasticsearchClient.readMap(indexName, id);
                if (map == null) return null;
                return new JSONObject(map);
            }

            @Override
            public Map<String, JSONObject> queryBulk(String indexName, Collection<String> ids) throws IOException {
                Map<String, Map<String, Object>> bulkresponse = ElasticIndexFactory.this.elasticsearchClient.readMapBulk(indexName, ids);
                Map<String, JSONObject> response = new HashMap<>();
                bulkresponse.forEach((id, obj) -> response.put(id, new JSONObject(obj)));
                return response;
            }

            @Override
            public JSONList query(String indexName, QueryLanguage language, String query, int start, int count) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                ElasticsearchClient.Query q = ElasticIndexFactory.this.elasticsearchClient.query(indexName, qb, null, Sort.DEFAULT, null, 0, start, count, 0, false);
                List<Map<String, Object>> results = q.results;
                JSONList list = new JSONList();
                for (int hitc = 0; hitc < results.size(); hitc++) {
                    Map<String, Object> map = results.get(hitc);
                    list.add(new JSONObject(map));
                }
                return list;
            }

            @Override
            public JSONObject query(final String indexName, final QueryBuilder queryBuilder, final QueryBuilder postFilter, final Sort sort, final HighlightBuilder hb, int timezoneOffset, int from, int resultCount, int aggregationLimit, boolean explain, WebMapping... aggregationFields) throws IOException {
                ElasticsearchClient.Query q = ElasticIndexFactory.this.elasticsearchClient.query(indexName, queryBuilder, postFilter, sort, hb, timezoneOffset, from, resultCount, aggregationLimit, explain, aggregationFields);
                JSONObject queryResult = new JSONObject(true);

                int hitCount = q.hitCount;
                queryResult.put("hitCount", hitCount);

                List<Map<String, Object>> results = q.results;
                JSONList list = new JSONList();
                for (int hitc = 0; hitc < results.size(); hitc++) {
                    Map<String, Object> map = results.get(hitc);
                    list.add(new JSONObject(map));
                }
                queryResult.put("results", list);

                List<String> explanations = q.explanations;
                queryResult.put("explanations", explanations);

                return queryResult;
            }

            @Override
            public boolean delete(String indexName, String typeName, String id) throws IOException {
                return ElasticIndexFactory.this.elasticsearchClient.delete(indexName, typeName, id);
            }

            @Override
            public long delete(String indexName, QueryLanguage language, String query) throws IOException {
                QueryBuilder qb = getQuery(language, query);
                return ElasticIndexFactory.this.elasticsearchClient.deleteByQuery(indexName, qb);
            }

            @Override
            public void refresh(String indexName) {
                ElasticIndexFactory.this.elasticsearchClient.refresh(indexName);
            }

            @Override
            public void close() {
            }

            private QueryBuilder getQuery(QueryLanguage language, String query) {
                QueryBuilder qb = QueryBuilders.boolQuery();
                if (language == QueryLanguage.fields) {
                    qb = QueryBuilders.boolQuery();
                    JSONObject json = new JSONObject(query);
                    for (String key: json.keySet()) {
                        ((BoolQueryBuilder) qb).filter(QueryBuilders.termQuery(key, json.get(key)));
                    }
                } else if (language == QueryLanguage.elastic) {
                    QueryStringQueryBuilder qsqb = QueryBuilders.queryStringQuery(query);
                    qsqb.useDisMax(false); // we want a boolean query here
                    qsqb.defaultOperator(Operator.AND);
                    qsqb.fuzziness(Fuzziness.ZERO);
                    qb = qsqb;
                } else if (language == QueryLanguage.gsa || language == QueryLanguage.yacy) {
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
