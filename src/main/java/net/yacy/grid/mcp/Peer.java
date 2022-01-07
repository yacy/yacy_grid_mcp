/**
 *  Peer
 *  Copyright 28.1.2017 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp;

import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import net.yacy.grid.Services;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.tools.DateParser;

public class Peer extends JSONObject {
    
    public static Peer getLocalMCP(String gridname, String mcpaddress, int port) {
        return new Peer()
                .setGridname(gridname)
                .setMCPAddress(mcpaddress)
                .setService(YaCyServices.mcp)
                .setPeername("anon")
                .setHost("127.0.0.1")
                .setPort(port)
                .setLastseen(new Date())
                .setLastping(new Date());
    }
    
    public Peer() {
        super(true);
        this.put("gridname", "");
        this.put("mcpaddress", "");
        this.put("service", "");
        this.put("peername", "");
        this.put("host", "");
        this.put("port", 0);
        this.put("publichost", "");
        this.put("publicport", 0);
        this.put("lastseen", "");
        this.put("lastping", "");
    }

    public Peer setGridname(String gridname) {
        this.put("gridname", gridname);
        return this;
    }
    
    public String getGridname() {
        return this.getString("gridname");
    }
    
    public Peer setMCPAddress(String mcpaddress) {
        this.put("mcpaddress", mcpaddress);
        return this;
    }
    
    public String getMCPAddress() {
        return this.getString("mcpaddress");
    }
    
    public Peer(JSONObject json) {
        super(true);
        this.putAll(json);
    }
    
    public Peer setService(Services service) {
        this.put("service", service.name());
        return this;
    }
    
    public String getService() {
        return this.getString("service");
    }
    
    public Peer setPeername(String peername) {
        this.put("peername", peername);
        return this;
    }
    
    public String getPeername() {
        return this.getString("peername");
    }
    
    public Peer setHost(String host) {
        this.put("host", host);
        return this;
    }
    
    public String getHost() {
        return this.getString("host");
    }
    
    public Peer setPort(int port) {
        this.put("port", port);
        return this;
    }
    
    public int getPort() {
        return this.getInt("port");
    }
    
    public Peer setPublichost(String publichost) {
        this.put("publichost", publichost);
        return this;
    }
    
    public String getPublichost() {
        return this.getString("publichost");
    }
    
    public Peer setPublicport(int publicport) {
        this.put("publicport", publicport);
        return this;
    }
    
    public int getPublicport() {
        return this.getInt("publicport");
    }
   
    public Peer setLastseen(Date lastseen) {
        this.put("lastseen", DateParser.iso8601Format.format(lastseen));
        return this;
    }
    
    public Date getLastseen() {
        try {
            return DateParser.iso8601Format.parse(this.getString("lastseen"));
        } catch (JSONException | ParseException e) {
            return null;
        }
    }
    
    public Peer setLastping(Date lastping) {
        this.put("lastping", DateParser.iso8601Format.format(lastping));
        return this;
    }
    
    public Date getLastping() {
        try {
            return DateParser.iso8601Format.parse(this.getString("lastping"));
        } catch (JSONException | ParseException e) {
            return null;
        }
    }
    
}
