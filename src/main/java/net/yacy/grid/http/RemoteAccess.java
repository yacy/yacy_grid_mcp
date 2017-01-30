/**
 *  RemoteAccess
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import net.yacy.grid.mcp.Data;

/**
 * Storage of a peer list which can be used for peer-to-peer communication.
 * This is a static class to provide access to all other objects easily.
 * We store the IPs here only temporary, not permanently.
 */
public class RemoteAccess {

    public static Map<String, Map<String, RemoteAccess>> history = new ConcurrentHashMap<String, Map<String, RemoteAccess>>();
    private final static MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
    
    public static Query evaluate(final HttpServletRequest request) {
        try {request.setCharacterEncoding("UTF-8");} catch (UnsupportedEncodingException e){} // set character encoding before any request is made
        Map<String, String> qm = getQueryMap(request.getQueryString());
        Query post = new Query(request);
        post.initGET(qm);
        return post;
    }
    
    public static long latestVisit(String servlet, String remoteHost) {
        Map<String, RemoteAccess> hmap = history.get(servlet);
        if (hmap == null) {hmap = new ConcurrentHashMap<>(); history.put(servlet, hmap);}
        RemoteAccess ra = hmap.get(remoteHost);
        return ra == null ? -1 : ra.accessTime;
    }
    
    public static String hostHash(String remoteHost) {
        return Integer.toHexString(Math.abs(remoteHost.hashCode()));
    }
    
    private String remoteHost, localPath, peername;
    private int localHTTPPort, localHTTPSPort;
    private long accessTime;
    
    private RemoteAccess(final String remoteHost, final String localPath, final Integer localHTTPPort, final Integer localHTTPSPort, final String peername) {
        this.remoteHost = remoteHost;
        this.localPath = localPath;
        this.localHTTPPort = localHTTPPort == null ? -1 : localHTTPPort.intValue();
        this.localHTTPSPort = localHTTPSPort == null ? -1 : localHTTPSPort.intValue();
        this.peername = peername;
        this.accessTime = System.currentTimeMillis();
    }
    
    public String getRemoteHost() {
        return this.remoteHost;
    }
    
    public String getLocalPath() {
        return this.localPath;
    }

    public long getAccessTime() {
        return this.accessTime;
    }

    public int getLocalHTTPPort() {
        return this.localHTTPPort;
    }

    public int getLocalHTTPSPort() {
        return this.localHTTPSPort;
    }
    
    public String getPeername() {
        return this.peername;
    }
    
    private static Set<String> localhostNames = new HashSet<>();
    static {
        localhostNames.add("0:0:0:0:0:0:0:1");
        localhostNames.add("fe80:0:0:0:0:0:0:1%1");
        localhostNames.add("127.0.0.1");
        localhostNames.add("localhost");
        try {localhostNames.add(InetAddress.getLocalHost().getHostAddress());} catch (UnknownHostException e) {}
        try {localhostNames.add(InetAddress.getLocalHost().getHostName());} catch (UnknownHostException e) {}
        try {localhostNames.add(InetAddress.getLocalHost().getCanonicalHostName());} catch (UnknownHostException e) {}
        try {for (InetAddress a: InetAddress.getAllByName(null)) {localhostNames.add(a.getHostAddress()); localhostNames.add(a.getHostName()); localhostNames.add(a.getCanonicalHostName());}} catch (UnknownHostException e) {}
        try {for (InetAddress a: InetAddress.getAllByName("localhost")) {localhostNames.add(a.getHostAddress()); localhostNames.add(a.getHostName()); localhostNames.add(a.getCanonicalHostName());}} catch (UnknownHostException e) {}
        //System.out.println(localhostNames);
    }
    
    public static void addLocalhost(String h) {
        localhostNames.add(h);
    }
    
    public static boolean isLocalhost(String host) {
        return localhostNames.contains(host);
    }

    public static Map<String, String> getQueryMap(String query) {
        Map<String, String> map = new HashMap<String, String>();
        if (query == null) return map;
        String[] params = query.split("&");
        for (String param : params) {
            int p = param.indexOf('=');
            if (p >= 0) try {
                map.put(param.substring(0, p), URLDecoder.decode(param.substring(p + 1), "UTF-8"));
            } catch (UnsupportedEncodingException e) {}
        }  
        return map;  
    }
    
    public static Map<String, byte[]> getPostMap(HttpServletRequest request) throws IOException {
        Map<String, byte[]> map = new HashMap<>();
        Map<String, String[]> pm = request.getParameterMap();
        if (pm != null && pm.size() > 0) {
            for (Map.Entry<String, String[]> entry: pm.entrySet()) {
                String[] v = entry.getValue();
                if (v != null && v.length > 0) map.put(entry.getKey(), v[0].getBytes(StandardCharsets.UTF_8));
            }
        } else try {
            request.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement); // without that we get a IllegalStateException in getParts()
            final byte[] b = new byte[1024];
            for (Part part: request.getParts()) {
                String name = part.getName();
                InputStream is = part.getInputStream();
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int c;
                try {while ((c = is.read(b, 0, b.length)) > 0) {
                    baos.write(b, 0, c);
                }} finally {is.close();}
                map.put(name, baos.toByteArray());
            }
        } catch (IOException | ServletException | IllegalStateException e) {
            Data.logger.debug("Parsing of POST multipart failed", e);
            throw new IOException(e.getMessage());
        }
        return map;
    }
}
