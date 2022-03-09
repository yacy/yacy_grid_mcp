/**
 *  IndexListener
 *  Copyright 14.01.2017 by Michael Peter Christen, @orbiterlab
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
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ai.susi.mind.SusiAction;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.assets.Asset;
import net.yacy.grid.io.index.CrawlerDocument;
import net.yacy.grid.io.index.CrawlerDocument.Status;
import net.yacy.grid.io.index.CrawlerMapping;
import net.yacy.grid.io.index.GridIndex;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.tools.CronBox.Telemetry;
import net.yacy.grid.tools.DateParser;
import net.yacy.grid.tools.JSONList;
import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.MultiProtocolURL;

public class IndexListener extends AbstractBrokerListener implements BrokerListener {

    public IndexListener(final Configuration Configuration, final YaCyServices service) {
         super(Configuration, service, Runtime.getRuntime().availableProcessors());
     }

    @Override
    public ActionResult processAction(final SusiAction action, final JSONArray jsondata, final String processName, final int processNumber) {
        // find result of indexing with http://localhost:9200/web/crawler/_search?q=text_t:*

        final String sourceasset_path = action.getStringAttr("sourceasset");
        if (sourceasset_path == null || sourceasset_path.length() == 0) return ActionResult.FAIL_IRREVERSIBLE;

        try {
            // get the message with parsed documents
            JSONList jsonlist = null;
            if (action.hasAsset(sourceasset_path)) {
                jsonlist = action.getJSONListAsset(sourceasset_path);
               }
            if (jsonlist == null || jsonlist.length() == 0) try {
                final Asset<byte[]> asset = this.config.gridStorage.load(sourceasset_path);
                final byte[] source = asset.getPayload();
                jsonlist = new JSONList(new ByteArrayInputStream(source));
            } catch (final IOException e) {
                Logger.warn(this.getClass(), "MCP.processAction could not read asset from storage: " + sourceasset_path, e);
                return ActionResult.FAIL_IRREVERSIBLE;
            }

            // for each document, write search index and crawler index
            indexloop: for (int line = 0; line < jsonlist.length(); line++) try {
                final JSONObject json = jsonlist.get(line);
                if (json.has("index")) continue indexloop; // this is an elasticsearch index directive, we just skip that

                // write search index
                String date = null;
                if (date == null && json.has(WebMapping.last_modified.getMapping().name())) date = WebMapping.last_modified.getMapping().name();
                if (date == null && json.has(WebMapping.load_date_dt.getMapping().name())) date = WebMapping.load_date_dt.getMapping().name();
                if (date == null && json.has(WebMapping.fresh_date_dt.getMapping().name())) date = WebMapping.fresh_date_dt.getMapping().name();
                final String url = json.getString(WebMapping.url_s.getMapping().name());
                final String urlid = MultiProtocolURL.getDigest(url);
                final boolean created = this.config.gridIndex.getElasticClient().writeMap(
                                    this.config.properties.getOrDefault("grid.elasticsearch.indexName.web", GridIndex.DEFAULT_INDEXNAME_WEB),
                                    this.config.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME),
                                    urlid, json.toMap());
                Logger.info(this.getClass(), "MCP.processAction indexed " + ((line + 1)/2)  + "/" + jsonlist.length()/2 + "(" + (created ? "created" : "updated")+ "): " + url);
                //BulkEntry be = new BulkEntry(json.getString("url_s"), "crawler", date, null, json.toMap());
                //bulk.add(be);

                // write crawler index
                try {
                    final JSONObject updater = new JSONObject()
                            .put(CrawlerMapping.status_s.getMapping().name(), Status.indexed.name())
                            .put(CrawlerMapping.status_date_dt.getMapping().name(), DateParser.iso8601MillisFormat.format(new Date()));
                    CrawlerDocument.update(this.config, this.config.gridIndex, urlid, updater);
                    // check with http://localhost:9200/crawler/_search?q=status_s:indexed
                } catch (final IOException e) {
                    // well that should not happen
                    Logger.warn(this.getClass(), "could not write crawler index", e);
                }
            } catch (final JSONException je) {
                Logger.warn(this.getClass(), "", je);
            }
            //Configuration.index.writeMapBulk(GridIndex.WEB_INDEX_NAME, bulk);
            Logger.info(this.getClass(), "MCP.processAction processed indexing message from queue: " + sourceasset_path);
            return ActionResult.SUCCESS;
        } catch (final Throwable e) {
            Logger.warn(this.getClass(), "MCP.processAction", e);
            return ActionResult.FAIL_IRREVERSIBLE;
        }
    }

     @Override
     public Telemetry getTelemetry() {
         return null;
     }
 }
