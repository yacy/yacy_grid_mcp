/**
 *  AbstractAPIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r and Robert Mader, @treba123
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
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.json.JSONObject;

@SuppressWarnings("serial")
public abstract class AbstractAPIHandler extends HttpServlet implements APIHandler {

    public static final Long defaultCookieTime = (long) (60 * 60 * 24 * 7);
    public static final Long defaultAnonymousTime = (long) (60 * 60 * 24);

    private final HttpResponseFactory defaultHttpResponseFactory = new DefaultHttpResponseFactory();
    private final BasicHttpContext basicHttpContext = new BasicHttpContext();
    private final HttpCoreContext httpCoreContext = HttpCoreContext.adapt(basicHttpContext);
    private final ProtocolVersion httpProtocol = new ProtocolVersion("HTTP", 1, 1);
    private final HttpResponse defaultHttpServletResponse = defaultHttpResponseFactory.newHttpResponse(httpProtocol, HttpStatus.SC_CONTINUE, httpCoreContext);
    
    public AbstractAPIHandler() {
    }

    public abstract ServiceResponse serviceImpl(final String protocolhostportstub, JSONObject params) throws IOException;
    public abstract ServiceResponse serviceImpl(final String protocolhostportstub, Map<String, byte[]> params) throws IOException;
    public abstract ServiceResponse serviceImpl(Query call, HttpServletResponse response) throws APIException;
    
    public ServiceResponse serviceImpl(JSONObject params) throws APIException {
        Query call = new Query(null).initGET(params);
        return serviceImpl(call, (HttpServletResponse) defaultHttpServletResponse);
    }

    public ServiceResponse serviceImpl(Map<String, byte[]> params) throws APIException {
        Query call = new Query(null).initPOST(params);
        return serviceImpl(call, (HttpServletResponse) defaultHttpServletResponse);
    }
    
    public String getAPIName() {
        String path = this.getAPIPath();
        int p = path.lastIndexOf('/');
        if (p >= 0) path = path.substring(p + 1);
        p = path.indexOf('.');
        if (p >= 0) path = path.substring(0, p);
        return path;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        process(request, response, post);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query query = RemoteAccess.evaluate(request);
        query.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, query);
    }
    
    private void process(HttpServletRequest request, HttpServletResponse response, Query query) throws ServletException, IOException {
        
        // extract standard query attributes
        String callback = query.get("callback", "");
        boolean jsonp = callback.length() > 0;
        boolean minified = query.get("minified", false);
        
        try {
            ServiceResponse serviceResponse = serviceImpl(query, response);
            if  (serviceResponse == null) {
                response.sendError(400, "your request does not contain the required data");
                return;
            }
            
            // write json
            query.setResponse(response, serviceResponse.getMimeType());
            response.setCharacterEncoding("UTF-8");
            if (serviceResponse.isObject() || serviceResponse.isArray()) {
                PrintWriter sos = response.getWriter();
                if (jsonp) sos.print(callback + "(");
                sos.print(serviceResponse.toString(minified));
                if (jsonp) sos.println(");");
                sos.println();
            } else if (serviceResponse.isString()) {
                PrintWriter sos = response.getWriter();
                sos.print(serviceResponse.toString(false));
            } else if (serviceResponse.isByteArray()) {
                response.getOutputStream().write(serviceResponse.getByteArray());
            }
        } catch (APIException e) {
            response.sendError(e.getStatusCode(), e.getMessage());
            return;
        }
    }
    
       
    /**
     * Creates a random alphanumeric string
     * @param length
     * @return
     */
    public static String createRandomString(Integer length){
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Delete the login cookie if present
     * @param response
     */
    protected void deleteLoginCookie(HttpServletResponse response){
        Cookie deleteCookie = new Cookie("login", null);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);
    }
}
