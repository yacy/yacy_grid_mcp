/**
 *  LocalDatabase
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

package net.yacy.grid.io.db;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerDatabase implements Database {

    private File basePath;
    private Map<String, TableFactory> dbConnector;
    
    public PeerDatabase(File basePath) {
        this.basePath = basePath;
        this.dbConnector = new ConcurrentHashMap<>();
    }
    
    private TableFactory getConnector(String serviceName) {
        TableFactory tableFactory = this.dbConnector.get(serviceName);
        if (tableFactory != null)  return tableFactory;
        synchronized (this) {
            tableFactory = this.dbConnector.get(serviceName);
            if (tableFactory != null)  return tableFactory;
            File clientPath = new File(this.basePath, serviceName);
            clientPath.mkdirs();
            tableFactory = new MapDBTableFactory(clientPath);
            this.dbConnector.put(serviceName, tableFactory);
        }
        return tableFactory;
    }
    
    @Override
    public void close() {
        this.dbConnector.values().forEach(db -> db.close());
    }

    @Override
    public long size(String serviceName, String tableName) throws IOException {
        TableFactory dbc = getConnector(serviceName);
        return dbc.getTable(tableName).size();
    }

    @Override
    public void write(String serviceName, String tableName, String id, String value) throws IOException {
        getConnector(serviceName).getTable(tableName).write(id, value);
    }

    @Override
    public String read(String serviceName, String tableName, String id) throws IOException {
        return getConnector(serviceName).getTable(tableName).read(id);
    }

    @Override
    public boolean exist(String serviceName, String tableName, String id) throws IOException {
        return getConnector(serviceName).getTable(tableName).exist(id);
    }

    @Override
    public boolean delete(String serviceName, String tableName, String id) throws IOException {
        return getConnector(serviceName).getTable(tableName).delete(id);
    }

    @Override
    public Iterator<String> ids(String serviceName, String tableName) throws IOException {
        return getConnector(serviceName).getTable(tableName).iterator();
    }

}
