/**
 *  Action
 *  Copyright 28.01.2017 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.contracts;

import org.json.JSONObject;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.APIServer;

/**
 * An action is the request to a YaCy Grid service to process the Contract with the specific
 * abilities of that service. That ability is addressed using the service name and some query attributes
 * that shall be passed to the services servlet.
 */
public class Action {

    private final static String KEY_SERVLET  = "servlet";
    private final static String KEY_QUERY    = "query";

    private JSONObject json;

    public Action() {
        this.json = new JSONObject();
    }

    public Action(JSONObject json) {
        this.json = json;
    }

    public JSONObject toJSONClone() {
        JSONObject j = new JSONObject(true);
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key))); // make a clone
        return j;
    }

    public Action setServletName(String name) {
        this.json.put(KEY_SERVLET, name);
        return this;
    }

    public String getServletName() {
        if (!this.json.has(KEY_SERVLET)) return null;
        return this.json.getString(KEY_SERVLET);
    }

    public APIHandler getServlet() {
        if (!this.json.has(KEY_SERVLET)) return null;
        APIHandler handler = APIServer.getAPI(this.json.getString(KEY_SERVLET));
        return handler;
    }

    public Action setQuery(JSONObject query) {
        this.json.put(KEY_QUERY, query);
        return this;
    }

    public JSONObject getQuery() {
        if (this.json.has(KEY_QUERY)) return this.json.getJSONObject(KEY_QUERY);
        JSONObject q = new JSONObject(true);
        this.json.put(KEY_QUERY, q);
        return q;
    }

    public String toString() {
        return toJSONClone().toString();
    }
}
