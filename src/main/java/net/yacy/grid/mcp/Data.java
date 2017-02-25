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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yacy.grid.io.assets.GridStorage;
import net.yacy.grid.io.db.JSONDatabase;
import net.yacy.grid.io.db.PeerDatabase;
import net.yacy.grid.io.messages.GridBroker;
import net.yacy.grid.io.messages.PeerBroker;

public class Data {
    
    public static File approot, gridServicePath;
    public static PeerDatabase peerDB;
    public static JSONDatabase peerJsonDB;
    public static GridBroker gridBroker;
    public static PeerBroker peerBroker;
    public static GridStorage gridStorage;
    public static Logger logger;
    public static Map<String, String> config;
    
    //public static Swagger swagger;
    
    public static void init(File root, File data, Map<String, String> cc) {
        logger = LoggerFactory.getLogger(Data.class);
        approot = root;
        config = cc;
        /*
        try {
            swagger = new Swagger(new File(new File(approot, "conf"), "swagger.json"));
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            e.printStackTrace();
        }
        */
        //swagger.getServlets().forEach(path -> System.out.println(swagger.getServlet(path).toString()));
        
        gridServicePath = data;
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
        
    }
    
    public static void close() {
        peerJsonDB.close();
        peerDB.close();
        
        peerBroker.close();
        gridBroker.close();
        
        gridStorage.close();
    }
    
}
