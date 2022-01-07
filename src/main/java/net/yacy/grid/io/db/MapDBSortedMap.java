/**
 *  MapDBSortedMap
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

package net.yacy.grid.io.db;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import net.yacy.grid.tools.Logger;

public class MapDBSortedMap implements NavigableCloseableMap<Long, byte[]>, CloseableMap<Long, byte[]> {

    private DB db;
    private BTreeMap<Long, byte[]> treeMap;

    public MapDBSortedMap(File f) {
        this.db = DBMaker.fileDB(f).closeOnJvmShutdown().transactionEnable().make();
        this.treeMap = this.db.treeMap(f.getName())
                .keySerializer(Serializer.LONG)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .createOrOpen();
    }

    @Override
    public int size() {
        return this.treeMap.getSize();
    }

    @Override
    public boolean isEmpty() {
        return this.treeMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.treeMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.treeMap.containsValue(value);
    }

    @Override
    public byte[] get(Object key) {
        return this.treeMap.get(key);
    }

    @Override
    public byte[] put(Long key, byte[] value) {
        byte[] b = this.treeMap.put(key, value);
    	this.db.commit();
    	return b;
    }

    @Override
    public byte[] remove(Object key) {
    	byte[] b = this.treeMap.remove(key);
    	this.db.commit();
    	return b;
    }

    @Override
    public void putAll(Map<? extends Long, ? extends byte[]> m) {
        this.treeMap.putAll(m);
    	this.db.commit();
    }

    @Override
    public void clear() {
        this.treeMap.clear();
    	this.db.commit();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Long> keySet() {
        return this.treeMap.keySet();
    }

    @Override
    public Collection<byte[]> values() {
        return this.treeMap.getValues();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<java.util.Map.Entry<Long, byte[]>> entrySet() {
        return this.treeMap.entrySet();
    }

    @Override
    public java.util.Map.Entry<Long, byte[]> firstEntry() {
        return this.treeMap.firstEntry();
    }

    @Override
    public java.util.Map.Entry<Long, byte[]> lastEntry() {
        return this.treeMap.lastEntry();
    }

    @Override
    public java.util.Map.Entry<Long, byte[]> pollFirstEntry() {
        return this.treeMap.pollFirstEntry();
    }

    @Override
    public java.util.Map.Entry<Long, byte[]> pollLastEntry() {
        return this.treeMap.pollLastEntry();
    }

    @Override
    public void close() throws IOException {
    	this.db.commit();
        this.treeMap.close();
        this.db.close();
    }

    public static void main(String[] args) {
    	File location = new File("/tmp/mapdbtest");
    	location.mkdirs();
    	try {
    		MapDBSortedMap map = new MapDBSortedMap(new File(location, "testdb"));
			for (int i = 0; i < 10; i++) {
				map.put((long) i, ("x" + i).getBytes());
			}
			map.close();
    		map = new MapDBSortedMap(new File(location, "testdb"));
			while (map.size() > 0) {
				Map.Entry<Long, byte[]> entry = map.pollFirstEntry();
				System.out.println(new String(entry.getValue()));
			}
			map.close();
		} catch (IOException e) {
            Logger.warn(e);
		}
    }
}
