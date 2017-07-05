/**
 *  Data
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.GridStorage;
import net.yacy.grid.io.db.JSONDatabase;
import net.yacy.grid.io.db.PeerDatabase;
import net.yacy.grid.io.index.ElasticsearchClient;
import net.yacy.grid.io.messages.GridBroker;
import net.yacy.grid.io.messages.PeerBroker;

public class Data {
    
    public static File gridServicePath;
    public static PeerDatabase peerDB;
    public static JSONDatabase peerJsonDB;
    public static GridBroker gridBroker;
    public static PeerBroker peerBroker;
    public static GridStorage gridStorage;
    public static Logger logger;
    public static Map<String, String> config;
    public static ElasticsearchClient index;
    
    //public static Swagger swagger;
    
    public static void init(File serviceData, Map<String, String> cc) {
        logger = LoggerFactory.getLogger(Data.class);
        config = cc;
        /*
        try {
            swagger = new Swagger(new File(new File(approot, "conf"), "swagger.json"));
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            e.printStackTrace();
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
        peerBroker = new PeerBroker(messagesPath);
        gridBroker = new GridBroker(messagesPath);
        
        // create storage
        File assetsPath = new File(gridServicePath, "assets");
        gridStorage = new GridStorage(assetsPath);
 
        // create index
        String elasticsearchAddress = config.getOrDefault("grid.elasticsearch.address", "localhost:9300");
        String elasticsearchClusterName = config.getOrDefault("grid.elasticsearch.clusterName", "");
        String elasticsearchWebIndexName= config.getOrDefault("grid.elasticsearch.webIndexName", "web");
        //index = new ElasticsearchClient(new String[]{elasticsearchAddress}, elasticsearchClusterName);
        index = new ElasticsearchClient(new String[]{elasticsearchAddress}, elasticsearchClusterName.length() == 0 ? null : elasticsearchClusterName);
        try {
            index.createIndexIfNotExists(elasticsearchWebIndexName, 1 /*shards*/, 1 /*replicas*/);
            String mapping = new String(Files.readAllBytes(Paths.get("conf/mappings/web.json")));
            index.setMapping("web", mapping);
            Data.logger.info("Connected elasticsearch at " + getHost(elasticsearchAddress));
        } catch (IOException | NoNodeAvailableException e) {
            index = null; // index not available
            e.printStackTrace();
            Data.logger.info("Failed connecting elasticsearch at " + getHost(elasticsearchAddress) + ": " + e.getMessage(), e);
        }
        
        // connect outside services
        // first try to connect to the configured MCPs.
        // if that fails, try to make all connections self
        String[] gridMcpAddress = config.get("grid.mcp.address").split(",");
        boolean mcpConnected = false;
        for (String address: gridMcpAddress) {
            if (
                    Data.gridBroker.connectMCP(getHost(address), YaCyServices.mcp.getDefaultPort()) &&
                    Data.gridStorage.connectMCP(getHost(address), YaCyServices.mcp.getDefaultPort())
                ) {
                Data.logger.info("Connected MCP at " + getHost(address));
                mcpConnected = true;
                break;
            }
        }
        
        if (!mcpConnected) {
            // try to connect to local services directly
            String[] gridBrokerAddress = config.get("grid.broker.address").split(",");
            for (String address: gridBrokerAddress) {
                if (Data.gridBroker.connectRabbitMQ(getHost(address), getPort(address, "-1"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Data.logger.info("Connected Broker at " + getHost(address));
                    break;
                }
            }
            if (!Data.gridBroker.isRabbitMQConnected()) {
                Data.logger.info("Connected to the embedded Broker");
            }
            String[] gridFtpAddress = config.get("grid.ftp.address").split(",");
            for (String address: gridFtpAddress) {
                if (Data.gridStorage.connectFTP(getHost(address), getPort(address, "2121"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Data.logger.info("Connected Storage at " + getHost(address));
                    break;
                }
            }
            if (!Data.gridStorage.isFTPConnected()) {
                Data.logger.info("Connected to the embedded Asset Storage");
            }
        }
        
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
    
    public static void close() {
        peerJsonDB.close();
        peerDB.close();
        
        peerBroker.close();
        gridBroker.close();
        
        gridStorage.close();
    }
    
}
