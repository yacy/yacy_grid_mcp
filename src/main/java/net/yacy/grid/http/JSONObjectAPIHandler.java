/**
 *  JSONObjectAPIHandler
 *  Copyright 18.01.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.http;

import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;

public abstract class JSONObjectAPIHandler extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -2191240526448018368L;

    public ServiceResponse serviceImpl(final String protocolhostportstub, JSONObject params) throws IOException {
        String urlstring = protocolhostportstub + this.getAPIPath() + JSONAPIHandler.json2url(params);
        JSONObject json = ClientConnection.loadJSONObject(urlstring);
        return new ServiceResponse(json);
    }
    
    public ServiceResponse serviceImpl(final String protocolhostportstub, Map<String, byte[]> params) throws IOException {
        JSONObject json = ClientConnection.loadJSONObject(protocolhostportstub + this.getAPIPath(), params);
        return new ServiceResponse(json);
    }
    
}
