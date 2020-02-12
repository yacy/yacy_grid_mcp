/**
 *  Index
 *  Copyright 7.1.2018 by Michael Peter Christen, @0rb1t3r
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

import net.yacy.grid.tools.JSONList;

/**
 * Grid-Service index Interface for grid-wide search
 * 
 *
 */
public interface Index {

    public IndexFactory checkConnection() throws IOException;

    public IndexFactory add(String indexName, String typeName, final String id, JSONObject object) throws IOException;

    public IndexFactory addBulk(String indexName, String typeName, final Map<String, JSONObject> objects) throws IOException;

    public boolean exist(String indexName, String id) throws IOException;

    public Set<String> existBulk(String indexName, Collection<String> ids) throws IOException;

    public long count(String indexName, QueryLanguage language, String query) throws IOException;

    public JSONObject query(String indexName, String id) throws IOException;

    public Map<String, JSONObject> queryBulk(String indexName, Collection<String> ids) throws IOException;

    public JSONList query(String indexName, QueryLanguage language, String query, int start, int count) throws IOException;

    public JSONObject query(final String indexName, final QueryBuilder queryBuilder, final QueryBuilder postFilter, final Sort sort, final HighlightBuilder hb, int timezoneOffset, int from, int resultCount, int aggregationLimit, boolean explain, WebMapping... aggregationFields) throws IOException;

    public boolean delete(String indexName, String typeName, String id) throws IOException;

    public long delete(String indexName, QueryLanguage language, String query) throws IOException;

    public void refresh(String indexName);

    public void close();

    public static enum QueryLanguage {
        yacy,   // a YaCy search query, must match all terms and search operators as in https://support.google.com/websearch/answer/2466433?visit_id=1-636509668520326600-1109926908&p=adv_operators&hl=en&rd=1
        gsa,    // a Google query string as in https://www.google.com/support/enterprise/static/gsa/docs/admin/74/gsa_doc_set/xml_reference/request_format.html#1076993
        elastic,// a Query String Query as in https://www.elastic.co/guide/en/elasticsearch/reference/6.1/query-dsl-query-string-query.html
        fields  // a flat JSON object with key-value pairs which have to match all
    }
}
