/**
 *  MCPLoaderThrottlingFactory
 *  Copyright 29.7.2019 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.control;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

import org.json.JSONObject;

import net.yacy.grid.http.APIServer;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Logger;
import net.yacy.grid.mcp.MCP;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.mcp.api.control.LoaderThrottlingService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.tools.MultiProtocolURL;

public class GridControl {

    private String mcp_host;
    private int mcp_port;

    private static Map<String, Long> loaderAccess = new ConcurrentHashMap<>();

    public GridControl() {
        this.mcp_host = null;
        this.mcp_port = 0;
    }

    public GridControl(String server, int port) {
        this.mcp_host = server;
        this.mcp_port = port;
    }

    public boolean connectMCP(String host, int port) {
        this.mcp_host = host;
        this.mcp_port = port;
        try {
            checkConnection();
            Logger.info(this.getClass(), "Index/Client: connected to MCP control at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getHost() {
        return this.mcp_host;
    }

    public int getPort() {
        return this.mcp_port;
    }

    public String getConnectionURL() {
        return "http://" + this.getHost() + ":" + this.getPort();
    }

    public void checkConnection() throws IOException {
        final Map<String, byte[]> params = new HashMap<>();
        String protocolhostportstub = GridControl.this.getConnectionURL();
        ServiceResponse sr = APIServer.getAPI(StatusService.NAME).serviceImpl(protocolhostportstub, params);
        if (!sr.getObject().has("system")) throw new IOException("MCP does not respond properly");
    }

    public static long computeThrottling(String id, String url, int depth, int crawlingDepth, boolean loaderHeadless, int priority) {
        MultiProtocolURL u;
        try {
            u = new MultiProtocolURL(url);
        } catch (MalformedURLException e) {
            return 500;
        }
        String host = u.getHost();
        Long lastLoadTime = loaderAccess.get(host);
        if (lastLoadTime == null) {
            // the host was never loaded!
            loaderAccess.put(host, System.currentTimeMillis());
            Logger.info("GridControl.computeThrottling: never-loaded " + host + ", delay = 0, url = " + url);
            return 0;
        }
        // compute access delta
        long delta = lastLoadTime.longValue() - System.currentTimeMillis();
        if (delta < 0) {
            // the latest load time was in the past
            loaderAccess.put(host, System.currentTimeMillis());
            long delay = Math.max(0, delta + 500); // in case that delta < -500, we don't need a throttling at all
            if (delay > 0) Logger.info("GridControl.computeThrottling: past-loaded " + host + ", delay = " + delay + ", url = " + url);
            return delay;
        }
        // the latest load time will be loaded by another thread in the future
        // we must add another delay to this
        long future = System.currentTimeMillis() + delta + 500;
        long delay = future - System.currentTimeMillis();
        loaderAccess.put(host, future);
        Logger.info("GridControl.computeThrottling: future-loading " + host + ", delay = " + delay + ", url = " + url);
        return delay; // == delta + 500
    }

    public long checkThrottling(String id, String url, int depth, int crawlingDepth, boolean loaderHeadless, int priority) throws IOException {
        final Map<String, byte[]> params = new HashMap<>();
        params.put("id", id.getBytes(StandardCharsets.UTF_8));
        params.put("url", url.getBytes(StandardCharsets.UTF_8));
        params.put("depth", Integer.toString(depth).getBytes(StandardCharsets.UTF_8));
        params.put("crawlingDepth", Integer.toString(crawlingDepth).getBytes(StandardCharsets.UTF_8));
        params.put("loaderHeadless", Boolean.toString(loaderHeadless).getBytes(StandardCharsets.UTF_8));
        params.put("priority", Integer.toString(priority).getBytes(StandardCharsets.UTF_8));
        String protocolhostportstub = GridControl.this.getConnectionURL();
        ServiceResponse sr = APIServer.getAPI(LoaderThrottlingService.NAME).serviceImpl(protocolhostportstub, params);
        JSONObject response = sr.getObject();
        if (response.has(ObjectAPIHandler.SUCCESS_KEY) && response.getBoolean(ObjectAPIHandler.SUCCESS_KEY)) {
            long delay = response.getLong("delay");
            //long time = response.getLong("time");
            return delay;
        } else {
            return 500;
        }
    }

    public static void main(String args[]) {
        // burn-in test
        List<Class<? extends Servlet>> services = new ArrayList<>();
        services.addAll(Arrays.asList(MCP.MCP_SERVICES));
        Service.initEnvironment(MCP.MCP_SERVICE, services, MCP.DATA_PATH, true);
        final GridControl loaderThrottling = new GridControl("127.0.0.1", 8100);
        long delay = 1000;
        try {
            delay = loaderThrottling.checkThrottling("123", "http://yacy.net", 2, 7, true, 0);
        } catch (IOException e) {
            e.printStackTrace();
            delay = 1001;
        }
        System.out.println(delay);
    }

}
