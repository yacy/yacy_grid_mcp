/**
 *  AvailableService
 *  Copyright 28.1.2017 by Michael Peter Christen, @0rb1t3r
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
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONTokener;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.messages.AvailableContainer;
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.io.messages.MessageContainer;
import net.yacy.grid.mcp.Data;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/peek.json?serviceName=crawler&queueName=webcrawler_00
 */
public class PeekService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749889L;
    public static final String NAME = "peek";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/messages/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        String serviceName = call.get("serviceName", "");
        String queueName = call.get("queueName", "");
        JSONObject json = new JSONObject(true);
        if (serviceName.length() > 0 && queueName.length() > 0) {
            try {
                YaCyServices service = YaCyServices.valueOf(serviceName);
                GridQueue queue = new GridQueue(queueName);
                AvailableContainer available = Data.gridBroker.available(service, queue);
                int ac = available.getAvailable();
                String url = available.getFactory().getConnectionURL();
                if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
                if (ac > 0) {
                    // load one message and send it right again to prevent that it is lost
                    MessageContainer<byte[]> message = Data.gridBroker.receive(service, queue, 3000);
                    // message can be null if a timeout occurred
                    if (message == null) {
                        json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                        json.put(ObjectAPIHandler.COMMENT_KEY, "timeout");
                    } else {
                        // send it again asap!
                        Data.gridBroker.send(service, queue, message.getPayload());
                        // evaluate whats inside
                        String payload = message.getPayload() == null ? null : new String(message.getPayload(), StandardCharsets.UTF_8);
                        JSONObject payloadjson = payload == null ? null : new JSONObject(new JSONTokener(payload));
                        json.put(ObjectAPIHandler.AVAILABLE_KEY, ac);
                        json.put(ObjectAPIHandler.MESSAGE_KEY, payloadjson == null ? new JSONObject() : payloadjson);
                        json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                    }
                } else {
                    json.put(ObjectAPIHandler.AVAILABLE_KEY, 0);
                    json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                }
            } catch (IOException e) {
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
