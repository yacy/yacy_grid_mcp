/**
 *  MapTable
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

import java.io.IOException;
import java.util.Iterator;

/**
 * Table implementation based on Maps
 */
public class MapTable implements Table {

    private CloseableMap<String, String> map;
    
    /**
     * Initialize a table using a back-up map. All changes will be
     * reflected in the backedMap
     * @param backedMap the storage location for the table
     */
    public MapTable(CloseableMap<String, String> backedMap) {
        this.map = backedMap;
    }
    
    @Override
    public void close() {
        try {
            this.map.close();
        } catch (IOException e) {
        }
    }
    
    /**
     * get the size of the table
     * @return the number of entries in the table
     */
    @Override
    public long size() {
        return this.map.size();
    }

    /**
     * Write a key-value pair to the table
     * @param id the id of the entry
     * @param value the value of the entry
     */
    @Override
    public void write(String id, String value) {
        this.map.put(id, value);
    }

    /**
     * read an entry from the table
     * @param id the id of the entry
     * @return the value or null if the entry does not exist
     */
    @Override
    public String read(String id) {
        return this.map.get(id);
    }

    /**
     * check if an entry exists
     * @param id the id of the entry
     * @return true if the entry exist and is non-empty, false otherwise
     */
    @Override
    public boolean exist(String id) {
        return this.map.containsKey(id);
    }

    /**
     * delete an entry from the table
     * @param id the id of the entry
     * @return true if the entry existed and was deleted, fals otherwise
     */
    @Override
    public boolean delete(String id) {
        return this.map.remove(id) != null;
    }

    @Override
    public Iterator<String> iterator() {
        return this.map.keySet().iterator();
    }

}
