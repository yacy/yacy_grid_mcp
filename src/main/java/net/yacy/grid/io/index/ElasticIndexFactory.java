/**
 *  ElasticIndexFactory
 *  Copyright 07.01.2018 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;

import org.json.JSONObject;

import net.yacy.grid.tools.JSONList;

public class ElasticIndexFactory implements IndexFactory {

    private static int DEFAULT_PORT = 9300;
    
    private String server, username, password;
    private int port;
    private Index index;
    
    public ElasticIndexFactory(String server, int port, String username, String password) throws IOException {
        this.server = server;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.port = port;

        this.index = new Index() {

            @Override
            public IndexFactory checkConnection() throws IOException {
                return ElasticIndexFactory.this;
            }

            @Override
            public IndexFactory add(String indexName, String typeName, String id, JSONObject object) throws IOException {
                
                return ElasticIndexFactory.this;
            }

            @Override
            public boolean exist(String id) throws IOException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int count(QueryLanguage language, String query) throws IOException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public JSONObject query(String id) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public JSONList query(QueryLanguage language, String query) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean delete(String id) throws IOException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int delete(QueryLanguage language, String query) throws IOException {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void close() {
                // TODO Auto-generated method stub
                
            }

        };
    }
    
    @Override
    public String getConnectionURL() {
        return "elastic://" +
                (this.username != null && this.username.length() > 0 ? username + (this.password != null && this.password.length() > 0 ? ":" + this.password : "") + "@" : "") +
                this.getHost() + ((this.hasDefaultPort() ? "" : ":" + this.getPort()));
    }

    @Override
    public String getHost() {
        return this.server;
    }

    @Override
    public boolean hasDefaultPort() {
        return this.port == -1 || this.port == DEFAULT_PORT;
    }

    @Override
    public int getPort() {
        return hasDefaultPort() ? DEFAULT_PORT : this.port;
    }

    @Override
    public Index getIndex() throws IOException {
        return this.index;
    }

    @Override
    public void close() {
        this.index.close();
    }


}
