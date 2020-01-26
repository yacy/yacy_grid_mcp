/**
 *  CrawlstartDocument
 *  Copyright 9.3.2018 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.index;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class CrawlstartDocument extends Document {

    public CrawlstartDocument() {
        super();
    }

    public CrawlstartDocument(Map<String, Object> map) {
        super(map);
    }

    public CrawlstartDocument(JSONObject obj) {
        super(obj);
    }

    public static Map<String, CrawlstartDocument> loadBulk(Index index, Collection<String> ids) throws IOException {
        Map<String, JSONObject> jsonmap = index.queryBulk(GridIndex.CRAWLSTART_INDEX_NAME, ids);
        Map<String, CrawlstartDocument> docmap = new HashMap<>();
        jsonmap.forEach((id, doc) -> docmap.put(id, new CrawlstartDocument(doc)));
        return docmap;
    }

    public static CrawlstartDocument load(Index index, String crawlid) throws IOException {
        JSONObject json = index.query(GridIndex.CRAWLSTART_INDEX_NAME, crawlid);
        if (json == null) throw new IOException("no crawl start with id " + crawlid + " in index");
        return new CrawlstartDocument(json);
    }

    public static void storeBulk(Index index, Collection<CrawlstartDocument> documents) throws IOException {
        if (index == null) return;
        Map<String, JSONObject> map = new HashMap<>();
        documents.forEach(crawlstartDocument -> {
            String id = crawlstartDocument.getCrawlID();
            map.put(id, crawlstartDocument);
        });
        index.addBulk(GridIndex.CRAWLSTART_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, map);
    }

    public CrawlstartDocument store(Index index) throws IOException {
        String crawlid = getCrawlID();
        index.add(GridIndex.CRAWLSTART_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, crawlid, this);
        return this;
    }

    public CrawlstartDocument setCrawlID(String crawl_id) {
        this.putString(CrawlstartMapping.crawl_id_s, crawl_id);
        return this;
    }

    public String getCrawlID() {
        return this.getString(CrawlstartMapping.crawl_id_s, "");
    }

    public CrawlstartDocument setMustmatch(String mustmatch) {
        this.putString(CrawlstartMapping.mustmatch_s, mustmatch);
        return this;
    }

    public String getMustmatch() {
        return this.getString(CrawlstartMapping.mustmatch_s, "");
    }

    public CrawlstartDocument setCollections(Collection<String> collections) {
        this.putStrings(CrawlstartMapping.collection_sxt, collections);
        return this;
    }

    public List<String> getCollections() {
        return this.getStrings(CrawlstartMapping.collection_sxt);
    }

    public CrawlstartDocument setCrawlstartURL(String url) {
        this.putString(CrawlstartMapping.start_url_s, url);
        return this;
    }

    public String getCrawstartURL() {
        return this.getString(CrawlstartMapping.start_url_s, "");
    }

    public CrawlstartDocument setCrawlstartSSLD(String url) {
        this.putString(CrawlstartMapping.start_ssld_s, url);
        return this;
    }

    public String getCrawstartSSLD() {
        return this.getString(CrawlstartMapping.start_ssld_s, "");
    }

    public CrawlstartDocument setInitDate(Date date) {
        this.putDate(CrawlstartMapping.init_date_dt, date);
        return this;
    }

    public Date getInitDate() {
        return this.getDate(CrawlstartMapping.init_date_dt);
    }

    public CrawlstartDocument setData(JSONObject data) {
        this.putObject(CrawlstartMapping.data_o, data);
        return this;
    }

    public JSONObject getData() {
        return this.getObject(CrawlstartMapping.data_o);
    }

}
