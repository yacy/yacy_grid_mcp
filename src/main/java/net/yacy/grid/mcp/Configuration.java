/**
 *  Data
 *  Copyright 14.01.2017 by Michael Peter Christen, @orbiterlab
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

import org.apache.log4j.BasicConfigurator;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.io.assets.GridStorage;
import net.yacy.grid.io.control.GridControl;
import net.yacy.grid.io.db.JSONDatabase;
import net.yacy.grid.io.db.PeerDatabase;
import net.yacy.grid.io.index.BoostsFactory;
import net.yacy.grid.io.index.ElasticIndexFactory;
import net.yacy.grid.io.index.GridIndex;
import net.yacy.grid.io.messages.GridBroker;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.MapUtil;
import net.yacy.grid.tools.OS;

public class Configuration {

    public final File conf_dir, data_dir;
    public final File gridServicePath;
    public final PeerDatabase peerDB;
    public final JSONDatabase peerJsonDB;
    public final GridBroker gridBroker;
    public final GridStorage gridStorage;
    public GridIndex gridIndex;
    public final GridControl gridControl;
    public final Map<String, String> properties;
    public final BoostsFactory boostsFactory;
    public final YaCyServices type;
    public final HazelcastInstance hazelcast;
    public final List<Class<? extends Servlet>> servlets;
    public final Map<String, APIHandler> serviceMap;

    //public Data(final File serviceData, final Service service, final boolean localStorage) {
    public Configuration(
            final String data_path,  // this is usually "data"
            final boolean localStorage,
            final YaCyServices serviceType,
            final Class<? extends Servlet>... servlets) {

        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // configure logging
        BasicConfigurator.configure();

        // load the config file(s);
        // what we are doing here is a bootstraping of configuration file(s): first we load the system configuration
        // then we know the port for the service. As every server for a specific port may have its own configuration
        // file we need to load the configuration again.
        this.type = serviceType;
        this.conf_dir = FileSystems.getDefault().getPath("conf").toFile();
        this.data_dir = FileSystems.getDefault().getPath(data_path).toFile();
        this.properties = readDoubleProps("config.properties");

        // log config
        for (final Map.Entry<String, String> centry: this.properties.entrySet()) {
            final String key = centry.getKey();
            final boolean pw = key.toLowerCase().contains("password");
            Logger.info("CONFIG: " + key + " = " + (pw ? "***" : centry.getValue()));
        }

        this.gridServicePath = dataInstancePath(serviceType.getDefaultPort());
        if (!this.gridServicePath.exists()) this.gridServicePath.mkdirs();

        // create databases
        final File dbPath = new File(this.gridServicePath, "db");
        if (!dbPath.exists()) dbPath.mkdirs();
        this.peerDB = new PeerDatabase(dbPath);
        this.peerJsonDB = new JSONDatabase(this.peerDB);

        // create broker
        final File messagesPath = new File(this.gridServicePath, "messages");
        if (!messagesPath.exists()) messagesPath.mkdirs();
        final boolean lazy = this.properties.containsKey("grid.broker.lazy") && this.properties.get("grid.broker.lazy").equals("true");
        final boolean autoAck = this.properties.containsKey("grid.broker.autoAck") && this.properties.get("grid.broker.autoAck").equals("true");
        final int queueLimit = this.properties.containsKey("grid.broker.queue.limit") ? Integer.parseInt(this.properties.get("grid.broker.queue.limit")) : 0;
        final int queueThrottling = this.properties.containsKey("grid.broker.queue.throttling") ? Integer.parseInt(this.properties.get("grid.broker.queue.throttling")) : 0;
        this.gridBroker = new GridBroker(localStorage ? messagesPath : null, lazy, autoAck, queueLimit, queueThrottling);

        // create storage
        final File assetsPath = new File(this.gridServicePath, "assets");
        final boolean deleteafterread = this.properties.containsKey("grid.assets.delete") && this.properties.get("grid.assets.delete").equals("true");
        this.gridStorage = new GridStorage(deleteafterread, localStorage ? assetsPath : null);

        // create index
        this.gridIndex = new GridIndex();

        // create control
        this.gridControl = new GridControl();

        // check network situation
        try {
            Logger.info("Local Host Address: " + InetAddress.getLocalHost().getHostAddress());
        } catch (final UnknownHostException e1) {
            e1.printStackTrace();
        }

        // connect outside services
        // first try to connect to the configured MCPs.
        // if that fails, try to make all connections self
        final String gridMcpAddressl = this.properties.containsKey("grid.mcp.address") ? this.properties.get("grid.mcp.address") : "";
        final String[] gridMcpAddress = gridMcpAddressl.split(",");
        boolean mcpConnected = false;
        for (final String address: gridMcpAddress) {
            if (address.length() <= 0) continue;
            Logger.info("Attempting Grid Connection to " + address);

            final String host = getHost(address);
            final int port = YaCyServices.mcp.getDefaultPort();
            Logger.info("Checking Broker connection at " + host + ":" + port);
            final boolean brokerConnected = this.gridBroker.connectMCP(host, port);
            if (!brokerConnected) {Logger.warn("..failed"); continue;}
            Logger.info(".. ok, RabbitMQ connected: " + this.gridBroker.isRabbitMQConnected());

            Logger.info("Checking Storage connection at " + host + ":" + port);
            final boolean storageConnected = this.gridStorage.connectMCP(host, port, true);
            if (!storageConnected) {Logger.warn("..failed"); continue;}
            Logger.info(".. ok, S3 connected: " + this.gridStorage.isS3Connected());

            Logger.info("Checking Index connection at " + host + ":" + port);
            final boolean indexConnected = this.gridIndex.connectMCP(host, port);
            if (!indexConnected) {Logger.warn("..failed"); continue;}
            Logger.info(".. ok, Elastic connected: " + this.gridIndex.isConnected());

            Logger.info("Checking Control connection at " + host + ":" + port);
            final boolean controlConnected = this.gridControl.connectMCP(host, port);
            if (!controlConnected) {Logger.warn("..failed"); continue;}
            Logger.info(".. ok, Control connected: " + this.gridControl.getConnectionURL() + " with url "+ this.gridControl.getConnectionURL());

            Logger.info("Connected MCP at " + getHost(address));
            mcpConnected = true;
            break;
        }

        if (!mcpConnected) {
            // try to connect to local services directly

            // connect broker
            final String[] gridBrokerAddress = (this.properties.containsKey("grid.broker.address") ? this.properties.get("grid.broker.address") : "").split(",");
            for (final String address: gridBrokerAddress) {
                if (!OS.portIsOpen(address)) continue;
                if (this.gridBroker.connectRabbitMQ(getHost(address), getPort(address, "-1"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Logger.info("Connected Broker at " + getHost(address));
                    break;
                }
            }
            if (!this.gridBroker.isRabbitMQConnected()) {
                Logger.info("Connected to the embedded Broker");
            }

            // connect storage
            // s3
            final String[] gridS3Address = (this.properties.containsKey("grid.s3.address") ? this.properties.get("grid.s3.address") : "").split(",");
            final boolean  gridS3Active = this.properties.containsKey("grid.s3.active") ? "true".equals(this.properties.get("grid.s3.active")) : true;
            for (final String address: gridS3Address) {
                if (address.length() > 0 && this.gridStorage.connectS3(getHost(address) /*bucket.endpoint*/, getPort(address, "9000"), getUser(address, "admin"), getPassword(address, "12345678"), gridS3Active)) {
                    Logger.info("Connected S3 Storage at " + getHost(address));
                    break;
                }
            }

            // ftp
            final String[] gridFtpAddress = (this.properties.containsKey("grid.ftp.address") ? this.properties.get("grid.ftp.address") : "").split(",");
            final boolean  gridFtpActive = this.properties.containsKey("grid.ftp.active") ? "true".equals(this.properties.get("grid.ftp.active")) : true;
            for (final String address: gridFtpAddress) {
                if (address.length() > 0 && this.gridStorage.connectFTP(getHost(address), getPort(address, "2121"), getUser(address, "admin"), getPassword(address, "admin"), gridFtpActive)) {
                    Logger.info("Connected FTP Storage at " + getHost(address));
                    break;
                }
            }

            // if there is no ftp and no s3 connection, we use a local asset storage
            if (!this.gridStorage.isFTPConnected() && !this.gridStorage.isS3Connected()) {
                Logger.info("Connected to the embedded Asset Storage");
            }

            // connect index
            final String[] elasticsearchAddress = this.properties.getOrDefault("grid.elasticsearch.address", "").split(",");
            final String elasticsearchClusterName = this.properties.getOrDefault("grid.elasticsearch.clusterName", "");
            final String elasticsearchTypeName = this.properties.getOrDefault("grid.elasticsearch.typeName", "_doc");
            for (final String address: elasticsearchAddress) {
                if (!OS.portIsOpen(address)) continue;
                try {
                    this.gridIndex = new GridIndex();
                    this.gridIndex.connectElasticsearch(ElasticIndexFactory.PROTOCOL_PREFIX + address + "/" + elasticsearchClusterName);
                    break;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // find connections first here before concurrent threads try to make their own connection concurrently
        try { this.gridIndex.checkConnection(); } catch (final IOException e) { Logger.error("no connection to MCP", e); }

        // init boosts from configuration
        final Map<String, String> defaultBoosts = readDoubleProps("boost.properties");
        this.boostsFactory = new BoostsFactory(defaultBoosts);

        // start hazelcast service
        final Config hazelcastconfig = new Config().setClusterName("YaCyGrid").setInstanceName("Services");
        this.hazelcast = Hazelcast.newHazelcastInstance(hazelcastconfig);
        final String uuid = this.hazelcast.getCluster().getLocalMember().getUuid().toString();
        this.hazelcast.getMap("status").put(uuid, StatusService.status());

        // initialize serviceMap for server
        this.servlets = new ArrayList<>();
        this.serviceMap = new ConcurrentHashMap<>();
        for (final Class<? extends Servlet> servlet: servlets) addServlet(servlet);
    }

    /**
     * read the configuration two times: first to determine the port
     * and the second time to get the configuration from that specific port-related configuration
     * @param confFileName
     * @return
     */
    public Map<String, String> readDoubleProps(final String confFileName) {
        File user_dir = new File(dataInstancePath( this.type.getDefaultPort()) , "conf");
        Map<String, String> config = MapUtil.readConfig(this.conf_dir, user_dir, confFileName);

        // read the port again and then read also the configuration again because the path of the custom settings may have moved
        if (config.containsKey("port")) {
            final int port = Integer.parseInt(config.get("port"));
            user_dir = new File(dataInstancePath(port) , "conf");
            config = MapUtil.readConfig(this.conf_dir, user_dir, confFileName);
        }
        return config;
    }

    private File dataInstancePath(final int port) {
        return new File(this.data_dir, this.type.name() + "-" + port);
    }

    public static String getHost(final String address) {
        final String hp = t(address, '@', address);
        return h(hp, ':', hp);
    }
    public static int getPort(final String address, final String defaultPort) {
        return Integer.parseInt(t(t(address, '@', address), ':', defaultPort));
    }
    public static String getUser(final String address, final String defaultUser) {
        return h(h(address, '@', ""), ':', defaultUser);
    }
    public static String getPassword(final String address, final String defaultPassword) {
        return t(h(address, '@', ""), ':', defaultPassword);
    }

    private static String h(final String a, final char s, final String d) {
        final int p = a.indexOf(s);
        return p < 0 ? d : a.substring(0,  p);
    }

    private static String t(final String a, final char s, final String d) {
        final int p = a.indexOf(s);
        return p < 0 ? d : a.substring(p + 1);
    }

    public void clearCaches() {
        // should i.e. be called in case of short memory status
        Logger.clean(5000);
    }

    public void addServlet(final Class<? extends Servlet> servlet) {
        try {
            final APIHandler handler = (APIHandler) servlet.newInstance();
            this.servlets.add(servlet);
            this.serviceMap.put(handler.getAPIName(), handler);
        } catch (InstantiationException | IllegalAccessException e) {
            Logger.warn(e);
        }
    }

    public APIHandler getAPI(final String name) {
        return this.serviceMap.get(name);
    }

    public void close() {
        if (this.peerDB != null) this.peerDB.close();
        if (this.peerJsonDB != null) this.peerJsonDB.close();
        if (this.gridBroker != null) this.gridBroker.close();
        if (this.gridStorage != null) this.gridStorage.close();
        if (this.gridIndex != null) this.gridIndex.close();
        if (this.hazelcast != null) this.hazelcast.shutdown();
    }

}
