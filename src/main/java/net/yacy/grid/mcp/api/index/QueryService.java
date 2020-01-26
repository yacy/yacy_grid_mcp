/**
 *  QueryObjectService
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

package net.yacy.grid.mcp.api.index;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.index.Index;
import net.yacy.grid.io.index.Index.QueryLanguage;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.JSONList;

/**
 * test:
 * http://127.0.0.1:8100/yacy/grid/mcp/index/query.json?index=web&query=now
 * http://127.0.0.1:8100/yacy/grid/mcp/index/query.json?index=web&id=31bf58014628ee9e28b5ffb8b91ddf3e
 */
public class QueryService extends ObjectAPIHandler implements APIHandler {
    private static final long serialVersionUID = 84232347733L;
    public static final String NAME = "query";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/index/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        //String indexName, String typeName, final String id, JSONObject object
        String indexName = call.get("index", "");
        String id = call.get("id", "");
        QueryLanguage language = QueryLanguage.valueOf(call.get("language", "yacy"));
        String query = call.get("query", "");
        int maximumRecords = call.get("maximumRecords", call.get("rows", call.get("num", 10)));
        int startRecord = call.get("startRecord", call.get("start", 0));
        JSONObject json = new JSONObject(true);
        if (indexName.length() > 0 && id.length() > 0) {
            try {
                Index index = Data.gridIndex.getElasticIndex();
                String url = index.checkConnection().getConnectionURL();
                JSONObject object = index.query(indexName, id);
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                JSONList list = new JSONList();
                if (object != null) list.add(object);
                json.put("count", list.length());
                json.put("list", list.toArray());
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
            } catch (IOException e) {
                json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
            }
        } else if (indexName.length() > 0 && query.length() > 0) {
            try {
                Index index = Data.gridIndex.getElasticIndex();
                String url = index.checkConnection().getConnectionURL();
                JSONList list = index.query(indexName, language, query, startRecord, maximumRecords);
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                json.put("count", list.length());
                json.put("list", list.toArray());
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
            } catch (IOException e) {
                json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
            }
        } else {
            json.put(ObjectAPIHandler.SUCCESS_KEY, false);
            json.put(ObjectAPIHandler.COMMENT_KEY, "the request must contain an index, type, and either an id or a query");
        }
        return new ServiceResponse(json);
    }
}
