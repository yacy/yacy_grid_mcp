/**
 *  MCP
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

package net.yacy.grid.mcp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.servlet.Servlet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.mind.SusiAction;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.Asset;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.index.SearchService;
import net.yacy.grid.mcp.api.info.ServicesService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.mcp.api.messages.AvailableService;
import net.yacy.grid.mcp.api.messages.ReceiveService;
import net.yacy.grid.mcp.api.messages.SendService;
import net.yacy.grid.tools.Digest;

/**
 * The Master Connect Program
 * 
 * URL for RabbitMQ: http://searchlab.eu:15672/
 */
public class MCP {

    private final static YaCyServices MCP_SERVICE = YaCyServices.mcp;
    private final static YaCyServices INDEXER_SERVICE = YaCyServices.indexer;
    private final static String DATA_PATH = "data";
 
    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] MCP_SERVICES = new Class[]{
            // information services
            ServicesService.class,
            StatusService.class,

            // message services
            SendService.class,
            ReceiveService.class,
            AvailableService.class,

            // asset services
            //RetrieveService.class,
            StoreService.class,
            LoadService.class,
            
            // search services
            SearchService.class
    };

    public static class IndexListener extends AbstractBrokerListener implements BrokerListener {

       public IndexListener(YaCyServices service) {
			super(service, 1);
		}

       @Override
       public boolean processAction(SusiAction action, JSONArray data) {
           // find result of indexing with http://localhost:9200/web/crawler/_search?q=text_t:*
           
           String sourceasset_path = action.getStringAttr("sourceasset");
           if (sourceasset_path == null || sourceasset_path.length() == 0) return false;
               
           InputStream sourceStream = null;
           try {
               Asset<byte[]> asset = Data.gridStorage.load(sourceasset_path);
               byte[] source = asset.getPayload();
               sourceStream = new ByteArrayInputStream(source);
               if (sourceasset_path.endsWith(".gz")) sourceStream = new GZIPInputStream(sourceStream);
               
               BufferedReader br = new BufferedReader(new InputStreamReader(sourceStream, StandardCharsets.UTF_8));
               String line;
               //List<BulkEntry> bulk = new ArrayList<>();
               indexloop: while ((line = br.readLine()) != null) try {
                   JSONObject json = new JSONObject(new JSONTokener(line));
                   if (json.has("index")) continue indexloop; // this is an elasticsearch index directive, we just skip that

                   String date = null;
                   if (date == null && json.has(WebMapping.last_modified.getSolrFieldName())) date = WebMapping.last_modified.getSolrFieldName();
                   if (date == null && json.has(WebMapping.load_date_dt.getSolrFieldName())) date = WebMapping.load_date_dt.getSolrFieldName();
                   if (date == null && json.has(WebMapping.fresh_date_dt.getSolrFieldName())) date = WebMapping.fresh_date_dt.getSolrFieldName();
                   String url = json.getString(WebMapping.url_s.getSolrFieldName());
                   String id = Digest.encodeMD5Hex(url);
                   boolean created = Data.index.writeMap("web", json.toMap(), "crawler", id);
                   Data.logger.info("indexed (" + (created ? "created" : "updated")+ "): " + url);
                   //BulkEntry be = new BulkEntry(json.getString("url_s"), "crawler", date, null, json.toMap());
                   //bulk.add(be);
               } catch (JSONException je) {}
               //Data.index.writeMapBulk("web", bulk);
               Data.logger.info("processed indexing message from queue: " + sourceasset_path);
               return true;
           } catch (Throwable e) {
               e.printStackTrace();
               return false;
           }
       }
    }
    
    public static void main(String[] args) {
        // initialize environment variables
        List<Class<? extends Servlet>> services = new ArrayList<>();
        services.addAll(Arrays.asList(MCP_SERVICES));
        Service.initEnvironment(MCP_SERVICE, services, DATA_PATH);
        
        // start listener
        if (Data.index != null) {
            BrokerListener brokerListener = new IndexListener(INDEXER_SERVICE);
            new Thread(brokerListener).start();
        }
        
        // start server
        Service.runService(null);
    }

}
