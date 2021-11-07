/**
 *  Cord
 *  Copyright 08.10.2021 by Michael Peter Christen, @orbiterlab
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

package eu.searchlab.storage.json;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.searchlab.storage.io.IOPath;

public interface Cord {

    public IOPath getObject();

    /**
     * get the number of objects within this cord
     * @return
     */
    public int size();

    /**
     * return true if the cord is empty
     * @return
     */
    public boolean isEmpty();

    /**
     * add an object to the end of the cord
     * @param key
     * @param value
     * @return
     * @throws IOException
     */
    public Cord append(JSONObject value) throws IOException;
    public Cord prepend(JSONObject value) throws IOException;
    public Cord insert(JSONObject value, int p) throws IOException;


    /**
     * remove the object denoted by the index position from the tray.
     * @param key
     * @return
     */
    public JSONObject remove(int p) throws IOException;
    public JSONObject removeFirst() throws IOException;
    public JSONObject removeLast() throws IOException;

    public List<JSONObject> removeAllWhere(String key, String value) throws IOException;
    public List<JSONObject> removeAllWhere(String key, long value) throws IOException;

    public JSONObject removeOneWhere(String key, String value) throws IOException;
    public JSONObject removeOneWhere(String key, long value) throws IOException;

    /**
     * Read one object from the tray.
     * If the key denotes not an object,
     * an exception is thrown.
     * @param key
     * @return
     * @throws IOException
     */
    public JSONObject get(int p) throws IOException;
    public JSONObject getFirst() throws IOException;
    public JSONObject getLast() throws IOException;

    public List<JSONObject> getAllWhere(String key, String value) throws IOException;
    public List<JSONObject> getAllWhere(String key, long value) throws IOException;

    public JSONObject getOneWhere(String key, String value) throws IOException;
    public JSONObject getOneWhere(String key, long value) throws IOException;
    /**
     * Translate the whole tray into JSONArray.
     * For most trays this is equivalent to return the buffered storage object.
     * Note that such objects must not be altered to prevent side-effects to the
     * implementing tray class.
     * @return
     * @throws IOException
     */
    public JSONArray toJSON() throws IOException;

    /**
     * hash codes of trays must be identical with the hash code of the object path hash
     * @return
     */
    @Override
    public int hashCode();

    /**
     * a commit must be done for all trays which buffer write operations in RAM
     * @return
     * @throws IOException
     */
    public Cord commit() throws IOException;

    /**
     * a close must be called to free resources and to flush unwritten buffered data
     */
    public void close() throws IOException;

}
