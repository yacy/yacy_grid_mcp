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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.Servlet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.susi.mind.SusiAction;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.Asset;
import net.yacy.grid.io.index.CrawlerDocument;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.io.index.CrawlerDocument.Status;
import net.yacy.grid.io.index.GridIndex;
import net.yacy.grid.mcp.api.admin.InquirySubmitService;
import net.yacy.grid.mcp.api.assets.LoadService;
import net.yacy.grid.mcp.api.assets.StoreService;
import net.yacy.grid.mcp.api.control.LoaderThrottlingService;
import net.yacy.grid.mcp.api.index.AddService;
import net.yacy.grid.mcp.api.index.CheckService;
import net.yacy.grid.mcp.api.index.CountService;
import net.yacy.grid.mcp.api.index.DeleteService;
import net.yacy.grid.mcp.api.index.ExistService;
import net.yacy.grid.mcp.api.index.GSASearchService;
import net.yacy.grid.mcp.api.index.QueryService;
import net.yacy.grid.mcp.api.index.YaCySearchService;
import net.yacy.grid.mcp.api.info.LogService;
import net.yacy.grid.mcp.api.info.ServicesService;
import net.yacy.grid.mcp.api.info.StatusService;
import net.yacy.grid.mcp.api.info.ThreaddumpService;
import net.yacy.grid.mcp.api.messages.AcknowledgeService;
import net.yacy.grid.mcp.api.messages.AvailableService;
import net.yacy.grid.mcp.api.messages.ClearService;
import net.yacy.grid.mcp.api.messages.PeekService;
import net.yacy.grid.mcp.api.messages.ReceiveService;
import net.yacy.grid.mcp.api.messages.RecoverService;
import net.yacy.grid.mcp.api.messages.SendService;
import net.yacy.grid.tools.GitTool;
import net.yacy.grid.tools.JSONList;
import net.yacy.grid.tools.MultiProtocolURL;

/**
 * The Master Connect Program
 * 
 * URL for RabbitMQ: http://searchlab.eu:15672/
 */
public class MCP {

    public final static YaCyServices MCP_SERVICE = YaCyServices.mcp;
    public final static YaCyServices INDEXER_SERVICE = YaCyServices.indexer;
    public final static String DATA_PATH = "data";
 
    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] MCP_SERVICES = new Class[]{
            // information services
            ServicesService.class,
            StatusService.class,
            ThreaddumpService.class,
            LogService.class,

            // control services
            LoaderThrottlingService.class,

            // message services
            AcknowledgeService.class,
            AvailableService.class,
            ClearService.class,
            PeekService.class,
            ReceiveService.class,
            RecoverService.class,
            SendService.class,

            // asset services
            //RetrieveService.class,
            StoreService.class,
            LoadService.class,

            // admin services
            InquirySubmitService.class,

            // search services
            YaCySearchService.class,
            GSASearchService.class,
            AddService.class,
            CheckService.class,
            CountService.class,
            DeleteService.class,
            ExistService.class,
            QueryService.class
    };

    public static class IndexListener extends AbstractBrokerListener implements BrokerListener {

       public IndexListener(YaCyServices service) {
            super(service, Runtime.getRuntime().availableProcessors());
        }

       @Override
       public ActionResult processAction(SusiAction action, JSONArray data, String processName, int processNumber) {
           // find result of indexing with http://localhost:9200/web/crawler/_search?q=text_t:*

           String sourceasset_path = action.getStringAttr("sourceasset");
           if (sourceasset_path == null || sourceasset_path.length() == 0) return ActionResult.FAIL_IRREVERSIBLE;

           try {
               // get the message with parsed documents
               JSONList jsonlist = null;
               if (action.hasAsset(sourceasset_path)) {
                   jsonlist = action.getJSONListAsset(sourceasset_path);
                  }
               if (jsonlist == null) try {
                   Asset<byte[]> asset = Data.gridStorage.load(sourceasset_path);
                   byte[] source = asset.getPayload();
                   jsonlist = new JSONList(new ByteArrayInputStream(source));
               } catch (IOException e) {
                   Data.logger.warn("MCP.processAction could not read asset from storage: " + sourceasset_path, e);
                   return ActionResult.FAIL_IRREVERSIBLE;
               }

               // for each document, write search index and crawler index
               indexloop: for (int line = 0; line < jsonlist.length(); line++) try {
                   JSONObject json = jsonlist.get(line);
                   if (json.has("index")) continue indexloop; // this is an elasticsearch index directive, we just skip that

                   // write search index
                   String date = null;
                   if (date == null && json.has(WebMapping.last_modified.getMapping().name())) date = WebMapping.last_modified.getMapping().name();
                   if (date == null && json.has(WebMapping.load_date_dt.getMapping().name())) date = WebMapping.load_date_dt.getMapping().name();
                   if (date == null && json.has(WebMapping.fresh_date_dt.getMapping().name())) date = WebMapping.fresh_date_dt.getMapping().name();
                   String url = json.getString(WebMapping.url_s.getMapping().name());
                   String urlid = MultiProtocolURL.getDigest(url);
                   boolean created = Data.gridIndex.getElasticClient().writeMap(GridIndex.WEB_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, urlid, json.toMap());
                   Data.logger.info("MCP.processAction indexed " + ((line + 1)/2)  + "/" + jsonlist.length()/2 + "(" + (created ? "created" : "updated")+ "): " + url);
                   //BulkEntry be = new BulkEntry(json.getString("url_s"), "crawler", date, null, json.toMap());
                   //bulk.add(be);

                   // write crawler index
                   try {
                       CrawlerDocument crawlerDocument = CrawlerDocument.load(Data.gridIndex, urlid);
                       crawlerDocument.setStatus(Status.indexed).setStatusDate(new Date());
                       crawlerDocument.store(Data.gridIndex);
                       // check with http://localhost:9200/crawler/_search?q=status_s:indexed
                   } catch (IOException e) {
                       // well that should not happen
                       Data.logger.warn("could not write crawler index", e);
                   }
               } catch (JSONException je) {
                   Data.logger.warn("", je);
               }
               //Data.index.writeMapBulk(GridIndex.WEB_INDEX_NAME, bulk);
               Data.logger.info("MCP.processAction processed indexing message from queue: " + sourceasset_path);
               return ActionResult.SUCCESS;
           } catch (Throwable e) {
               Data.logger.warn("MCP.processAction", e);
               return ActionResult.FAIL_IRREVERSIBLE;
           }
       }
    }

    public static void main(String[] args) {
        // initialize environment variables
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // start server
        List<Class<? extends Servlet>> services = new ArrayList<>();
        services.addAll(Arrays.asList(MCP_SERVICES));
        Service.initEnvironment(MCP_SERVICE, services, DATA_PATH, true);

        // start listener
        BrokerListener brokerListener = new IndexListener(INDEXER_SERVICE);
        new Thread(brokerListener).start();

        // start server
        Data.logger.info("started MCP");
        Data.logger.info(new GitTool().toString());
        Data.logger.info("you can now search using the query api, i.e.:");
        Data.logger.info("curl http://127.0.0.1:8100/yacy/grid/mcp/index/yacysearch.json?query=test");
        Service.runService(null);

        // this line is reached if the server was shut down
        brokerListener.terminate();
    }

}
