/**
 *  JSONDatabase
 *  Copyright 16.01.2017 by Michael Peter Christen, @orbiterlab
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

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONDatabase {

    private Database db;
    
    public JSONDatabase(Database backeddb) {
        this.db = backeddb;
    }
    
    public long size(String serviceName, String tableName) throws IOException {
        return this.db.size(serviceName, tableName);
    }
    
    public void write(String serviceName, String tableName, String id, JSONObject value) throws IOException {
        this.db.write(serviceName, tableName, id, value.toString(0));
    }

    public JSONObject read(String serviceName, String tableName, final String id) throws IOException {
        String s = this.db.read(serviceName, tableName, id);
        JSONTokener t = new JSONTokener(s);
        return new JSONObject(t);
    }

    public boolean exist(String serviceName, String tableName, final String id) throws IOException {
        return this.db.exist(serviceName, tableName, id);
    }

    public boolean delete(String serviceName, String tableName, final String id) throws IOException {
        return this.db.delete(serviceName, tableName, id);
    }
    
    public JSONArray export(String serviceName, String tableName) throws IOException {
        JSONArray a = new JSONArray();
        Iterator<String> idi = this.db.ids(serviceName, tableName);
        while (idi.hasNext()) {
            String id = idi.next();
            JSONObject j = read(serviceName, tableName, id);
            if (j != null) a.put(j);
        }
        return a;
    }
    
    public void close() {
        try {this.db.close();} catch (IOException e) {}
    }
    
}
