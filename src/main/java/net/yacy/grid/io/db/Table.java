/**
 *  Table
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

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for a table object, to be used as table in a database.
 * The iterable method iterates the ids of the database keys in no specific order
 */
public interface Table extends Closeable, Iterable<String>  {
    
    /**
     * get the size of the table
     * @return the number of entries in the table
     */
    public long size() throws IOException;

    /**
     * Write a key-value pair to the table
     * @param id the id of the entry
     * @param value the value of the entry
     */
    public void write(String id, String value) throws IOException;

    /**
     * read an entry from the table
     * @param id the id of the entry
     * @return the value or null if the entry does not exist
     */
    public String read(final String id) throws IOException;

    /**
     * check if an entry exists
     * @param id the id of the entry
     * @return true if the entry exist and is non-empty, false otherwise
     */
    public boolean exist(final String id) throws IOException;

    /**
     * delete an entry from the table
     * @param id the id of the entry
     * @return true if the entry existed and was deleted, fals otherwise
     */
    public boolean delete(final String id) throws IOException;
    
}
