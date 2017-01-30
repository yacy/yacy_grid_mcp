/**
 *  Swagger
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

package net.yacy.grid.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Swagger extends JSONObject {

    public static enum Type {
        array, object, string, integer
    }
    
    public Swagger(File f) throws UnsupportedEncodingException, FileNotFoundException {
        super(new JSONTokener(new InputStreamReader(new FileInputStream(f), "UTF-8")));
    }
    
    public String getBasePath() {
        return this.getString("basePath");
    }
    
    public Set<String> getServlets() {
        return this.getJSONObject("paths").keySet();
    }
    
    public Servlet getServlet(String path) {
        return new Servlet(path, this.getJSONObject("paths").getJSONObject(path));
    }
    
    public class Servlet extends JSONObject {
        private String path;
        private String method;
        private Map<String, JSONObject> parameters;
        private JSONObject responses;
        public Servlet(String path, JSONObject o) {
            this.putAll(o);
            this.path = path;
            this.method = this.keys().next();
            JSONArray parameterArray = this.getJSONObject(this.method).getJSONArray("parameters");
            this.parameters = new LinkedHashMap<>();
            for (int i = 0; i < parameterArray.length(); i++) {
                JSONObject p = parameterArray.getJSONObject(i);
                this.parameters.put(p.getString("name"), p);
            }
            this.responses = this.getJSONObject(this.method).getJSONObject("responses").getJSONObject("200");
        }
        public String getPath() {
            return this.path;
        }
        public String getMethod() {
            return this.method;
        }
        public Set<String> getParameterNames() {
            return this.parameters.keySet();
        }
        public Type getParameterType(String paramName) {
            return Type.valueOf(this.parameters.get(paramName).getString("type"));
        }
        public Type getResponseType() {
            return Type.valueOf(this.responses.getJSONObject("schema").getString("type"));
        }
        public Definition getResponseDefinition() {
            String ref = this.responses.getJSONObject("schema").getJSONObject("items").getString("$ref");
            int p = ref.lastIndexOf('/');
            return getDefinition(ref.substring(p + 1));
        }
        public String toString() {
            return getPath() + getParameterNames() + " -> " + getResponseDefinition().toString();
        }
    }

    public Definition getDefinition(String definitionName) {
        return new Definition(definitionName, this.getJSONObject("definitions").getJSONObject(definitionName));
    }

    public static class Definition extends JSONObject {
        private String name;
        public Definition(String name, JSONObject o) {
            this.putAll(o);
            this.name = name;
        }
        
        public String getName() {
            return this.name;
        }
        
        public Type getType() {
            return Type.valueOf(this.getString("type"));
        }
        
        public Set<String> getPropertyNames() {
            return this.getJSONObject("properties").keySet();
        }
        
        public Type getPropertyType(String propertyName) {
            return Type.valueOf(this.getJSONObject("properties").getString(propertyName));
        }
        
        public String toString() {
            return getName() + "[" + this.getType() + "] == " + getPropertyNames().toString();
        }
    }
    
}
