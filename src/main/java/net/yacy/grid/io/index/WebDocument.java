/**
 *  WebDocument
 *  Copyright 1.4.2018 by Michael Peter Christen, @orbiterlab
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.json.JSONObject;

import net.yacy.grid.mcp.Configuration;
import net.yacy.grid.tools.MultiProtocolURL;

public class WebDocument extends Document {

    public WebDocument() {
        super();
    }

    public WebDocument(final Map<String, Object> map) {
        super(map);
    }

    public WebDocument(final JSONObject obj) {
        super(obj.toMap());
    }

    public static Map<String, WebDocument> loadBulk(final Configuration config, final Index index, final Collection<String> ids) throws IOException {
        final Map<String, JSONObject> jsonmap = index.queryBulk(
                config.properties.getOrDefault("grid.elasticsearch.indexName.web", GridIndex.DEFAULT_INDEXNAME_WEB),
                ids);
        final Map<String, WebDocument> docmap = new HashMap<>();
        jsonmap.forEach((id, doc) -> docmap.put(id, new WebDocument(doc)));
        return docmap;
    }

    public static WebDocument load(final Configuration config, final Index index, final String id) throws IOException {
        final JSONObject json = index.query(
                config.properties.getOrDefault("grid.elasticsearch.indexName.web", GridIndex.DEFAULT_INDEXNAME_WEB),
                id);
        if (json == null) throw new IOException("no document with id " + id + " in index");
        return new WebDocument(json);
    }

    public static void storeBulk(final Configuration config, final Index index, final Collection<WebDocument> documents) throws IOException {
        if (index == null) return;
        final Map<String, JSONObject> map = new HashMap<>();
        documents.forEach(webDocument -> {
            map.put(webDocument.getId(), webDocument);
        });
        index.addBulk(
                config.properties.getOrDefault("grid.elasticsearch.indexName.web", GridIndex.DEFAULT_INDEXNAME_WEB),
                config.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME),
                map);
    }

    public WebDocument store(final Configuration config, final Index index) throws IOException {
        if (index == null) return this;
        index.add(
                config.properties.getOrDefault("grid.elasticsearch.indexName.web", GridIndex.DEFAULT_INDEXNAME_WEB),
                config.properties.getOrDefault("grid.elasticsearch.typeName", GridIndex.DEFAULT_TYPENAME),
                getId(), this);
        return this;
    }

    public WebDocument setCrawlId(final String crawl_id) {
        this.putString(WebMapping.crawl_id_s, crawl_id);
        return this;
    }

    public String getCrawlId() {
        return this.getString(WebMapping.crawl_id_s, "");
    }

    public String getTitle() {
        final List<String> title = super.getStrings(WebMapping.title);
        return title == null || title.isEmpty() ? "" : title.iterator().next().toString();
    }

    public String getId() {
        return MultiProtocolURL.getDigest(getLink());
    }

    public String getLink() {
        return super.getString(WebMapping.url_s, "");
    }

    public String getSnippet(final Map<String, HighlightField> highlights, final YaCyQuery yq) {
        if (yq.yacyModifiers.contains("ranking")) {
            final StringBuilder info = new StringBuilder();
            for (final String t: yq.positiveBag) {
                info.append(t).append('(').append(analyseToken(t, yq).trim()).append(')').append(' ');
            }
            for (final String t: yq.negativeBag) {
                info.append('-').append(t).append('(').append(analyseToken(t, yq).trim()).append(')').append(' ');
            }
            return info.toString();
        }
        final List<String> description = super.getStrings(WebMapping.description_txt);
        final String snippetDescription = description == null || description.isEmpty() ? "" : description.iterator().next().toString();
        final String snippetHighlight = highlights == null || highlights.isEmpty() ? "" : highlights.values().iterator().next().fragments()[0].toString();
        final String snippet = snippetDescription.length() > snippetHighlight.length() ? snippetDescription : snippetHighlight;
        return snippet;
    }

    private String analyseToken(final String token, final YaCyQuery yq) {
        final StringBuilder t = new StringBuilder();
        final Pattern pattern = Pattern.compile(token);
        for (final Map.Entry<WebMapping, Float> boost: yq.boosts.entrySet()) {
            int count = 0;
            if (boost.getKey().getMapping().getType() == MappingType.string) {
                if (this.isString(boost.getKey())) {
                    if (token.equals(this.getString(boost.getKey(), "").toLowerCase())) count++;
                } else if (this.isStrings(boost.getKey())) {
                    for (final String f: this.getStrings(boost.getKey())) {
                        if (token.equals(f.toLowerCase())) count++;
                    }
                } else {
                    continue;
                }
            } else {
                String content = null;
                if (this.isString(boost.getKey())) {
                    content = this.getString(boost.getKey(), "").toLowerCase();
                } else if (this.isStrings(boost.getKey())) {
                    content = this.getStrings(boost.getKey()).toString().toLowerCase();
                } else {
                    continue;
                }
                final Matcher matcher = pattern.matcher(content);
                while (matcher.find()) count++;
            }
            if (count > 0) t.append(Integer.toString(count)).append('*').append(boost.getKey().name()).append('^').append(boost.getValue().toString()).append('+');
        }
        if (t.charAt(t.length() - 1) == '+') t.setLength(t.length() - 1);
        return t.toString();
    }

    public String pickImage(final String dflt) {
        final List<String> links = super.getStrings(WebMapping.images_sxt);
        final List<Integer> heights = super.getInts(WebMapping.images_height_val);
        final List<Integer> widths = super.getInts(WebMapping.images_width_val);
        if (links.size() == 0) return dflt;
        if (links.size() == heights.size() && heights.size() == widths.size()) {
            int maxsize = 0;
            int maxi = 0;
            for (int i = 0; i < heights.size(); i++) {
                final int pixel = heights.get(i) * widths.get(i);
                if (pixel > maxsize) {
                    maxsize = pixel;
                    maxi = i;
                }
            }
            final String link = links.get(maxi);
            return link;
        } else {
            final String link = links.get(0);
            return link;
        }
    }

    public Date getDate() {
        final Date last_modified_date = super.getDate(WebMapping.last_modified);
        return last_modified_date == null ? new Date() : last_modified_date;
    }

    public int getSize() {
        final int size = super.getInt(WebMapping.size_i);
        return size;
    }

    public String getHost() {
        final String host = super.getString(WebMapping.host_s, "");
        return host;
    }
}
