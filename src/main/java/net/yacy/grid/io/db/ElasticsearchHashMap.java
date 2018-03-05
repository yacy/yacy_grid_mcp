/**
 *  ElasticsearchHashMap
 *  Copyright 03.12.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.db;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.index.query.QueryBuilders;

import net.yacy.grid.io.index.ElasticsearchClient;
import net.yacy.grid.io.index.Sort;
import net.yacy.grid.io.index.ElasticsearchClient.Query;

public class ElasticsearchHashMap extends AbstractMap<String, String> implements CloseableMap<String, String> {

    private ElasticsearchClient elastic;
    private String index;
    
    public ElasticsearchHashMap(ElasticsearchClient elastic, String index) {
        this.elastic = elastic;
        this.index = index;
    }
    
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        Query query = this.elastic.query(this.index, null, QueryBuilders.matchAllQuery(), null, Sort.DEFAULT, null, 0, 0, Integer.MAX_VALUE, 0, false);
        List<Map<String, Object>> result = query.results;
        Set<Map.Entry<String, String>> set = new HashSet<>();
        for (Map<String, Object> r: result) {
            set.add(new AbstractMap.SimpleEntry<>((String) r.get("key"), (String) r.get("value")));
        }
        return set;
    }

    @Override
    public void close() throws IOException {        
    }
}
