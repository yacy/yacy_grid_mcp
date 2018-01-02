/**
 *  LogService
 *  Copyright 02.01.2018 by Michael Peter Christen, @0rb1t3r
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

import javax.servlet.http.HttpServletResponse;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.mcp.Data;

/**
 * The Log Service
 * call http://localhost:8100/yacy/grid/mcp/info/log.txt
 */
public class LogService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = -7095346222464124199L;
    public static final String NAME = "log";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/info/" + NAME + ".txt";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response) {
        final StringBuilder buffer = new StringBuilder(1000);
        for (String line: Data.logAppender.getLines()) {
            buffer.append(line);
        }
        return new ServiceResponse(buffer.toString());
    }

}
