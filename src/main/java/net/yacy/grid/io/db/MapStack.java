/**
 *  MapStack
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A stack based on a map
 */
public class MapStack<A> implements Stack<A> {
    
    private NavigableMap<Long, A> map;
    private AtomicLong cc;
    
    public MapStack(NavigableMap<Long, A> backedMap) {
        this.map = backedMap;
        this.cc = new AtomicLong(0);
    }
    
    @Override
    public MapStack<A> clear() throws IOException {
        this.map.clear();
       
        return this;
    }
    
    @Override
    public int size() {
        return map.size();
    }
    
    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public MapStack<A> push(A value) {
        this.map.put(System.currentTimeMillis() + this.cc.incrementAndGet(), value);
        return this;
    }

    @Override
    public A bot() {
        return this.map.firstEntry().getValue();
    }

    @Override
    public A pot() {
        Map.Entry<Long, A> entry = this.map.pollFirstEntry();
        return entry == null ? null : entry.getValue();
    }

    @Override
    public A top() {
        return this.map.lastEntry().getValue();
    }

    @Override
    public A pop() {
        Map.Entry<Long, A> entry = this.map.pollLastEntry();
        return entry == null ? null : entry.getValue();
    }

    @Override
    public void close() {
        if (map instanceof Closeable)
            try {
                ((Closeable) this.map).close();
            } catch (IOException e) {}
    }
    
    public static void main(String[] args) {
        File f = new File("/tmp/stacktest");
        MapStack<byte[]> stack = new MapStack<byte[]>(MapDBTreeMap.newLongMap(f));
        try {stack.clear();} catch (IOException e) {}
        System.out.println(f.length());
        for (int i = 0; i < 10001; i++) {
            stack.push(Integer.toString(i).getBytes(StandardCharsets.UTF_8));
        }
        for (int i = 0; i < 10000; i++) {
            stack.pot();
        }
        stack.close();
        System.out.println(f.length());
    }
}