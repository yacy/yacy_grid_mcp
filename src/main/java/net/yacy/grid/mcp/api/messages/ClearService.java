/**
 *  ClearService
 *  Copyright 04.01.2018 by Michael Peter Christen, @orbiterlab
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
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.io.messages.QueueFactory;
import net.yacy.grid.mcp.Service;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/clear.json?serviceName=loader&queueName=webloader_00
 */
public class ClearService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749879L;
    public static final String NAME = "clear";

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
                final QueueFactory qf = Service.instance.config.gridBroker.clear(YaCyServices.valueOf(serviceName), new GridQueue(queueName));
                final String url = qf.getConnectionURL();
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
