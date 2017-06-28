/**
 *  MapDBTreeMap
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

package net.yacy.grid.io.db;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

public class MapDBTreeMap<K> implements SortedMap<K, byte[]>, NavigableMap<K, byte[]>, CloseableMap<K, byte[]> {

    private DB db;
    private BTreeMap<K, byte[]> treeMap;

    public static MapDBTreeMap<Long> newLongMap(File f) {
        return new MapDBTreeMap<Long>(f, Serializer.LONG);
    }
    
    public static MapDBTreeMap<String> newStringMap(File f) {
        return new MapDBTreeMap<String>(f, Serializer.STRING);
    }
    
    private MapDBTreeMap(File f, GroupSerializer<K> keySerializer) {
        this.db = DBMaker.fileDB(f).closeOnJvmShutdown().transactionEnable().make();
        this.treeMap = db.treeMap(f.getName())
                .keySerializer(keySerializer)
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
    public byte[] put(K key, byte[] value) {
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
    public void putAll(Map<? extends K, ? extends byte[]> m) {
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
    public Set<K> keySet() {
        return this.treeMap.keySet();
    }

    @Override
    public Collection<byte[]> values() {
        return this.treeMap.getValues();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<java.util.Map.Entry<K, byte[]>> entrySet() {
        return this.treeMap.entrySet();
    }

    @Override
    public Comparator<? super K> comparator() {
        return this.treeMap.comparator();
    }

    @Override
    public SortedMap<K, byte[]> subMap(K fromKey, K toKey) {
        return this.treeMap.subMap(fromKey, toKey);
    }

    @Override
    public SortedMap<K, byte[]> headMap(K toKey) {
        return this.treeMap.headMap(toKey);
    }

    @Override
    public SortedMap<K, byte[]> tailMap(K fromKey) {
        return this.treeMap.tailMap(fromKey);
    }

    @Override
    public K firstKey() {
        return this.treeMap.firstKey();
    }

    @Override
    public K lastKey() {
        return this.treeMap.lastKey();
    }

    @Override
    public java.util.Map.Entry<K, byte[]> lowerEntry(K key) {
        return this.treeMap.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        return this.treeMap.lowerKey(key);
    }

    @Override
    public java.util.Map.Entry<K, byte[]> floorEntry(K key) {
        return this.treeMap.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public java.util.Map.Entry<K, byte[]> ceilingEntry(K key) {
        return this.treeMap.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        return this.treeMap.ceilingKey(key);
    }

    @Override
    public java.util.Map.Entry<K, byte[]> higherEntry(K key) {
        return this.treeMap.higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
        return this.treeMap.higherKey(key);
    }

    @Override
    public java.util.Map.Entry<K, byte[]> firstEntry() {
        return this.treeMap.firstEntry();
    }

    @Override
    public java.util.Map.Entry<K, byte[]> lastEntry() {
        return this.treeMap.lastEntry();
    }

    @Override
    public java.util.Map.Entry<K, byte[]> pollFirstEntry() {
        return this.treeMap.pollFirstEntry();
    }

    @Override
    public java.util.Map.Entry<K, byte[]> pollLastEntry() {
        return this.treeMap.pollLastEntry();
    }

    @Override
    public NavigableMap<K, byte[]> descendingMap() {
        return this.treeMap.descendingMap();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return this.treeMap.navigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return this.treeMap.descendingKeySet();
    }

    @Override
    public NavigableMap<K, byte[]> subMap(K fromKey,  boolean fromInclusive, K toKey, boolean toInclusive) {
        return this.treeMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public NavigableMap<K, byte[]> headMap(K toKey, boolean inclusive) {
        return this.treeMap.headMap(toKey, inclusive);
    }

    @Override
    public NavigableMap<K, byte[]> tailMap(K fromKey, boolean inclusive) {
        return this.treeMap.tailMap(fromKey, inclusive);
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
    		MapDBTreeMap<Long> map = MapDBTreeMap.newLongMap(new File(location, "testdb"));
			for (int i = 0; i < 10; i++) {
				map.put((long) i, ("x" + i).getBytes());
			}
			map.close();
    		map = MapDBTreeMap.newLongMap(new File(location, "testdb"));
			while (map.size() > 0) {
				Map.Entry<Long, byte[]> entry = map.pollFirstEntry();
				System.out.println(new String(entry.getValue()));
			}
			map.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
