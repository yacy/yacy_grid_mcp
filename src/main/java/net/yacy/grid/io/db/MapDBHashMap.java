/**
 *  MapDBHashMap
 *  Copyright 15.01.2017 by Michael Peter Christen, @orbiterlab
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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDBHashMap implements CloseableMap<String, String> {

    private DB db;
    private HTreeMap<String, String> hashMap;
    
    public MapDBHashMap(File f) {
        this.db = DBMaker.fileDB(f).closeOnJvmShutdown().transactionEnable().make();
        this.hashMap = db.hashMap(f.getName())
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.STRING)
                .createOrOpen();
    }

    @Override
    public int size() {
        return this.hashMap.getSize();
    }

    @Override
    public boolean isEmpty() {
        return this.hashMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.hashMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.hashMap.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return this.hashMap.get(key);
    }

    @Override
    public String put(String key, String value) {
        return this.hashMap.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return this.hashMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        this.hashMap.putAll(m);
    }

    @Override
    public void clear() {
        this.hashMap.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> keySet() {
        return this.hashMap.keySet();
    }

    @Override
    public Collection<String> values() {
        return this.hashMap.getValues();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        return this.hashMap.entrySet();
    }

    @Override
    public void close() throws IOException {
        this.hashMap.close();
        this.db.close();
    }
}
