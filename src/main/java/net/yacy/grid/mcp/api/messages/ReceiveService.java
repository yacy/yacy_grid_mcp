/**
 *  ReceiveService
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

import net.yacy.grid.QueueName;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.messages.MessageContainer;
import net.yacy.grid.mcp.Data;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/messages/receive.json?serviceName=testService&queueName=testQueue
 */
public class ReceiveService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749879L;
    public static final String NAME = "receive";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/messages/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        String serviceName = call.get("serviceName", "");
        String queueName = call.get("queueName", "");
        long timeout = call.get("timeout", -1);
        JSONObject json = new JSONObject(true);
        if (serviceName.length() > 0 && queueName.length() > 0) {
            try {
                MessageContainer<byte[]> message = Data.gridBroker.receive(YaCyServices.valueOf(serviceName), new QueueName(queueName), timeout);
                // message can be null if a timeout occurred
                if (message == null) {
                    json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                    json.put(ObjectAPIHandler.COMMENT_KEY, "timeout");
                } else {
                    String url = message.getFactory().getConnectionURL();
                    byte[] payload = message.getPayload();
                    json.put(ObjectAPIHandler.MESSAGE_KEY, payload == null ? "" : new String(payload, StandardCharsets.UTF_8));
                    json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                    if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
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
