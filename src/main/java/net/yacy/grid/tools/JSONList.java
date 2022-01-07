/**
 *  JSONList
 *  (C) 3.7.2017 by Michael Peter Christen; mc@yacy.net, @orbiterlab
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

package net.yacy.grid.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A JSONList is an objet which represents a list of json objects: that is
 * a popular file format which is used as elasticsearch bulk index format.
 * A jsonlist file is a text file where every line is a json object.
 * To bring a JSONArray into jsonlist format you have to do
 * - make sure that every element in the JSONArray is JSONObject
 * - print out every json in the list without indentation
 */
public class JSONList implements Iterable<Object> {

	private JSONArray array;
	
	public JSONList() {
		this.array = new JSONArray();
	}
	
	public JSONList(InputStream sourceStream) throws IOException {
        this();
        BufferedReader br = new BufferedReader(new InputStreamReader(sourceStream, StandardCharsets.UTF_8));
        String line;
        try {
	        while ((line = br.readLine()) != null) {
	        	line = line.trim();
	        	if (line.length() == 0) continue;
	            JSONObject json = new JSONObject(new JSONTokener(line));
	            this.add(json);
	        }
        } catch (JSONException e) {
        	throw new IOException(e);
        }
	}

	public JSONList(JSONArray a) throws IOException {
		for (int i = 0; i < a.length(); i++) {
			if (!(a.get(i) instanceof JSONObject)) throw new IOException("all objects in JSONArray must be JSONObject");
		};
		this.array = a;
	}
	
	public JSONList(byte[] b) throws IOException {
		this(new ByteArrayInputStream(b));
	}
	
	public JSONList(String jsonlist) throws IOException {
		this(jsonlist.getBytes(StandardCharsets.UTF_8));
	}
	
	public JSONList add(JSONObject object) {
		this.array.put(object);
		return this;
	}
	
	public JSONObject get(int i) {
		return this.array.getJSONObject(i);
	}
	
	public int length() {
		return this.array.length();
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		this.array.forEach(entry -> sb.append(entry.toString()).append("\n"));
		return sb.toString();
	}
	
	public JSONArray toArray() {
		return this.array;
	}

	@Override
	public Iterator<Object> iterator() {
		return this.array.iterator();
	}
}
