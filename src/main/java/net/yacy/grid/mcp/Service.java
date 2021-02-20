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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.log4j.BasicConfigurator;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIServer;
import net.yacy.grid.mcp.api.info.StatusService;
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
    private static int port = 0;
    public  static HazelcastInstance hazelcast = null;

    Service(final String[] fields) {
        this.fields = new LinkedHashSet<String>();
        for (String field: fields) this.fields.add(field);
    }

    public static void initEnvironment(
            final YaCyServices serviceType,
            final List<Class<? extends Servlet>> services,
            final String data_path,  // this is usually "data"
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

        // overwrite the config with environment variables. Because a '.' (dot) is not allowed in system environments
        // the dot can be replaced by "_" (underscore), i.e. like:
        // grid_broker_address="anonymous:yacy@127.0.0.1:5672" java -jar build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT.jar
        String[] keys = config.keySet().toArray(new String[config.size()]); // create a clone of the keys to prevent a ConcurrentModificationException
        for (String key: keys) if (System.getenv().containsKey(key.replace('.', '_'))) config.put(key, System.getenv().get(key.replace('.', '_')));

        // the config can further be overwritten by System Properties, i.e. like:
        // java -jar -Dgrid.broker.address="anonymous:yacy@127.0.0.1:5672" build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT.jar
        for (String key: keys) if (System.getProperties().containsKey(key)) config.put(key, System.getProperties().getProperty(key));

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

    public static int getPort() {
        return port == 0 ? Integer.parseInt(Data.config.get("port")) : port;
    }

    public static void runService(final String html_path) {

        // read the port again and then read also the configuration again because the path of the custom settings may have moved
        port = Integer.parseInt(Data.config.get("port"));

        // start server
        try {
            // open the server on available port
            boolean portForce = Boolean.getBoolean(Data.config.get("port.force"));
            port = APIServer.open(port, html_path, portForce);

            // give positive feedback
            Data.logger.info("Service started at port " + port);

            // start hazelcast service
            Config config = new Config().setClusterName("YaCyGrid").setInstanceName(Service.type.name());
            hazelcast = Hazelcast.newHazelcastInstance(config);
            String uuid = hazelcast.getCluster().getLocalMember().getUuid().toString();
            hazelcast.getMap("status").put(uuid, StatusService.status());

            // prepare shutdown signal
            boolean pidkillfileCreated = false;
            // we use two files: one kill file which can be used to stop the process and one pid file which exists until the process runs
            // in case that the deletion of the kill file does not cause a termination, still a "fuser -k" on the pid file can be used to
            // terminate the process.
            File pidfile = new File(data_dir, type.name() + "-" + port + ".pid");
            File killfile = new File(data_dir, type.name() + "-" + port + ".kill");
            if (pidfile.exists()) pidfile.delete();
            if (killfile.exists()) killfile.delete();
            if (!pidfile.exists()) try {
                pidfile.createNewFile();
                if (pidfile.exists()) {pidfile.deleteOnExit(); pidkillfileCreated = true;}
            } catch (IOException e) {
                Data.logger.info("pid file " + pidfile.getAbsolutePath() + " creation failed: " + e.getMessage());
            }
            if (!killfile.exists()) try {
                killfile.createNewFile();
                if (killfile.exists()) killfile.deleteOnExit(); else pidkillfileCreated = false;
            } catch (IOException e) {
                Data.logger.info("kill file " + killfile.getAbsolutePath() + " creation failed: " + e.getMessage());
                pidkillfileCreated = false;
            }

            // wait for shutdown signal (kill on process)
            if (pidkillfileCreated) {
                // we can control this by deletion of the kill file
                Data.logger.info("to stop this process, delete kill file " + killfile.getAbsolutePath());
                while (APIServer.isAlive() && killfile.exists()) {
                    try {Thread.sleep(1000);} catch (InterruptedException e) {}
                }
                APIServer.stop();
            } else {
                // something with the pid file creation did not work; fail-over to normal operation waiting for a kill command
                APIServer.join();
            }
            Data.logger.info("server nominal termination requested");
        } catch (IOException e) {
            Data.logger.error("Main fail", e);
        } finally {
            APIServer.stop();
            if (hazelcast != null) hazelcast.shutdown();
            Data.logger.info("closing data.");
            Data.close();
            Data.logger.info("server terminated.");
        }
    }

}
