/**
 *  LoadService
 *  Copyright 28.1.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.mcp.api.assets;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.JSONObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.assets.Asset;
import net.yacy.grid.mcp.Data;

/**
 * test with http://127.0.0.1:8100/yacy/grid/mcp/assets/load?path=/xx/test.txt
 */
public class LoadService extends JSONObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578378303032739879L;
    public static final String NAME = "load";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/assets/" + NAME;
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        String path = call.get("path", "");
        byte[] a = null;
        if (path.length() > 0) {
            try {
                Asset<byte[]> asset = Data.gridStorage.load(path);
                a = asset.getPayload();
            } catch (IOException e) {
                Data.logger.error(e.getMessage(), e);
            }
        }
        // TODO: set Mime Type using the path extension
        return new ServiceResponse(a);
    }
}
