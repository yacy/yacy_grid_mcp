/**
 *  MCP
 *  Copyright 14.01.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Servlet;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.info.ServicesService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.mcp.api.messages.AvailableService;
import net.yacy.grid.mcp.api.messages.ReceiveService;
import net.yacy.grid.mcp.api.messages.SendService;

public class MCP {

    private final static YaCyServices SERVICE = YaCyServices.mcp;
    private final static String DATA_PATH = "data";
    private final static String APP_PATH = "mcp";
 
    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] MCP_SERVICES = new Class[]{
            // information services
            ServicesService.class,
            StatusService.class,

            // message services
            SendService.class,
            ReceiveService.class,
            AvailableService.class,

            // asset services
            //RetrieveService.class,
            StoreService.class,
            LoadService.class
    };
    
    public static void main(String[] args) {
        List<Class<? extends Servlet>> services = new ArrayList<>();
        services.addAll(Arrays.asList(MCP_SERVICES));
        Service.runService(SERVICE, DATA_PATH, APP_PATH, services);
    }

}
