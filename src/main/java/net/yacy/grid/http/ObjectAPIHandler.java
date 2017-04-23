/**
 *  JSONObjectAPIHandler
 *  Copyright 18.01.2017 by Michael Peter Christen, @0rb1t3r
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public abstract class ObjectAPIHandler extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -2191240526448018368L;

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
    
    /**
     * GET request
     */
    public ServiceResponse serviceImpl(final String protocolhostportstub, JSONObject params) throws IOException {
        String urlstring = protocolhostportstub + this.getAPIPath() + json2url(params);
        ClientConnection connection = new ClientConnection(urlstring);
        return doConnection(connection);
    }
    
    /**
     * POST request
     */
    public ServiceResponse serviceImpl(final String protocolhostportstub, Map<String, byte[]> params) throws IOException {
        String urlstring = protocolhostportstub + this.getAPIPath();
        ClientConnection connection = new ClientConnection(urlstring, params);
        return doConnection(connection);
    }
    
    private ServiceResponse doConnection(ClientConnection connection) throws IOException {
        Charset charset = connection.getContentType().getCharset();
        String mime = connection.getContentType().getMimeType(); //application/javascript, application/octet-stream
        byte[] b = connection.load();
        if (mime.indexOf("javascript") >= 0) {
            if (b.length > 0 && b[0] == (byte) '[') {
                JSONArray json = new JSONArray(new JSONTokener(new String(b, charset == null ? StandardCharsets.UTF_8 : charset)));
                return new ServiceResponse(json);
            } else {
                JSONObject json = new JSONObject(new JSONTokener(new String(b, charset == null ? StandardCharsets.UTF_8 : charset)));
                return new ServiceResponse(json);
            }
        } else {
            // consider this is binary
            return new ServiceResponse(b);
        }
    }
    
}
