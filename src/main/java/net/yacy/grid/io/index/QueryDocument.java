/**
 *  QueryDocument
 *  Copyright 9.3.2018 by Michael Peter Christen, @0rb1t3r
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
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import net.yacy.grid.mcp.Data;

public class QueryDocument extends Document {

    public QueryDocument() {
        super();
    }

    public QueryDocument(Map<String, Object> map) {
        super(map);
    }

    public QueryDocument(JSONObject obj) {
        super(obj);
    }

    public static void storeBulk(Index index, Collection<QueryDocument> documents) throws IOException {
        if (index == null) return;
        Map<String, JSONObject> map = new HashMap<>();
        documents.forEach(queryDocument -> {
            String id = Long.toString(System.currentTimeMillis());
            map.put(id, queryDocument);
        });
        index.addBulk(Data.config.get("grid.elasticsearch.indexName.query"), Data.config.get("grid.elasticsearch.typeName"), map);
    }

    public QueryDocument store(Index index) throws IOException {
        String id = Long.toString(System.currentTimeMillis());
        index.add(Data.config.get("grid.elasticsearch.indexName.query"), Data.config.get("grid.elasticsearch.typeName"), id, this);
        return this;
    }
    /*
     * fields to add:
    query_t(MappingType.text_general, true, true, false, false, true, "the exact query as the user made in the search text field"),
    origin_s(MappingType.string, true, true, false, true, true, "the origin refers to a place where the query came from. It can i.e. be an IP. IPs must be pseudomized according to requirements by law.", true),
    collection_sxt(MappingType.string, true, true, true, false, false, "tags that are attached to crawls/index generation", false, "collections", "Collections", "String", "collection"),
    timezoneOffset_i(MappingType.num_integer, true, true, false, false, false, "the timezone offset as submitted by the browser", true),
    startRecord_i(MappingType.num_integer, true, true, false, false, false, "the number of the first hit. The minumum record number is 0 for the first hit.", true),
    maximumRecords_i(MappingType.num_integer, true, true, false, false, false, "the wanted maximum number of records. However, this might not be the same number as returned as search result.", true),
    date_dt(MappingType.date, true, true, false, false, false, "the time when the search was done"),
    hits_i(MappingType.num_integer, true, true, false, false, false, "the number of hits in the search index. Not the number of returned results.", true),
    compiled_s(MappingType.string, true, true, false, true, true, "The query as it can be represented in a compiled form.", true),
    count_i(MappingType.num_integer, true, true, false, false, false, "the number of hits which are returned in the search result.", true),
    title_txt(MappingType.text_general, true, true, true, false, true, "the titles of the search hits"),
    url_sxt(MappingType.string, true, true, true, false, false, "the urls of the search hits", false),
    snippet_txt(MappingType.text_general, true, true, true, false, true, "the snippets of the search hits"),
    last_modified_dts(MappingType.date, true, true, true, false, true, "the document dates of the search hits"),
    size_val(MappingType.num_integer, true, true, true, false, false, "size of the documents of the search hits");
     */
    public QueryDocument addDocument(List<Map<String, Object>> searchresult) {
        for (int hitc = 0; hitc < searchresult.size(); hitc++) {
            Document doc = new Document(searchresult.get(hitc));
        }
        return this;
    }
}
