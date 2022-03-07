/**
 *  CrawlerDocument
 *  Copyright 7.3.2018 by Michael Peter Christen, @orbiterlab
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
import net.yacy.grid.tools.Digest;

public class CrawlerDocument extends Document {

    public static enum Status {
        rejected,     // the crawler has rejected the urls based on a filter setting
        accepted,     // the crawler has accepted the url and it was handed over to the loader
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

    public CrawlerDocument(final Map<String, Object> map) {
        super(map);
    }

    public CrawlerDocument(final JSONObject obj) {
        super(obj);
    }

    public static Map<String, CrawlerDocument> loadBulk(final Configuration data, final Index index, final Collection<String> ids) throws IOException {
        final Map<String, JSONObject> jsonmap = index.queryBulk(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawler", GridIndex.DEFAULT_INDEXNAME_CRAWLER),
                ids);
        final Map<String, CrawlerDocument> docmap = new HashMap<>();
        jsonmap.forEach((id, doc) -> {
            if (doc != null) docmap.put(id, new CrawlerDocument(doc));
        });
        return docmap;
    }

    public static CrawlerDocument load(final Configuration data, final Index index, final String id) throws IOException {
        JSONObject json = null;
        final String crawlerIndexName = data.properties.getOrDefault("grid.elasticsearch.indexName.crawler", GridIndex.DEFAULT_INDEXNAME_CRAWLER);
        try {
            // first try
            json = index.query(crawlerIndexName, id);
        } catch (final IOException e) {
            // this might fail because the object was not yet present in the index
            // we try to fix this with a refresh
            index.refresh(crawlerIndexName);
            try {
                // second try
                json = index.query(crawlerIndexName, id);
            } catch (final IOException ee) {
                // fail
                throw ee;
            }
        }
        if (json == null) throw new IOException("no document with id " + id + " in index");
        return new CrawlerDocument(json);
    }

    public static void storeBulk(final Configuration data, final Index index, final Map<String, CrawlerDocument> documents) throws IOException {
        if (index == null) return;
        final Map<String, JSONObject> map = new HashMap<>();
        documents.forEach((id, crawlerDocument) -> {
            if (crawlerDocument != null) {
                map.put(id, crawlerDocument);
            } else {
                assert false : "document is null";
            }
        });
        index.addBulk(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawler", GridIndex.DEFAULT_INDEXNAME_CRAWLER),
                data.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME), map);
    }

    public static void update(final Configuration data, final Index index, final String objectid, final JSONObject changes) throws IOException {
        final CrawlerDocument crawlerDocument = load(data, index, objectid);
        for (final String key: changes.keySet()) crawlerDocument.put(key, changes.get(key));
        crawlerDocument.store(data, index, objectid);
    }

    public CrawlerDocument store(final Configuration data, final Index index, final String objectid) throws IOException {
        if (index != null) index.add(
                data.properties.getOrDefault("grid.elasticsearch.indexName.crawler", GridIndex.DEFAULT_INDEXNAME_CRAWLER),
                data.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME), objectid, this);
        return this;
    }

    public CrawlerDocument store(final Configuration data, final Index index) throws IOException {
        final String url = getURL();
        if (url != null && url.length() > 0) {
            final String id = Digest.encodeMD5Hex(url);
            store(data, index, id);
        } else {
            assert false : "url not set / store";
        }
        return this;
    }

    public CrawlerDocument setCrawlID(final String crawlId) {
        this.putString(CrawlerMapping.crawl_id_s, crawlId);
        return this;
    }

    public String getCrawlID() {
        return this.getString(CrawlerMapping.crawl_id_s, "");
    }

    public CrawlerDocument setMustmatch(final String mustmatch) {
        this.putString(CrawlerMapping.mustmatch_s, mustmatch.replace("\\", "\\\\"));
        return this;
    }

    public String getMustmatch() {
        return this.getString(CrawlerMapping.mustmatch_s, "").replace("\\\\", "\\");
    }

    public CrawlerDocument setCollections(final Collection<String> collections) {
        this.putStrings(CrawlerMapping.collection_sxt, collections);
        return this;
    }

    public List<String> getCollections() {
        return this.getStrings(CrawlerMapping.collection_sxt);
    }

    public CrawlerDocument setCrawlstartURL(final String url) {
        this.putString(CrawlerMapping.start_url_s, url);
        return this;
    }

    public String getCrawstartURL() {
        return this.getString(CrawlerMapping.start_url_s, "");
    }

    public CrawlerDocument setCrawlstartSSLD(final String url) {
        this.putString(CrawlerMapping.start_ssld_s, url);
        return this;
    }

    public String getCrawstartSSLD() {
        return this.getString(CrawlerMapping.start_ssld_s, "");
    }

    public CrawlerDocument setInitDate(final Date date) {
        this.putDate(CrawlerMapping.init_date_dt, date);
        return this;
    }

    public Date getInitDate() {
        return this.getDate(CrawlerMapping.init_date_dt);
    }

    public CrawlerDocument setStatusDate(final Date date) {
        this.putDate(CrawlerMapping.status_date_dt, date);
        return this;
    }

    public Date getStatusDate() {
        return this.getDate(CrawlerMapping.status_date_dt);
    }

    public CrawlerDocument setStatus(final Status status) {
        this.putString(CrawlerMapping.status_s, status.name());
        return this;
    }

    public Status getStatus() {
        final String status = this.getString(CrawlerMapping.status_s, "");
        return Status.valueOf(status);
    }

    public CrawlerDocument setURL(final String url) {
        this.putString(CrawlerMapping.url_s, url);
        return this;
    }

    public String getURL() {
        return this.getString(CrawlerMapping.url_s, "");
    }

    public CrawlerDocument setComment(final String comment) {
        this.putString(CrawlerMapping.comment_t, comment);
        return this;
    }

    public String getComment() {
        return this.getString(CrawlerMapping.comment_t, "");
    }

}
