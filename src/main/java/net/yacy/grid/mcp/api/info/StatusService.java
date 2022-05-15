/**
 *  StatusService
 *  Copyright 27.02.2015 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp.api.info;

import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hazelcast.cluster.Member;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.tools.OS;

// test: http://localhost:8100/yacy/grid/mcp/info/status.json
public class StatusService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749479L;
    public static final String NAME = "status";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/info/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {

        // generate json
        final JSONObject json = new JSONObject(true);
        final JSONObject systemStatus = new JSONObject(true);
        systemStatus.putAll(new JSONObject(status()));
        final JSONArray members = new JSONArray();
        if (Service.instance.config.hazelcast != null) {
            for (final Member member: Service.instance.config.hazelcast.getCluster().getMembers()) {
                final JSONObject m = new JSONObject(true);
                final String uuid = member.getUuid().toString();
                m.put("uuid", uuid);
                m.put("host", member.getAddress().getHost());
                try {m.put("ip", member.getAddress().getInetAddress().getHostAddress());} catch (JSONException | UnknownHostException e) {}
                m.put("port", member.getAddress().getPort());
                m.put("isLite", member.isLiteMember());
                m.put("isLocal", member.localMember());
                @SuppressWarnings("unchecked")
                final Map<String, Object> status = (Map<String, Object>) Service.instance.config.hazelcast.getMap("status").get(uuid);
                m.put("status", status);
                members.put(m);
            }
            systemStatus.put("hazelcast_cluster_name", Service.instance.config.hazelcast.getConfig().getClusterName());
            systemStatus.put("hazelcast_instance_name", Service.instance.config.hazelcast.getConfig().getInstanceName());
            systemStatus.put("hazelcast_members", members);
            systemStatus.put("hazelcast_members_count", members.length());
        }

        final JSONObject client_info = new JSONObject(true);
        final JSONObject request_header = new JSONObject(true);
        client_info.put("request_header", request_header);

        json.put("status", systemStatus);
        json.put("client_info", client_info);

        return new ServiceResponse(json);
    }

    public static Map<String, Object> status() {
        final Runtime runtime = Runtime.getRuntime();
        final Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", Service.instance == null ? "" : Service.instance.config.type.name());
        status.put("assigned_memory", runtime.maxMemory());
        status.put("used_memory", runtime.totalMemory() - runtime.freeMemory());
        status.put("available_memory", runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory());
        status.put("cores", runtime.availableProcessors());
        status.put("threads", Thread.activeCount());
        status.put("deadlocks", OS.deadlocks());
        status.put("load_system_average", OS.getSystemLoadAverage());
        //system.put("load_system_cpu", OS.getSystemCpuLoad());
        status.put("load_process_cpu", OS.getProcessCpuLoad());
        status.put("server_threads", Service.instance == null ? 0 : Service.instance.getServerThreads());
        return status;
    }

}
