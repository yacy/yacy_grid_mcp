/**
 *  DeleteService
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
import net.yacy.grid.mcp.Service;

/**
 * tests:
 * http://127.0.0.1:8100/yacy/grid/mcp/index/delete.json?index=web&type=crawler&id=31bf58014628ee9e28b5ffb8b91ddf3e
 *
 */
public class DeleteService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 84232349879L;
    public static final String NAME = "delete";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/index/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {
        //String indexName, String typeName, final String id, JSONObject object
        final String indexName = call.get("index", "");
        final String typeName = call.get("type", "_doc"); // should not be null
        final String id = call.get("id", "");
        final QueryLanguage language = QueryLanguage.valueOf(call.get("language", "yacy"));
        final String query = call.get("query", "");
        final JSONObject json = new JSONObject(true);
        if (indexName.length() > 0 && typeName.length() > 0 && id.length() > 0) {
            try {
                final Index index = Service.instance.config.gridIndex.getElasticIndex();
                final String url = index.checkConnection().getConnectionURL();
                final boolean deleted = index.delete(indexName, typeName, id);
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                json.put("deleted", deleted);
                json.put("count", deleted ? 1 : 0);
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
            } catch (final IOException e) {
                json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
            }
        } else if (indexName.length() > 0 && typeName.length() > 0 && query.length() > 0) {
            try {
                final Index index = Service.instance.config.gridIndex.getElasticIndex();
                final String url = index.checkConnection().getConnectionURL();
                final long count = index.delete(indexName, language, query);
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                json.put("count", count);
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
            } catch (final IOException e) {
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
