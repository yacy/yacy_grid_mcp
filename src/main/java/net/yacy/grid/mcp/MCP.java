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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import javax.servlet.Servlet;

import org.apache.log4j.BasicConfigurator;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.info.ServicesService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.mcp.api.messages.AvailableService;
import net.yacy.grid.mcp.api.messages.ReceiveService;
import net.yacy.grid.mcp.api.messages.SendService;

public class MCP {
    
    public static void main(String[] args) {
        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // configure logging
        BasicConfigurator.configure();
        
        
        // define service port
        int port = YaCyServices.mcp.getDefaultPort();

        // define services
        @SuppressWarnings("unchecked")
        Class<? extends Servlet>[] services = new Class[]{
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

        // start server
        APIServer.init(services);
        try {
            // open the server on available port
            port = APIServer.open(port, true);

            // find home path
            File home = FileSystems.getDefault().getPath(".").toFile();
            Data.init(home, "mcp-" + port);
            
            // connect outside services
            // TODO make addresses configurable
            if (port == YaCyServices.mcp.getDefaultPort()) {
                // primary mcp services try to connect to local services directly
                Data.gridBroker.connectRabbitMQ("127.0.0.1", -1);
                Data.gridStorage.connectFTP("127.0.0.1", 2121, "anonymous", "yacy");
            } else {
                // secondary mcp services try connect to the primary mcp which then tells
                // us where to connect to directly
                Data.gridBroker.connectMCP("127.0.0.1", YaCyServices.mcp.getDefaultPort());
                Data.gridStorage.connectMCP("127.0.0.1", YaCyServices.mcp.getDefaultPort());
            }
            
            // give positive feedback
            Data.logger.info("Service started at port " + port);
            
            // prepare shutdown signal
            File pid = new File(Data.dataPath, "mcp-" + port + ".pid");
            if (pid.exists()) pid.delete(); // clean up rubbish
            pid.createNewFile();
            pid.deleteOnExit();
            
            // wait for shutdown signal (kill on process)
            APIServer.join();
        } catch (IOException e) {
            Data.logger.error("Main fail", e);
        }
        
        Data.close();
    }
    
}
