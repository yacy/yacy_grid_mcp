/**
 *  CheckService
 *  Copyright 04.03.2018 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp.api.index;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.index.Index;
import net.yacy.grid.io.index.IndexFactory;
import net.yacy.grid.mcp.Service;

public class CheckService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578334562349879L;
    public static final String NAME = "check";

    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/index/" + NAME + ".json";
    }

    @Override
    public ServiceResponse serviceImpl(final Query call, final HttpServletResponse response) {
        final JSONObject json = new JSONObject(true);
        try {
            final Index index = Service.instance.config.gridIndex.getElasticIndex();
            final IndexFactory factory = index.checkConnection();
            final String url = factory.getConnectionURL();
            json.put(ObjectAPIHandler.SUCCESS_KEY, true);
            if (url != null) json.put(ObjectAPIHandler.SERVICE_KEY, url);
        } catch (final IOException e) {
            json.put(ObjectAPIHandler.SUCCESS_KEY, false);
            json.put(ObjectAPIHandler.COMMENT_KEY, e.getMessage());
        }
        return new ServiceResponse(json);
    }
}
