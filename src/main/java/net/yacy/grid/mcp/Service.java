/**
 *  Service
 *  Copyright 16.01.2017 by Michael Peter Christen, @0rb1t3r
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.log4j.BasicConfigurator;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.tools.MapUtil;

/**
 * The services enum contains information about all services implemented in this
 * grid element. It is the name of the servlets and their result object set.
 * This enum class should be replaced with a Swagger definition.
 */
public enum Service {

    Services(new String[]{
            "network",     // the name of the network
            "service",     // the YaCy Grid type of the service
            "name",        // an identifier which the owner of the service can assign as they want
            "host",        // host-name or IP address of the service
            "port",        // port on on the server where the service is running
            "publichost",  // host name which the service names as public access point, may be different from the recognized host access (backward routing may be different)
            "lastseen",    // ISO 8601 Time of the latest contact of the mcp to the service. Empty if the service has never been seen.
            "lastping"     // ISO 8601 Time of the latest contact of the service to the mcp
    });
    
    public static YaCyServices type = null;
    private final Set<String> fields;
    public static File conf_dir;
    public static File data_dir;
    
    Service(final String[] fields) {
        this.fields = new LinkedHashSet<String>();
        for (String field: fields) this.fields.add(field);
    }
    
    public static void initEnvironment(
            final YaCyServices serviceType,
            final List<Class<? extends Servlet>> services,
            final String data_path,
            final boolean localStorage) {
        type = serviceType;
        
        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // configure logging
        BasicConfigurator.configure();
        
        // load the config file(s);
        // what we are doing here is a bootstraping of configuration file(s): first we load the system configuration
        // then we know the port for the service. As every server for a specific port may have its own configuration
        // file we need to load the configuration again.
        conf_dir = FileSystems.getDefault().getPath("conf").toFile();
        data_dir = FileSystems.getDefault().getPath(data_path).toFile();
        Map<String, String> config = readDoubleConfig("config.properties");
        
        // define services
        services.forEach(service -> APIServer.addService(service));
        
        // find data path
        int port = Integer.parseInt(config.get("port"));
        Data.init(dataInstancePath(data_dir, port), config, localStorage);
    }

    /**
     * read the configuration two times: first to determine the port
     * and the second time to get the configuration from that specific port-related configuration
     * @param confFileName
     * @return
     */
    public static Map<String, String> readDoubleConfig(String confFileName) {
        File user_dir = new File(dataInstancePath(data_dir, type.getDefaultPort()) , "conf");
        Map<String, String> config = MapUtil.readConfig(conf_dir, user_dir, confFileName);
        
        // read the port again and then read also the configuration again because the path of the custom settings may have moved
        if (config.containsKey("port")) {
            int port = Integer.parseInt(config.get("port"));
            user_dir = new File(dataInstancePath(data_dir, port) , "conf");
            config = MapUtil.readConfig(conf_dir, user_dir, confFileName);
        }
        return config;
    }
    
    private static File dataInstancePath(File data_dir, int port) {
        return new File(data_dir, type.name() + "-" + port);
    }
    
    public static void runService(final String html_path) {
        
        // read the port again and then read also the configuration again because the path of the custom settings may have moved
        int port = Integer.parseInt(Data.config.get("port"));

        // start server
        try {            
            // open the server on available port
            boolean portForce = Boolean.getBoolean(Data.config.get("port.force"));
            port = APIServer.open(port, html_path, portForce);

            // give positive feedback
            Data.logger.info("Service started at port " + port);

            // prepare shutdown signal
            File pid = new File(data_dir, type.name() + "-" + port + ".pid");
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
