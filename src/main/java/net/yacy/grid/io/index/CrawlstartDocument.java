/**
 *  CrawlstartDocument
 *  Copyright 9.3.2018 by Michael Peter Christen, @orbiterlab
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

import net.yacy.grid.mcp.Configuration;

public class CrawlstartDocument extends Document {

    public CrawlstartDocument() {
        super();
    }

    public CrawlstartDocument(final Map<String, Object> map) {
        super(map);
    }

    public CrawlstartDocument(final JSONObject obj) {
        super(obj);
    }

    public static Map<String, CrawlstartDocument> loadBulk(final Configuration data, final Index index, final Collection<String> ids) throws IOException {
        final Map<String, JSONObject> jsonmap = index.queryBulk(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawlstart", GridIndex.DEFAULT_INDEXNAME_CRAWLSTART),
                ids);
        final Map<String, CrawlstartDocument> docmap = new HashMap<>();
        jsonmap.forEach((id, doc) -> docmap.put(id, new CrawlstartDocument(doc)));
        return docmap;
    }

    public static CrawlstartDocument load(final Configuration data, final Index index, final String crawlid) throws IOException {
        final JSONObject json = index.query(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawlstart", GridIndex.DEFAULT_INDEXNAME_CRAWLSTART),
                crawlid);
        if (json == null) throw new IOException("no crawl start with id " + crawlid + " in index");
        return new CrawlstartDocument(json);
    }

    public static void storeBulk(final Configuration data, final Index index, final Collection<CrawlstartDocument> documents) throws IOException {
        if (index == null) return;
        final Map<String, JSONObject> map = new HashMap<>();
        documents.forEach(crawlstartDocument -> {
            final String id = crawlstartDocument.getCrawlID();
            map.put(id, crawlstartDocument);
        });
        index.addBulk(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawlstart", GridIndex.DEFAULT_INDEXNAME_CRAWLSTART),
                data.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME), map);
    }

    public CrawlstartDocument store(final Configuration data, final Index index) throws IOException {
        final String crawlid = getCrawlID();
        index.add(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawlstart", GridIndex.DEFAULT_INDEXNAME_CRAWLSTART),
                data.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME), crawlid, this);
        return this;
    }

    public CrawlstartDocument setCrawlID(final String crawl_id) {
        this.putString(CrawlstartMapping.crawl_id_s, crawl_id);
        return this;
    }

    public String getCrawlID() {
        return this.getString(CrawlstartMapping.crawl_id_s, "");
    }

    public CrawlstartDocument setMustmatch(final String mustmatch) {
        this.putString(CrawlstartMapping.mustmatch_s, mustmatch);
        return this;
    }

    public String getMustmatch() {
        return this.getString(CrawlstartMapping.mustmatch_s, "");
    }

    public CrawlstartDocument setCollections(final Collection<String> collections) {
        this.putStrings(CrawlstartMapping.collection_sxt, collections);
        return this;
    }

    public List<String> getCollections() {
        return this.getStrings(CrawlstartMapping.collection_sxt);
    }

    public CrawlstartDocument setCrawlstartURL(final String url) {
        this.putString(CrawlstartMapping.start_url_s, url);
        return this;
    }

    public String getCrawstartURL() {
        return this.getString(CrawlstartMapping.start_url_s, "");
    }

    public CrawlstartDocument setCrawlstartSSLD(final String url) {
        this.putString(CrawlstartMapping.start_ssld_s, url);
        return this;
    }

    public String getCrawstartSSLD() {
        return this.getString(CrawlstartMapping.start_ssld_s, "");
    }

    public CrawlstartDocument setInitDate(final Date date) {
        this.putDate(CrawlstartMapping.init_date_dt, date);
        return this;
    }

    public Date getInitDate() {
        return this.getDate(CrawlstartMapping.init_date_dt);
    }

    public CrawlstartDocument setData(final JSONObject data) {
        this.putObject(CrawlstartMapping.data_o, data);
        return this;
    }

    public JSONObject getData() {
        return this.getObject(CrawlstartMapping.data_o);
    }

}
