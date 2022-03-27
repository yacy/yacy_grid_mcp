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

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.io.messages.MessageContainer;
import net.yacy.grid.mcp.Service;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/peek.json?count=10&serviceName=loader&queueName=webloader_10
 *
 * Names of queues can be found in YaCyServices:
 * crawler_webcrawler_00 - 07
 * indexer_elasticsearch_00
 * loader_webloader_00 - 31
 * parser_yacyparser_00
 *
 * compare with http://localhost:15672/#/queues
 */
public class PeekService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749889L;
    public static final String NAME = "peek";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/messages/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {
        final String serviceName = call.get("serviceName", "");
        final String queueName = call.get("queueName", "");
        final String counts = call.get("count", "1");
        int count = Integer.parseInt(counts);
        final JSONObject json = new JSONObject(true);
        final JSONArray messageArray = new JSONArray();
        json.put(ObjectAPIHandler.MESSAGES_KEY, messageArray);
        if (serviceName.length() > 0 && queueName.length() > 0) {
            final YaCyServices service = YaCyServices.valueOf(serviceName);
            final GridQueue queue = new GridQueue(queueName);
            count = Math.min(100, count);
            final List<MessageContainer> messages = Service.instance.config.gridBroker.peek(service, queue, count);
            for (final MessageContainer message: messages) {
                final String payload = message.getPayload() == null ? null : new String(message.getPayload(), StandardCharsets.UTF_8);
                final JSONObject payloadjson = payload == null ? null : new JSONObject(new JSONTokener(payload));
                if (payloadjson != null) messageArray.put(payloadjson);
            }
            json.put(ObjectAPIHandler.SUCCESS_KEY, true);
        } else {
            json.put(ObjectAPIHandler.SUCCESS_KEY, false);
            json.put(ObjectAPIHandler.COMMENT_KEY, "the request must contain a serviceName and a queueName");
        }
        json.put(ObjectAPIHandler.COUNT_KEY, messageArray.length());
        return new ServiceResponse(json);
    }
}
