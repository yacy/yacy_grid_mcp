/**
 *  AvailableService
 *  Copyright 28.1.2017 by Michael Peter Christen, @orbiterlab
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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.messages.AvailableContainer;
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.mcp.Service;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/available.json?serviceName=crawler&queueName=webcrawler_00
 */
public class AvailableService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749879L;
    public static final String NAME = "available";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/messages/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {
        final String serviceName = call.get("serviceName", "");
        final String queueName = call.get("queueName", "");
        final JSONObject json = new JSONObject(true);
        if (serviceName.length() > 0 && queueName.length() > 0) {
            try {
                final AvailableContainer available = Service.instance.config.gridBroker.available(YaCyServices.valueOf(serviceName), new GridQueue(queueName));
                final String url = available.getFactory().getConnectionURL();
                json.put(ObjectAPIHandler.AVAILABLE_KEY, available.getAvailable());
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
            } catch (final IOException e) {
                json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
            }
        } else {
            json.put(ObjectAPIHandler.SUCCESS_KEY, false);
            json.put(ObjectAPIHandler.COMMENT_KEY, "the request must contain a serviceName and a queueName");
        }
        return new ServiceResponse(json);
    }
}
