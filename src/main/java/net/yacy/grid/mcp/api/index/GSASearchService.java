/**
 *  GSASearchService
 *  Copyright 04.07.2017 by Michael Peter Christen, @0rb1t3r
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.index.query.Operator;
import org.json.XML;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.DateParser;
import net.yacy.grid.tools.Digest;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/index/gsasearch.xml?q=*
 * compare with
 * http://localhost:9200/web/crawler/_search?q=*:*
 */
public class GSASearchService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749975L;
    public static final String NAME = "gsasearch";

    // GSA date formatter (short form of ISO8601 date format)
    private static final String PATTERN_GSAFS = "yyyy-MM-dd";
    public static final SimpleDateFormat FORMAT_GSAFS = new SimpleDateFormat(PATTERN_GSAFS, Locale.US);
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/index/" + NAME + ".xml";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        String query = call.get("q", "");
        String queryXML = XML.escape(query);
        //String contentdom = call.get("contentdom", "text");
        int maximumRecords = call.get("num", 10);
        int startRecord = call.get("startRecord", 0);
        //int meanCount = call.get("meanCount", 5);
        //int timezoneOffset = call.get("timezoneOffset", 0);
        //String nav = call.get("nav", "");
        //String prefermaskfilter = call.get("prefermaskfilter", "");
        //String constraint = call.get("constraint", "");
        
        // no xml encoder here on purpose, we will try to not have such things into our software in the future!
        StringBuffer sb = new StringBuffer(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        
        // GSP
        sb.append("<GSP VER=\"3.2\">\n");
        sb.append("<!-- This is a Google Search Appliance API result, provided by YaCy. See https://developers.google.com/search-appliance/documentation/614/xml_reference -->\n");
        sb.append("<TM>0</TM>\n");
        sb.append("<Q>").append(queryXML).append("</Q>\n");
        sb.append("<PARAM name=\"output\" value=\"xml_no_dtd\" original_value=\"xml_no_dtd\"/>\n");
        sb.append("<PARAM name=\"ie\" value=\"UTF-8\" original_value=\"UTF-8\"/>\n");
        sb.append("<PARAM name=\"oe\" value=\"UTF-8\" original_value=\"UTF-8\"/>\n");
        sb.append("<PARAM name=\"q\" value=\"").append(queryXML).append("\" original_value=\"").append(queryXML).append("\"/>\n");
        sb.append("<PARAM name=\"start\" value=\"").append(Integer.toString(startRecord)).append("\" original_value=\"").append(Integer.toString(startRecord)).append("\"/>\n");
        sb.append("<PARAM name=\"num\" value=\"").append(Integer.toString(maximumRecords)).append("\" original_value=\"").append(Integer.toString(maximumRecords)).append("\"/>\n");
        
        // RES
        List<Map<String, Object>> result = Data.index.query("web", query, Operator.AND, 0, 10, "text_t");
        sb.append("<RES SN=\"1\" EN=\"3\">\n");
        sb.append("<M>").append(Integer.toString(result.size())).append("</M>\n");
        sb.append("<FI/>\n");
        sb.append("<NB><NU>").append(getAPIPath()).append("?q=\"").append(queryXML).append("\"&amp;site=&amp;lr=&amp;ie=UTF-8&amp;oe=UTF-8&amp;output=xml_no_dtd&amp;client=&amp;access=&amp;sort=&amp;start=").append(Integer.toString(startRecord)).append("&amp;num=").append(Integer.toString(maximumRecords)).append("&amp;sa=N</NU></NB>\n");
        
        // List
        final AtomicInteger hit = new AtomicInteger(1);
        result.forEach(map -> {
            List<?> title = (List<?>) map.get(WebMapping.title.getSolrFieldName());
            String titleXML = title == null || title.isEmpty() ? "" : XML.escape(title.iterator().next().toString());
            Object link = map.get(WebMapping.url_s.getSolrFieldName());
            String linkXML = XML.escape(link.toString());
            String urlhash = Digest.encodeMD5Hex(link.toString());
            List<?> description = (List<?>) map.get(WebMapping.description_txt.getSolrFieldName());
            String descriptionXML = description == null || description.isEmpty() ? "" :XML.escape(description.iterator().next().toString());
            String last_modified = (String) map.get(WebMapping.last_modified.getSolrFieldName());
            Date last_modified_date = DateParser.iso8601MillisParser(last_modified);
            Integer size = (Integer) map.get(WebMapping.size_i.getSolrFieldName());
            int sizekb = size / 1024;
            int sizemb = sizekb / 1024;
            String size_string = sizemb > 0 ? (Integer.toString(sizemb) + " mbyte") : sizekb > 0 ? (Integer.toString(sizekb) + " kbyte") : (Integer.toString(size) + " byte");
            //String host = (String) map.get(WebMapping.host_s.getSolrFieldName());
	        sb.append("<R N=\"").append(Integer.toString(hit.getAndIncrement())).append("\" MIME=\"text/html\">\n");
	        sb.append("<T>").append(titleXML).append("</T>\n");
	        sb.append("<FS NAME=\"date\" VALUE=\"").append(formatGSAFS(last_modified_date)).append("\"/>\n");
	        sb.append("<CRAWLDATE>").append(DateParser.formatRFC1123(last_modified_date)).append("</CRAWLDATE>\n");
	        sb.append("<LANG>en</LANG>\n");
	        sb.append("<U>").append(linkXML).append("</U>\n");
	        sb.append("<UE>").append(linkXML).append("</UE>\n");
	        sb.append("<S>").append(descriptionXML).append("</S>\n");
	        sb.append("<COLS>dht</COLS>\n");
	        sb.append("<HAS><L/><C SZ=\"").append(size_string).append("\" CID=\"").append(urlhash).append("\" ENC=\"UTF-8\"/></HAS>\n");
	        //sb.append("<ENT_SOURCE>yacy_v1.921_20170616_9248.tar.gz/amBzuRuUFyt6</ENT_SOURCE>\n");
	        sb.append("</R>\n");
        });
        
        // END RES GSP
        sb.append("</RES>\n");
        sb.append("</GSP>\n");

        return new ServiceResponse(sb.toString());
    }

/*
     public void write(final Writer writer, final SolrQueryRequest request, final SolrQueryResponse rsp) throws IOException {
        assert rsp.getValues().get("responseHeader") != null;
        assert rsp.getValues().get("response") != null;

        long start = System.currentTimeMillis();

        SimpleOrderedMap<Object> responseHeader = (SimpleOrderedMap<Object>) rsp.getResponseHeader();
        DocList response = ((ResultContext) rsp.getValues().get("response")).getDocList();
        @SuppressWarnings("unchecked")
        SimpleOrderedMap<Object> highlighting = (SimpleOrderedMap<Object>) rsp.getValues().get("highlighting");
        Map<String, LinkedHashSet<String>> snippets = OpensearchResponseWriter.highlighting(highlighting);
        Map<Object,Object> context = request.getContext();

        // parse response header
        ResHead resHead = new ResHead();
        NamedList<?> val0 = (NamedList<?>) responseHeader.get("params");
        resHead.rows = Integer.parseInt((String) val0.get(CommonParams.ROWS));
        resHead.offset = response.offset(); // equal to 'start'
        resHead.numFound = response.matches();
        //resHead.df = (String) val0.get("df");
        //resHead.q = (String) val0.get("q");
        //resHead.wt = (String) val0.get("wt");
        //resHead.status = (Integer) responseHeader.get("status");
        //resHead.QTime = (Integer) responseHeader.get("QTime");
        //resHead.maxScore = response.maxScore();

        // write header
        writer.write(XML_START);
        String query = request.getParams().get("originalQuery");
        String site  = getContextString(context, "site", "");
        String sort  = getContextString(context, "sort", "");
        String client  = getContextString(context, "client", "");
        String ip  = getContextString(context, "ip", "");
        String access  = getContextString(context, "access", "");
        String entqr  = getContextString(context, "entqr", "");
        OpensearchResponseWriter.solitaireTag(writer, "TM", Long.toString(System.currentTimeMillis() - start));
        OpensearchResponseWriter.solitaireTag(writer, "Q", query);
        paramTag(writer, "sort", sort);
        paramTag(writer, "output", "xml_no_dtd");
        paramTag(writer, "ie", StandardCharsets.UTF_8.name());
        paramTag(writer, "oe", StandardCharsets.UTF_8.name());
        paramTag(writer, "client", client);
        paramTag(writer, "q", query);
        paramTag(writer, "site", site);
        paramTag(writer, "start", Integer.toString(resHead.offset));
        paramTag(writer, "num", Integer.toString(resHead.rows));
        paramTag(writer, "ip", ip);
        paramTag(writer, "access", access); // p - search only public content, s - search only secure content, a - search all content, both public and secure
        paramTag(writer, "entqr", entqr); // query expansion policy; (entqr=1) -- Uses only the search appliance's synonym file, (entqr=1) -- Uses only the search appliance's synonym file, (entqr=3) -- Uses both standard and local synonym files.

        // body introduction
        final int responseCount = response.size();
        writer.write("<RES SN=\"" + (resHead.offset + 1) + "\" EN=\"" + (resHead.offset + responseCount) + "\">"); writer.write(lb); // The index (1-based) of the first and last search result returned in this result set.
        writer.write("<M>" + resHead.numFound + "</M>"); writer.write(lb); // The estimated total number of results for the search.
        writer.write("<FI/>"); writer.write(lb); // Indicates that document filtering was performed during this search.
        int nextStart = resHead.offset + responseCount;
        int nextNum = Math.min(resHead.numFound - nextStart, responseCount < resHead.rows ? 0 : resHead.rows);
        int prevStart = resHead.offset - resHead.rows;
        if (prevStart >= 0 || nextNum > 0) {
            writer.write("<NB>");
            if (prevStart >= 0) {
                writer.write("<PU>");
                XML.escapeCharData("/gsa/search?q=" + request.getParams().get(CommonParams.Q) + "&site=" + site +
                         "&lr=&ie=UTF-8&oe=UTF-8&output=xml_no_dtd&client=" + client + "&access=" + access +
                         "&sort=" + sort + "&start=" + prevStart + "&sa=N", writer); // a relative URL pointing to the NEXT results page.
                writer.write("</PU>");
            }
            if (nextNum > 0) {
                writer.write("<NU>");
                XML.escapeCharData("/gsa/search?q=" + request.getParams().get(CommonParams.Q) + "&site=" + site +
                         "&lr=&ie=UTF-8&oe=UTF-8&output=xml_no_dtd&client=" + client + "&access=" + access +
                         "&sort=" + sort + "&start=" + nextStart + "&num=" + nextNum + "&sa=N", writer); // a relative URL pointing to the NEXT results page.
                writer.write("</NU>");
            }
            writer.write("</NB>");
        }
        writer.write(lb);

        // parse body
        SolrIndexSearcher searcher = request.getSearcher();
        DocIterator iterator = response.iterator();
        String urlhash = null;
        for (int i = 0; i < responseCount; i++) {
            int id = iterator.nextDoc();
            Document doc = searcher.doc(id, SOLR_FIELDS);
            List<IndexableField> fields = doc.getFields();

            // pre-scan the fields to get the mime-type            
            String mime = "";
            for (IndexableField value: fields) {
                String fieldName = value.name();
                if (CollectionSchema.content_type.getSolrFieldName().equals(fieldName)) {
                    mime = value.stringValue();
                    break;
                }
            }
            
            // write the R header for a search result
            writer.write("<R N=\"" + (resHead.offset + i + 1)  + "\"" + (i == 1 ? " L=\"2\"" : "")  + (mime != null && mime.length() > 0 ? " MIME=\"" + mime + "\"" : "") + ">"); writer.write(lb);
            //List<String> texts = new ArrayList<String>();
            List<String> descriptions = new ArrayList<String>();
            List<String> collections = new ArrayList<String>();
            int size = 0;
            boolean title_written = false; // the solr index may contain several; we take only the first which should be the visible tag in <title></title>
            String title = null;
            for (IndexableField value: fields) {
                String fieldName = value.name();

                // apply generic matching rule
                String stag = field2tag.get(fieldName);
                if (stag != null) {
                    OpensearchResponseWriter.solitaireTag(writer, stag, value.stringValue());
                    continue;
                }

                // if the rule is not generic, use the specific here
                if (CollectionSchema.id.getSolrFieldName().equals(fieldName)) {
                    urlhash = value.stringValue();
                    continue;
                }
                if (CollectionSchema.sku.getSolrFieldName().equals(fieldName)) {
                    OpensearchResponseWriter.solitaireTag(writer, GSAToken.U.name(), value.stringValue());
                    OpensearchResponseWriter.solitaireTag(writer, GSAToken.UE.name(), value.stringValue());
                    continue;
                }
                if (CollectionSchema.title.getSolrFieldName().equals(fieldName) && !title_written) {
                    title = value.stringValue();
                    OpensearchResponseWriter.solitaireTag(writer, GSAToken.T.name(), highlight(title, query));
                    //texts.add(value.stringValue());
                    title_written = true;
                    continue;
                }
                if (CollectionSchema.description_txt.getSolrFieldName().equals(fieldName)) {
                    descriptions.add(value.stringValue());
                    //texts.adds(description);
                    continue;
                }
                if (CollectionSchema.last_modified.getSolrFieldName().equals(fieldName)) {
                    Date d = new Date(Long.parseLong(value.stringValue()));
                    writer.write("<FS NAME=\"date\" VALUE=\"" + formatGSAFS(d) + "\"/>\n");
                    //OpensearchResponseWriter.solitaireTag(writer, GSAToken.CACHE_LAST_MODIFIED.getSolrFieldName(), HeaderFramework.formatRFC1123(d));
                    //texts.add(value.stringValue());
                    continue;
                }
                if (CollectionSchema.load_date_dt.getSolrFieldName().equals(fieldName)) {
                    Date d = new Date(Long.parseLong(value.stringValue()));
                    OpensearchResponseWriter.solitaireTag(writer, GSAToken.CRAWLDATE.name(), HeaderFramework.formatRFC1123(d));
                    //texts.add(value.stringValue());
                    continue;
                }
                if (CollectionSchema.size_i.getSolrFieldName().equals(fieldName)) {
                    size = value.stringValue() != null && value.stringValue().length() > 0 ? Integer.parseInt(value.stringValue()) : -1;
                    continue;
                }
                if (CollectionSchema.collection_sxt.getSolrFieldName().equals(fieldName)) {
                    collections.add(value.stringValue());
                    continue;
                }
                //System.out.println("superfluous field: " + fieldName + ": " + value.stringValue()); // this can be avoided setting the enableLazyFieldLoading = false in solrconfig.xml
            }
            // compute snippet from texts
            LinkedHashSet<String> snippet = urlhash == null ? null : snippets.get(urlhash);
            OpensearchResponseWriter.removeSubsumedTitle(snippet, title);
            OpensearchResponseWriter.solitaireTag(writer, GSAToken.S.name(), snippet == null || snippet.size() == 0 ? (descriptions.size() > 0 ? descriptions.get(0) : "") : OpensearchResponseWriter.getLargestSnippet(snippet));
            OpensearchResponseWriter.solitaireTag(writer, GSAToken.GD.name(), descriptions.size() > 0 ? descriptions.get(0) : "");
            String cols = collections.toString();
            if (collections.size() > 0) OpensearchResponseWriter.solitaireTag(writer, "COLS", collections.size() > 1 ? cols.substring(1, cols.length() - 1).replaceAll(" ", "") : collections.get(0));
            writer.write("<HAS><L/><C SZ=\""); writer.write(Integer.toString(size / 1024)); writer.write("k\" CID=\""); writer.write(urlhash); writer.write("\" ENC=\"UTF-8\"/></HAS>\n");
            if (YaCyVer == null) YaCyVer = yacyVersion.thisVersion().getName() + "/" + Switchboard.getSwitchboard().peers.mySeed().hash;
            OpensearchResponseWriter.solitaireTag(writer, GSAToken.ENT_SOURCE.name(), YaCyVer);
            OpensearchResponseWriter.closeTag(writer, "R");
        }
        writer.write("</RES>"); writer.write(lb);
        writer.write(XML_STOP);
    }

    private static String getContextString(Map<Object,Object> context, String key, String dflt) {
        Object v = context.get(key);
        if (v == null) return dflt;
        if (v instanceof String) return (String) v;
        if (v instanceof String[]) {
            String[] va = (String[]) v;
            return va.length == 0 ? dflt : va[0];
        }
        return dflt;
    }
    
    public static void paramTag(final Writer writer, final String tagname, String value) throws IOException {
        if (value == null || value.length() == 0) return;
        writer.write("<PARAM name=\"");
        writer.write(tagname);
        writer.write("\" value=\"");
        XML.escapeAttributeValue(value, writer);
        writer.write("\" original_value=\"");
        XML.escapeAttributeValue(value, writer);
        writer.write("\"/>"); writer.write(lb);
    }

    public static String highlight(String text, String query) {
        if (query != null) {
            String[] q = CommonPattern.SPACE.split(CommonPattern.PLUS.matcher(query.trim().toLowerCase()).replaceAll(" "));
            for (String s: q) {
                int p = text.toLowerCase().indexOf(s.toLowerCase());
                if (p < 0) continue;
                text = text.substring(0, p) + "<b>" + text.substring(p, p + s.length()) + "</b>" + text.substring(p + s.length());
            }
            return text.replaceAll(Pattern.quote("</b> <b>"), " ");
        } 
        return text;
    }
 */
    
    /**
     * Format date for GSA (short form of ISO8601 date format)
     * @param date
     * @return datestring "yyyy-mm-dd"
     * @see ISO8601Formatter
     */
    public final String formatGSAFS(final Date date) {
        if (date == null) return "";
        synchronized (GSASearchService.FORMAT_GSAFS) {
            final String s = GSASearchService.FORMAT_GSAFS.format(date);
            return s;
        }
    }
}

/*
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<GSP VER="3.2">
<!-- This is a Google Search Appliance API result, provided by YaCy. See https://developers.google.com/search-appliance/documentation/614/xml_reference -->
<TM>0</TM>
<Q>www</Q>
<PARAM name="output" value="xml_no_dtd" original_value="xml_no_dtd"/>
<PARAM name="ie" value="UTF-8" original_value="UTF-8"/>
<PARAM name="oe" value="UTF-8" original_value="UTF-8"/>
<PARAM name="q" value="www" original_value="www"/>
<PARAM name="start" value="0" original_value="0"/>
<PARAM name="num" value="3" original_value="3"/>
<RES SN="1" EN="3">
<M>168448</M>
<FI/>
<NB><NU>/gsa/search?q="www"&amp;site=&amp;lr=&amp;ie=UTF-8&amp;oe=UTF-8&amp;output=xml_no_dtd&amp;client=&amp;access=&amp;sort=&amp;start=3&amp;num=3&amp;sa=N</NU></NB>
<R N="1" MIME="text/html">
<T>/&lt;b&gt;www&lt;/b&gt;/ - www</T>
<FS NAME="date" VALUE="2015-03-12"/>
<CRAWLDATE>Mon, 30 Mar 2015 00:00:00 +0000</CRAWLDATE>
<LANG>en</LANG>
<U>http://8ch.net/www/3.html</U>
<UE>http://8ch.net/www/3.html</UE>
<S>8ch net &lt;b&gt;www&lt;/b&gt; 3 html.</S>
<COLS>dht</COLS>
<HAS><L/><C SZ="30k" CID="0C_-H73BCMYQ" ENC="UTF-8"/></HAS>
<ENT_SOURCE>yacy_v1.921_20170616_9248.tar.gz/amBzuRuUFyt6</ENT_SOURCE>
</R>
<R N="2" L="2" MIME="text/html">
<T>/&lt;b&gt;www&lt;/b&gt;/ - www</T>
<FS NAME="date" VALUE="2015-03-21"/>
<CRAWLDATE>Mon, 30 Mar 2015 00:00:00 +0000</CRAWLDATE>
<LANG>en</LANG>
<U>http://8ch.net/www/</U>
<UE>http://8ch.net/www/</UE>
<S>8ch net &lt;b&gt;www&lt;/b&gt;.</S>
<COLS>dht</COLS>
<HAS><L/><C SZ="37k" CID="1wAx173BCMYQ" ENC="UTF-8"/></HAS>
<ENT_SOURCE>yacy_v1.921_20170616_9248.tar.gz/amBzuRuUFyt6</ENT_SOURCE>
</R>
<R N="3" MIME="text/html">
<T>/&lt;b&gt;www&lt;/b&gt;/ - www</T>
<FS NAME="date" VALUE="2015-03-12"/>
<CRAWLDATE>Mon, 30 Mar 2015 00:00:00 +0000</CRAWLDATE>
<LANG>en</LANG>
<U>https://8ch.net/www/9.html</U>
<UE>https://8ch.net/www/9.html</UE>
<HAS><L/><C SZ="30k" CID="ucERt5SxnvSw" ENC="UTF-8"/></HAS>
<ENT_SOURCE>yacy_v1.921_20170616_9248.tar.gz/amBzuRuUFyt6</ENT_SOURCE>
</R>
</RES>
</GSP>
*/