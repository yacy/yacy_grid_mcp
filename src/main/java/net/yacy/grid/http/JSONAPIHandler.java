/**
 *  JSONAPIHandler
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

package net.yacy.grid.http;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONObject;

public class JSONAPIHandler {    

    // generic keywords in JSON keys of API responses
    public static final String SUCCESS_KEY   = "success";
    public static final String SERVICE_KEY   = "service";
    public static final String COMMENT_KEY   = "comment";
    
    // for messages
    public static final String MESSAGE_KEY   = "message";
    public static final String AVAILABLE_KEY = "available";
    
    /**
     * helper method to implement serviceImpl
     * @param params
     * @return
     * @throws IOException
     */
    public static String json2url(final JSONObject params) throws IOException {
        StringBuilder query = new StringBuilder();
        if (params != null) {
            Iterator<String> i = params.keys(); int c = 0;
            while (i.hasNext()) {
                String key = i.next();
                query.append(c == 0 ? '?' : '&');
                query.append(key);
                query.append('=');
                query.append(params.get(key));
                c++;
            }
        }
        return query.toString();
    }
    
}
