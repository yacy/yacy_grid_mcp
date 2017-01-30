/**
 *  TableFactory
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

/**
 * Factory for table objects
 */
public interface TableFactory {

    /**
     * get a table for a given table name. If the table does not exist, it will
     * be generated. If the table was generated before, the table is not re-generated
     * but the previously generated handle is returned
     * @param tableName the name of the table
     * @return the table
     * @throws IOException
     */
    public Table getTable(String tableName) throws IOException;
    
    /**
     * close the factory and all tables that have been opened
     */
    public void close();
}
