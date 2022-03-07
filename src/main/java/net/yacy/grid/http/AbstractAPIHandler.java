/**
 *  AbstractAPIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @orbiterlab and Robert Mader, @treba123
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
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.tools.Logger;

@SuppressWarnings("serial")
public abstract class AbstractAPIHandler extends HttpServlet implements APIHandler {

    public static final Long defaultCookieTime = (long) (60 * 60 * 24 * 7);
    public static final Long defaultAnonymousTime = (long) (60 * 60 * 24);

    public AbstractAPIHandler() {
    }

    @Override
    public abstract ServiceResponse serviceImpl(final String protocolhostportstub, JSONObject params) throws IOException;
    @Override
    public abstract ServiceResponse serviceImpl(final String protocolhostportstub, Map<String, byte[]> params) throws IOException;
    @Override
    public abstract ServiceResponse serviceImpl(Query call, HttpServletResponse response) throws APIException;

    @Override
    public String getAPIName() {
        String path = this.getAPIPath();
        int p = path.lastIndexOf('/');
        if (p >= 0) path = path.substring(p + 1);
        p = path.indexOf('.');
        if (p >= 0) path = path.substring(0, p);
        return path;
    }

    private void setCORS(final HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "accept, content-type");
    }

    @Override
    protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        super.doOptions(request, response);
        setCORS(response); // required by angular framework; detailed CORS can be set within the servlet
    }

    @Override
    protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        super.doHead(request, response);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final Query post = RemoteAccess.evaluate(request);
        process(request, response, post);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final Query query = RemoteAccess.evaluate(request);
        query.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, query);
    }

    private void process(final HttpServletRequest request, final HttpServletResponse response, final Query query) throws ServletException, IOException {

        final long startTime = System.currentTimeMillis();

        // extract standard query attributes
        final String callback = query.get("callback", "");
        final boolean jsonp = callback.length() > 0;
        final boolean minified = query.get("minified", false);

        try {
            final ServiceResponse serviceResponse = serviceImpl(query, response);
            if  (serviceResponse == null) {
                final String message = "your request does not contain the required data";
                logClient(startTime, query, 400, message);
                response.sendError(400, message);
                return;
            }
            if (serviceResponse.allowCORS()) {
                setCORS(response);
            }

            // write json
            query.setResponse(response, serviceResponse.getMimeType());
            response.setCharacterEncoding("UTF-8");
            if (serviceResponse.isObject() || serviceResponse.isArray()) {
                final PrintWriter sos = response.getWriter();
                if (jsonp) sos.print(callback + "(");
                final String out = serviceResponse.toString(minified);
                sos.print(out);
                if (jsonp) sos.println(");");
                sos.println();
                logClient(startTime, query, 200, "ok: " + (minified ? out : serviceResponse.toString(true)));
            } else if (serviceResponse.isString()) {
                final PrintWriter sos = response.getWriter();
                final String out = serviceResponse.toString(false);
                sos.print(out);
                logClient(startTime, query, 200, "ok: " + out);
            } else if (serviceResponse.isByteArray()) {
                response.getOutputStream().write(serviceResponse.getByteArray());
                response.setHeader("Access-Control-Allow-Origin", "*");
                logClient(startTime, query, 200, "ok (ByteArray)");
            }
        } catch (final APIException e) {
            final String message = e.getMessage();
            logClient(startTime, query, e.getStatusCode(), message);
            response.sendError(e.getStatusCode(), message);
            return;
        }
    }

    public void logClient(
            final long startTime,
            final Query query,
            final int httpResponseCode,
            String message) {
        final String host = query.getClientHost();
        String q = query.toString(512);
        if (q.length() > 512) q = q.substring(0, 512) + "...";
        final long t = System.currentTimeMillis() - startTime;
        String path = getAPIPath();
        if (q.length() > 0) path = path + "?" + q;
        if (message.length() > 512) message = message.substring(0, 512) + "...";
        final String m = host + " - " + httpResponseCode + " - " + t + "ms - " + path + " - " + message;
        Logger.info(this.getClass(), m);
    }
}
