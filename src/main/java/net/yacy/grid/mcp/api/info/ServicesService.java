/**
 *  ServicesService
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

package net.yacy.grid.mcp.api.info;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.JSONArrayAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.Service;

public class ServicesService extends JSONArrayAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749879L;
    public static final String NAME = "services";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/info/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        
        // generate json
        JSONArray json = new JSONArray();
        try {
            json = Data.peerJsonDB.export(YaCyServices.mcp.name(), Service.Services.name());
        } catch (IOException e) {
            Data.logger.error("ServicesService fail", e);
        }
        return new ServiceResponse(json);
    }
    
}
