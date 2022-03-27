/**
 *  QueuesService
 *  Copyright 27.03.2022 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp.api.messages;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;

/**
 * QueuesService
 * Does not require any parameter, it produces a list of queue names for each service that can be retrieved by the messages apis
 * i.e.:
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/queues.json
 */
public class QueuesService  extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578475243031749879L;
    public static final String NAME = "queues";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/messages/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {
        final JSONObject json = new JSONObject(true);
        final Map<String, String[]> map = YaCyServices.getServiceQueueArrayMap();
        json.put(ObjectAPIHandler.SUCCESS_KEY, true);
        final JSONObject services = new JSONObject(true);
        json.put("services", services);
        for (final Map.Entry<String, String[]> entry: map.entrySet()) {
            final JSONArray queues = new JSONArray();
            for (int j = 0; j < entry.getValue().length; j++) {
                queues.put(entry.getValue()[j]);
            }
            services.put(entry.getKey(), queues);
        }
        return new ServiceResponse(json);
    }

}
