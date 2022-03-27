/**
 *  JSONObjectAPIHandler
 *  Copyright 18.01.2017 by Michael Peter Christen, @orbiterlab
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
    public static final String MESSAGES_KEY  = "messages";
    public static final String COUNT_KEY     = "count";
    public static final String DELIVERY_TAG  = "deliveryTag";
    public static final String AVAILABLE_KEY = "available";

    /**
     * helper method to implement serviceImpl
     * @param params
     * @return
     * @throws IOException
     */
    public static String json2url(final JSONObject params) throws IOException {
        final StringBuilder query = new StringBuilder();
        if (params != null) {
            final Iterator<String> i = params.keys(); int c = 0;
            while (i.hasNext()) {
                final String key = i.next();
                query.append(c == 0 ? '?' : '&');
                query.append(key);
                query.append('=');
                query.append(URLEncoder.encode(params.get(key).toString(), "UTF-8"));
                c++;
            }
        }
        return query.toString();
    }

    public static Map<String, byte[]> json2map(final JSONObject params) throws IOException {
        final HashMap<String, byte[]> map = new HashMap<>();
        if (params != null) {
            final Iterator<String> i = params.keys();
            while (i.hasNext()) {
                final String key = i.next();
                map.put(key, params.get(key).toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    /**
     * POST request
     */
    @Override
    public ServiceResponse serviceImpl(final String protocolhostportstub, final JSONObject params) throws IOException {
        /*
        String urlstring = protocolhostportstub + this.getAPIPath() + json2url(params);
        ClientConnection connection = new ClientConnection(urlstring);
        return doConnection(connection);
        */
        final String urlstring = protocolhostportstub + this.getAPIPath();
        final ClientConnection connection = new ClientConnection(urlstring, json2map(params));
        return doConnection(connection);
    }

    /**
     * POST request
     */
    @Override
    public ServiceResponse serviceImpl(final String protocolhostportstub, final Map<String, byte[]> params) throws IOException {
        final String urlstring = protocolhostportstub + this.getAPIPath();
        final ClientConnection connection = new ClientConnection(urlstring, params);
        return doConnection(connection);
    }

    private ServiceResponse doConnection(final ClientConnection connection) throws IOException {
        final Charset charset = connection.getContentType().getCharset();
        final String mime = connection.getContentType().getMimeType(); //application/javascript, application/octet-stream
        final byte[] b = connection.load();
        if (b.length == 0) throw new IOException("response empty");
        if (mime.indexOf("javascript") >= 0) {
            if (b.length > 0 && b[0] == (byte) '[') {
                final JSONArray json = new JSONArray(new JSONTokener(new String(b, charset == null ? StandardCharsets.UTF_8 : charset)));
                return new ServiceResponse(json);
            } else {
                final JSONObject json = new JSONObject(new JSONTokener(new String(b, charset == null ? StandardCharsets.UTF_8 : charset)));
                return new ServiceResponse(json);
            }
        } else {
            // consider this is binary
            return new ServiceResponse(b);
        }
    }

}
