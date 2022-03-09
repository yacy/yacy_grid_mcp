/**
 *  MCPLoaderThrottlingFactory
 *  Copyright 29.7.2019 by Michael Peter Christen, @orbiterlab
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Configuration;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.mcp.api.control.LoaderThrottlingService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.MultiProtocolURL;

public class GridControl {

    private String mcp_host;
    private int mcp_port;

    private static Map<String, Long> loaderAccess = new ConcurrentHashMap<>();

    public GridControl() {
        this.mcp_host = null;
        this.mcp_port = 0;
    }

    public GridControl(final String server, final int port) {
        this.mcp_host = server;
        this.mcp_port = port;
    }

    public boolean connectMCP(final String host, final int port) {
        this.mcp_host = host;
        this.mcp_port = port;
        try {
            checkConnection();
            Logger.info(this.getClass(), "Index/Client: connected to MCP control at " + host + ":" + port);
            return true;
        } catch (final IOException e) {
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
        final String protocolhostportstub = GridControl.this.getConnectionURL();
        final ServiceResponse sr = Service.instance.config.getAPI(StatusService.NAME).serviceImpl(protocolhostportstub, params);
        if (!sr.getObject().has("status")) throw new IOException("MCP does not respond properly");
    }

    public static long computeThrottling(final String id, final String url, final int depth, final int crawlingDepth, final boolean loaderHeadless, final int priority) {
        MultiProtocolURL u;
        try {
            u = new MultiProtocolURL(url);
        } catch (final MalformedURLException e) {
            return 500;
        }
        final String host = u.getHost();
        final Long lastLoadTime = loaderAccess.get(host);
        if (lastLoadTime == null) {
            // the host was never loaded!
            loaderAccess.put(host, System.currentTimeMillis());
            Logger.info("GridControl.computeThrottling: never-loaded " + host + ", delay = 0, url = " + url);
            return 0;
        }
        // compute access delta
        final long delta = lastLoadTime.longValue() - System.currentTimeMillis();
        if (delta < 0) {
            // the latest load time was in the past
            loaderAccess.put(host, System.currentTimeMillis());
            final long delay = Math.max(0, delta + 500); // in case that delta < -500, we don't need a throttling at all
            if (delay > 0) Logger.info("GridControl.computeThrottling: past-loaded " + host + ", delay = " + delay + ", url = " + url);
            return delay;
        }
        // the latest load time will be loaded by another thread in the future
        // we must add another delay to this
        final long future = System.currentTimeMillis() + delta + 500;
        final long delay = future - System.currentTimeMillis();
        loaderAccess.put(host, future);
        Logger.info("GridControl.computeThrottling: future-loading " + host + ", delay = " + delay + ", url = " + url);
        return delay; // == delta + 500
    }

    public long checkThrottling(final String id, final String url, final int depth, final int crawlingDepth, final boolean loaderHeadless, final int priority) throws IOException {
        final Map<String, byte[]> params = new HashMap<>();
        params.put("id", id.getBytes(StandardCharsets.UTF_8));
        params.put("url", url.getBytes(StandardCharsets.UTF_8));
        params.put("depth", Integer.toString(depth).getBytes(StandardCharsets.UTF_8));
        params.put("crawlingDepth", Integer.toString(crawlingDepth).getBytes(StandardCharsets.UTF_8));
        params.put("loaderHeadless", Boolean.toString(loaderHeadless).getBytes(StandardCharsets.UTF_8));
        params.put("priority", Integer.toString(priority).getBytes(StandardCharsets.UTF_8));
        final String protocolhostportstub = GridControl.this.getConnectionURL();
        final ServiceResponse sr = Service.instance.config.getAPI(LoaderThrottlingService.NAME).serviceImpl(protocolhostportstub, params);
        final JSONObject response = sr.getObject();
        if (response.has(ObjectAPIHandler.SUCCESS_KEY) && response.getBoolean(ObjectAPIHandler.SUCCESS_KEY)) {
            final long delay = response.getLong("delay");
            //long time = response.getLong("time");
            return delay;
        } else {
            return 500;
        }
    }

    public static void main(final String args[]) {
        // burn-in test
        final Configuration config = new Configuration("data",  true, YaCyServices.mcp);
        final GridControl loaderThrottling = new GridControl("127.0.0.1", 8100);
        long delay = 1000;
        try {
            delay = loaderThrottling.checkThrottling("123", "http://yacy.net", 2, 7, true, 0);
        } catch (final IOException e) {
            e.printStackTrace();
            delay = 1001;
        }
        System.out.println(delay);
        config.close();
    }

}
