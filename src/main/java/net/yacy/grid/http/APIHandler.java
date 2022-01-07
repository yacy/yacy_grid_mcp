/**
 *  APIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @orbiterlab
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

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

/**
 * Interface for all servlets
 */
public interface APIHandler {

    /**
     * get the path to the servlet
     * @return the url path of the servlet
     */
    public String getAPIPath();

    /**
     * get a name of the servlet (which can be used in the Action object to use servlets for contract actions)
     * @return the name element from getAPIPath()
     */
    public String getAPIName();

    /**
     * call the servlet with a query locally without a network connection
     * @param call a query object
     * @param response a http response object
     * @return a Service Response
     * @throws APIException
     */
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) throws APIException;

    /**
     * call a remote servlet with given params
     * @param protocolhostportstub the url stub string
     * @param params a json object with a set of key/values where each value is of type String
     * @return a Service Response
     * @throws IOException
     */
    public ServiceResponse serviceImpl(final String protocolhostportstub, JSONObject params) throws IOException;

    /**
     * call a remote servlet with given params
     * @param protocolhostportstub the url stub string
     * @param params a map object with a set of key/values where each value is of type byte[]
     * @return a Service Response
     * @throws IOException
     */
    public ServiceResponse serviceImpl(final String protocolhostportstub, Map<String, byte[]> params) throws IOException;

}
