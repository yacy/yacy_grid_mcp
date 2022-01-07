/**
 *  LoaderThrottlingService
 *  Copyright 29.07.2019 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp.api.control;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.control.GridControl;

/**
 * The loader throttling Service:
 * called, when a URL is supposed to be loaded.
 * The MCP centrally coordinates the loading begin times for each loader thread.
 * This is done in such a way that the urls from the same host are not loaded too often
 * to avoid a DoS situation at the target host.
 * call http://localhost:8100/yacy/grid/mcp/control/loaderThrottling.json?url=klg.de
 */
public class LoaderThrottlingService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749479L;
    public static final String NAME = "loaderThrottling";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/control/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {

        // get loader attributes
        String id = call.get("id", ""); // the crawl id
        String url = call.get("url", "");
        int depth = call.get("depth", 0);
        int crawlingDepth = call.get("crawlingDepth", 0); // the maximum depth for the crawl start of this domain
        boolean loaderHeadless = call.get("loaderHeadless", false);
        int priority = call.get("priority", 0);

        // generate json
        JSONObject json = new JSONObject(true);
        json.put("delay", GridControl.computeThrottling(id, url, depth, crawlingDepth, loaderHeadless, priority));
        json.put("time", System.currentTimeMillis() + 500);
        json.put(ObjectAPIHandler.SUCCESS_KEY, true);
        json.put(ObjectAPIHandler.COMMENT_KEY, "");
        return new ServiceResponse(json);
    }

}
