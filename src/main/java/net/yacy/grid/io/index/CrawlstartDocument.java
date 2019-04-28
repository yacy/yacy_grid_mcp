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
    
    public static CrawlstartDocument load(Index index, String crawlid) throws IOException {
        JSONObject json = index.query(GridIndex.CRAWLSTART_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, crawlid);
        if (json == null) throw new IOException("no crawl start with id " + crawlid + " in index");
        return new CrawlstartDocument(json);
    }
    
    public CrawlstartDocument store(Index index, final String id) throws IOException {
        index.add(GridIndex.CRAWLSTART_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, id, this);
        return this;
    }

    public CrawlstartDocument setCrawlStart(String crawlId) {
        this.putString(CrawlstartMapping.start_s, crawlId);
        return this;
    }
    
    public String getCrawStart() {
        return this.getString(CrawlstartMapping.start_s, "");
    }

    public CrawlstartDocument setInitDate(Date date) {
        this.putDate(CrawlstartMapping.init_date_dt, date);
        return this;
    }
    
    public Date getInitDate() {
        return this.getDate(CrawlstartMapping.init_date_dt);
    }
    
    public CrawlstartDocument setCollections(Collection<String> collections) {
        this.putStrings(CrawlstartMapping.collection_sxt, collections);
        return this;
    }
    
    public List<String> getCollections() {
        return this.getStrings(CrawlstartMapping.collection_sxt);
    }
    
    public CrawlstartDocument setCrawlID(String url) {
        this.putString(CrawlstartMapping.crawl_id_s, url);
        return this;
    }
    
    public String getCrawlID() {
        return this.getString(CrawlstartMapping.crawl_id_s, "");
    }
    
    public CrawlstartDocument setData(JSONObject data) {
        this.putObject(CrawlstartMapping.data_o, data);
        return this;
    }
    
    public JSONObject getData() {
        return this.getObject(CrawlstartMapping.data_o);
    }
    
}
