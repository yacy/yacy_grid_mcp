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
import java.util.Map;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.GridStorage;
import net.yacy.grid.io.control.GridControl;
import net.yacy.grid.io.db.JSONDatabase;
import net.yacy.grid.io.db.PeerDatabase;
import net.yacy.grid.io.index.BoostsFactory;
import net.yacy.grid.io.index.ElasticIndexFactory;
import net.yacy.grid.io.index.GridIndex;
import net.yacy.grid.io.messages.GridBroker;
import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.OS;

public class Data {

    public static File gridServicePath;
    public static PeerDatabase peerDB;
    public static JSONDatabase peerJsonDB;
    public static GridBroker gridBroker;
    public static GridStorage gridStorage;
    public static GridIndex gridIndex;
    public static GridControl gridControl;
    public static Map<String, String> config;
    public static BoostsFactory boostsFactory;

    //public static Swagger swagger;

    public static void init(File serviceData, Map<String, String> cc, boolean localStorage) {

        //final LogManager logManager = LogManager.getLogManager();
        config = cc;
        /*
        try {
            swagger = new Swagger(new File(new File(approot, "conf"), "swagger.json"));
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
        }
        */
        //swagger.getServlets().forEach(path -> System.out.println(swagger.getServlet(path).toString()));

        gridServicePath = serviceData;
        if (!gridServicePath.exists()) gridServicePath.mkdirs();

        // create databases
        File dbPath = new File(gridServicePath, "db");
        if (!dbPath.exists()) dbPath.mkdirs();
        peerDB = new PeerDatabase(dbPath);
        peerJsonDB = new JSONDatabase(peerDB);

        // create broker
        File messagesPath = new File(gridServicePath, "messages");
        if (!messagesPath.exists()) messagesPath.mkdirs();
        boolean lazy = config.containsKey("grid.broker.lazy") && config.get("grid.broker.lazy").equals("true");
        boolean autoAck = config.containsKey("grid.broker.autoAck") && config.get("grid.broker.autoAck").equals("true");
        int queueLimit = config.containsKey("grid.broker.queue.limit") ? Integer.parseInt(config.get("grid.broker.queue.limit")) : 0;
        int queueThrottling = config.containsKey("grid.broker.queue.throttling") ? Integer.parseInt(config.get("grid.broker.queue.throttling")) : 0;
        gridBroker = new GridBroker(localStorage ? messagesPath : null, lazy, autoAck, queueLimit, queueThrottling);

        // create storage
        File assetsPath = new File(gridServicePath, "assets");
        boolean deleteafterread = cc.containsKey("grid.assets.delete") && cc.get("grid.assets.delete").equals("true");
        gridStorage = new GridStorage(deleteafterread, localStorage ? assetsPath : null);

        // create index
        gridIndex = new GridIndex();

        // create control
        gridControl = new GridControl();

        // check network situation
        try {
            Logger.info("Local Host Address: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }

        // connect outside services
        // first try to connect to the configured MCPs.
        // if that fails, try to make all connections self
        String gridMcpAddressl = config.containsKey("grid.mcp.address") ? config.get("grid.mcp.address") : "";
        String[] gridMcpAddress = gridMcpAddressl.split(",");
        boolean mcpConnected = false;
        for (String address: gridMcpAddress) {
        	if (address.length() == 0) continue;
            String host = getHost(address);
            int port = YaCyServices.mcp.getDefaultPort();
            if (    address.length() > 0 &&
                    Data.gridBroker.connectMCP(host, port) &&
                    Data.gridStorage.connectMCP(host, port, true) &&
                    Data.gridIndex.connectMCP(host, port) &&
                    Data.gridControl.connectMCP(host, port)
                ) {
                Logger.info("Connected MCP at " + getHost(address));
                mcpConnected = true;
                break;
            }
        }

        if (!mcpConnected) {
            // try to connect to local services directly

            // connect broker
            String[] gridBrokerAddress = (config.containsKey("grid.broker.address") ? config.get("grid.broker.address") : "").split(",");
            for (String address: gridBrokerAddress) {
                if (!OS.portIsOpen(address)) continue;
                if (Data.gridBroker.connectRabbitMQ(getHost(address), getPort(address, "-1"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Logger.info("Connected Broker at " + getHost(address));
                    break;
                }
            }
            if (!Data.gridBroker.isRabbitMQConnected()) {
                Logger.info("Connected to the embedded Broker");
            }

            // connect storage
            // s3
            String[] gridS3Address = (config.containsKey("grid.s3.address") ? config.get("grid.s3.address") : "").split(",");
            boolean  gridS3Active = config.containsKey("grid.s3.active") ? "true".equals(config.get("grid.s3.active")) : true;
            for (String address: gridS3Address) {
                if (address.length() > 0 && Data.gridStorage.connectS3(getHost(address) /*bucket.endpoint*/, getPort(address, "9000"), getUser(address, "admin"), getPassword(address, "12345678"), gridS3Active)) {
                    Logger.info("Connected S3 Storage at " + getHost(address));
                    break;
                }
            }

            // ftp
            String[] gridFtpAddress = (config.containsKey("grid.ftp.address") ? config.get("grid.ftp.address") : "").split(",");
            boolean  gridFtpActive = config.containsKey("grid.ftp.active") ? "true".equals(config.get("grid.ftp.active")) : true;
            for (String address: gridFtpAddress) {
                if (address.length() > 0 && Data.gridStorage.connectFTP(getHost(address), getPort(address, "2121"), getUser(address, "admin"), getPassword(address, "admin"), gridFtpActive)) {
                    Logger.info("Connected FTP Storage at " + getHost(address));
                    break;
                }
            }

            // if there is no ftp and no s3 connection, we use a local asset storage
            if (!Data.gridStorage.isFTPConnected() && !Data.gridStorage.isS3Connected()) {
                Logger.info("Connected to the embedded Asset Storage");
            }

            // connect index
            String[] elasticsearchAddress = config.getOrDefault("grid.elasticsearch.address", "").split(",");
            String elasticsearchClusterName = config.getOrDefault("grid.elasticsearch.clusterName", "");
            String elasticsearchTypeName = config.getOrDefault("grid.elasticsearch.typeName", "_doc");
            for (String address: elasticsearchAddress) {
                if (!OS.portIsOpen(address)) continue;
                try {
                    gridIndex = new GridIndex();
                    gridIndex.connectElasticsearch(ElasticIndexFactory.PROTOCOL_PREFIX + address + "/" + elasticsearchClusterName);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // find connections first here before concurrent threads try to make their own connection concurrently
        try { Data.gridIndex.checkConnection(); } catch (IOException e) { Logger.error("no connection to MCP", e); }

        // init boosts from configuration
        Map<String, String> defaultBoosts = Service.readDoubleConfig("boost.properties");
        boostsFactory = new BoostsFactory(defaultBoosts);
    }

    public static String getHost(String address) {
        String hp = t(address, '@', address);
        return h(hp, ':', hp);
    }
    public static int getPort(String address, String defaultPort) {
        return Integer.parseInt(t(t(address, '@', address), ':', defaultPort));
    }
    public static String getUser(String address, String defaultUser) {
        return h(h(address, '@', ""), ':', defaultUser);
    }
    public static String getPassword(String address, String defaultPassword) {
        return t(h(address, '@', ""), ':', defaultPassword);
    }

    private static String h(String a, char s, String d) {
        int p = a.indexOf(s);
        return p < 0 ? d : a.substring(0,  p);
    }

    private static String t(String a, char s, String d) {
        int p = a.indexOf(s);
        return p < 0 ? d : a.substring(p + 1);
    }

    public static void clearCaches() {
        // should i.e. be called in case of short memory status
        Logger.clean(5000);

    }

    public static void close() {
        peerJsonDB.close();
        peerDB.close();
        gridBroker.close();
        gridStorage.close();
        gridIndex.close();
    }

}
