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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
    public static LogAppender logAppender;
    private static ElasticsearchClient index = null; // will be initialized on-the-fly
    
    //public static Swagger swagger;
    
    public static void init(File serviceData, Map<String, String> cc) {
        PatternLayout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %p %c %x - %m%n");
        logger = Logger.getRootLogger();
        logger.removeAllAppenders();
        logAppender = new LogAppender(layout, 100000);
        logger.addAppender(logAppender);
        logger.addAppender(new ConsoleAppender(layout));
        
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
        peerBroker = new PeerBroker(messagesPath);
        gridBroker = new GridBroker(messagesPath);
        
        // create storage
        File assetsPath = new File(gridServicePath, "assets");
        boolean deleteafterread = cc.containsKey("grid.assets.delete") && cc.get("grid.assets.delete").equals("true");
        gridStorage = new GridStorage(deleteafterread, assetsPath);
        
        // connect outside services
        // first try to connect to the configured MCPs.
        // if that fails, try to make all connections self
        String gridMcpAddressl = config.containsKey("grid.mcp.address") ? config.get("grid.mcp.address") : "";
        String[] gridMcpAddress = gridMcpAddressl.split(",");
        boolean mcpConnected = false;
        for (String address: gridMcpAddress) {
            if (    address.length() > 0 &&
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
            String[] gridBrokerAddress = (config.containsKey("grid.broker.address") ? config.get("grid.broker.address") : "").split(",");
            for (String address: gridBrokerAddress) {
                if (Data.gridBroker.connectRabbitMQ(getHost(address), getPort(address, "-1"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Data.logger.info("Connected Broker at " + getHost(address));
                    break;
                }
            }
            if (!Data.gridBroker.isRabbitMQConnected()) {
                Data.logger.info("Connected to the embedded Broker");
            }
            String[] gridFtpAddress = (config.containsKey("grid.ftp.address") ? config.get("grid.ftp.address") : "").split(",");
            for (String address: gridFtpAddress) {
                if (address.length() > 0 && Data.gridStorage.connectFTP(getHost(address), getPort(address, "2121"), getUser(address, "anonymous"), getPassword(address, "yacy"))) {
                    Data.logger.info("Connected Storage at " + getHost(address));
                    break;
                }
            }
            if (!Data.gridStorage.isFTPConnected()) {
                Data.logger.info("Connected to the embedded Asset Storage");
            }
        }
        
    }
    
    public static ElasticsearchClient getIndex() {
        if (index == null) {
            // create index
            String elasticsearchAddress = config.getOrDefault("grid.elasticsearch.address", "localhost:9300");
            String elasticsearchClusterName = config.getOrDefault("grid.elasticsearch.clusterName", "");
            String elasticsearchWebIndexName= config.getOrDefault("grid.elasticsearch.webIndexName", "web");
            Path webMappingPath = Paths.get("conf/mappings/web.json");
            if (webMappingPath.toFile().exists()) try {
                index = new ElasticsearchClient(new String[]{elasticsearchAddress}, elasticsearchClusterName.length() == 0 ? null : elasticsearchClusterName);
                index.createIndexIfNotExists(elasticsearchWebIndexName, 1 /*shards*/, 1 /*replicas*/);
                String mapping = new String(Files.readAllBytes(webMappingPath));
                JSONObject mo = new JSONObject(new JSONTokener(mapping));
                mo = mo.getJSONObject("mappings").getJSONObject("_default_");
                index.setMapping("web", mo.toString());
                Data.logger.info("Connected elasticsearch at " + getHost(elasticsearchAddress));
            } catch (IOException | NoNodeAvailableException e) {
                index = null; // index not available
                Data.logger.info("Failed connecting elasticsearch at " + getHost(elasticsearchAddress) + ": " + e.getMessage(), e);
            } else {
                Data.logger.info("no web index mapping available, no connection to elasticsearch attempted");
            }
        }
        return index;
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
        logAppender.clean(5000);
        
    }
    
    public static void close() {
        peerJsonDB.close();
        peerDB.close();
        
        peerBroker.close();
        gridBroker.close();
        
        gridStorage.close();
    }
    
}
