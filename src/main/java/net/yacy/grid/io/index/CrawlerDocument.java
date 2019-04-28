/**
 *  CrawlerDocument
 *  Copyright 7.3.2018 by Michael Peter Christen, @0rb1t3r
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

import net.yacy.grid.tools.Digest;

public class CrawlerDocument extends Document {

    public static enum Status {
        rejected,     // the crawler has rejected the urls based on a filter setting
        created,      // the crawler has created the url and it was handed over to the loader
        load_failed,  // the loader failed to load the document
        loaded,       // the loader has loaded the document and passed it to the parser
        noncanonical, // in case that the parser rejected further processing
        parse_failed, // the parser failed to translate the document
        parsed,       // the parser processed the document and passed it to the indexer
        indexed       // the document was pushed to the indexer
    }
    
    public CrawlerDocument() {
        super();
    }
    
    public CrawlerDocument(Map<String, Object> map) {
        super(map);
    }
    
    public CrawlerDocument(JSONObject obj) {
        super(obj);
    }

    public static CrawlerDocument load(Index index, String id) throws IOException {
        JSONObject json = index.query(GridIndex.CRAWLER_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, id);
        if (json == null) throw new IOException("no document with id " + id + " in index");
        return new CrawlerDocument(json);
    }

    public CrawlerDocument store(Index index) throws IOException {
        if (index == null) return this;
        String id = Digest.encodeMD5Hex(this.getURL());
        index.add(GridIndex.CRAWLER_INDEX_NAME, GridIndex.EVENT_TYPE_NAME, id, this);
        return this;
    }

    public CrawlerDocument setCrawlId(String crawlId) {
        this.putString(CrawlerMapping.crawl_id_s, crawlId);
        return this;
    }
    
    public String getCrawlId() {
        return this.getString(CrawlerMapping.crawl_id_s, "");
    }
    
    public CrawlerDocument setURL(String url) {
        this.putString(CrawlerMapping.url_s, url);
        return this;
    }
    
    public String getURL() {
        return this.getString(CrawlerMapping.url_s, "");
    }
    
    public CrawlerDocument setStatus(Status status) {
        this.putString(CrawlerMapping.status_s, status.name());
        return this;
    }
    
    public Status getStatus() {
        String status = this.getString(CrawlerMapping.status_s, "");
        return Status.valueOf(status);
    }
    
    public CrawlerDocument setInitDate(Date date) {
        this.putDate(CrawlerMapping.init_date_dt, date);
        return this;
    }
    
    public Date getInitDate() {
        return this.getDate(CrawlerMapping.init_date_dt);
    }
    
    public CrawlerDocument setStatusDate(Date date) {
        this.putDate(CrawlerMapping.status_date_dt, date);
        return this;
    }
    
    public Date getStatusDate() {
        return this.getDate(CrawlerMapping.status_date_dt);
    }
    
    public CrawlerDocument setCollections(Collection<String> collections) {
        this.putStrings(CrawlerMapping.collection_sxt, collections);
        return this;
    }
    
    public List<String> getCollections() {
        return this.getStrings(CrawlerMapping.collection_sxt);
    }
    
    public CrawlerDocument setComment(String comment) {
        this.putString(CrawlerMapping.comment_t, comment);
        return this;
    }
    
    public String getComment() {
        return this.getString(CrawlerMapping.comment_t, "");
    }
    
}
