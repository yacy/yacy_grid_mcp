/**
 *  CrawlService
 *  Copyright 18.07.2018 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.mcp.api.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIException;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.messages.AvailableContainer;
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.DateParser;

/**
 * test with
 * http://127.0.0.1:8100/yacy/grid/mcp/admin/inquirysubmit.json?userid=itsme&host=yacy.net
 */
public class InquirySubmitService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = -18449008500061149L;
    public static final String NAME = "inquirysubmit";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/admin/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) throws APIException {
        String userid = call.get("userid", "");
        String action = call.get("action", "crawl");
        String host = call.get("host", "");
        String depth = call.get("depth", "3");
        JSONObject json = new JSONObject(true);
        if (userid.length() > 0 && action.length() > 0 && host.length() > 0) {
            long time = System.currentTimeMillis();
            String messageid = Integer.toHexString(Long.toString(time).hashCode() >> 1 + call.hashCode() >> 1);
            JSONObject message = new JSONObject(true);
            message.put("userid", userid);
            message.put("messageid", messageid);
            message.put("time", time);
            message.put("date", DateParser.formatRFC1123(new Date(time)));
            message.put("action", action);
            message.put("host", host);
            message.put("depth", Integer.parseInt(depth));
            try {
                GridQueue queue = new GridQueue("inquiry_open");
                Data.gridBroker.send(YaCyServices.mcp, queue, message.toString().getBytes(StandardCharsets.UTF_8));
                AvailableContainer available = Data.gridBroker.available(YaCyServices.mcp, queue);
                long queuepos = available.getAvailable();
                json.put(ObjectAPIHandler.SUCCESS_KEY, true);
                json.put(ObjectAPIHandler.COMMENT_KEY, "inquiry enqueued. Your request is on position " + queuepos);
                json.put("message", message);
                json.put("position", queuepos);
                json.put("messageid", messageid);
            } catch (IOException e) {
                json.put(ObjectAPIHandler.SUCCESS_KEY, false);
                json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
            }
        } else {
            json.put(ObjectAPIHandler.SUCCESS_KEY, false);
            json.put(ObjectAPIHandler.COMMENT_KEY, "the parameter userid and host must be given");
        }
        return new ServiceResponse(json);
    }

}
