/**
 *  SearchIndex
 *  Copyright 25.03.2017 by Michael Peter Christen, @orbiterlab
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
package net.yacy.grid.io.index;

import java.io.Closeable;
import java.io.IOException;

import org.json.JSONObject;

import net.yacy.grid.contracts.Contract;

public interface SearchIndex extends Closeable {

    public void insert(String indexName, String tenantName, JSONObject json) throws IOException;
    
    public Contract select(String indexName, String tenantName, String field, String value) throws IOException;

}
