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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.json.XML;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.index.ElasticsearchClient;
import net.yacy.grid.io.index.Sort;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.io.index.YaCyQuery;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.Classification;
import net.yacy.grid.tools.DateParser;
import net.yacy.grid.tools.Digest;

/**
 * This is a implementation of the GSA Google Search Appliance API as documented in
 * https://www.google.com/support/enterprise/static/gsa/docs/admin/74/gsa_doc_set/xml_reference/index.html
 * 
 * We re-implement here functionality as given in the YaCy/1 implementation from
 * https://github.com/yacy/yacy_search_server/blob/master/source/net/yacy/http/servlets/GSAsearchServlet.java
 * which used a de-published API reference from
 * https://developers.google.com/search-appliance/documentation/614/xml_reference 
 * 
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
        // query Attributes:
        // for original GSA query attributes, see https://www.google.com/support/enterprise/static/gsa/docs/admin/74/gsa_doc_set/xml_reference/request_format.html#1082911
        String q = call.get("q", "");
        int num = call.get("num", call.get("rows", call.get("maximumRecords", 10))); // in GSA: the maximum value of this parameter is 1000
        int start = call.get("startRecord", call.get("start", 0)); // The index number of the results is 0-based
        Classification.ContentDomain contentdom =  Classification.ContentDomain.contentdomParser(call.get("contentdom", "all"));
        String site = call.get("site", call.get("collection", "").replace(',', '|'));  // important: call arguments may overrule parsed collection values if not empty. This can be used for authentified indexes!
        String[] sites = site.length() == 0 ? new String[0] : site.split("\\|");
        int timezoneOffset = call.get("timezoneOffset", -1);
        boolean explain = call.get("explain", false);
        Sort sort = new Sort(call.get("sort", ""));
        String queryXML = XML.escape(q);
        
        // prepare a query
        QueryBuilder termQuery = new YaCyQuery(q, sites, contentdom, timezoneOffset).queryBuilder;

        HighlightBuilder hb = new HighlightBuilder().field(WebMapping.text_t.getSolrFieldName()).preTags("").postTags("").fragmentSize(140);
        ElasticsearchClient.Query query = Data.getIndex().query("web", termQuery, null, sort, hb, timezoneOffset, start, num, 0, explain);
        List<Map<String, Object>> result = query.results;
        List<String> explanations = query.explanations;
 
        // no xml encoder here on purpose, we will try to not have such things into our software in the future!
        StringBuffer sb = new StringBuffer(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        
        // GSP
        sb.append("<GSP VER=\"3.2\">\n");
        sb.append("<!-- This is a Google Search Appliance API result, provided by YaCy Grid (see: https://github.com/yacy/yacy_grid_mcp). For the GSA protocol, see https://www.google.com/support/enterprise/static/gsa/docs/admin/74/gsa_doc_set/xml_reference/index.html -->\n");
        sb.append("<TM>0</TM>\n");
        sb.append("<Q>").append(queryXML).append("</Q>\n");
        sb.append("<PARAM name=\"output\" value=\"xml_no_dtd\" original_value=\"xml_no_dtd\"/>\n");
        sb.append("<PARAM name=\"ie\" value=\"UTF-8\" original_value=\"UTF-8\"/>\n");
        sb.append("<PARAM name=\"oe\" value=\"UTF-8\" original_value=\"UTF-8\"/>\n");
        sb.append("<PARAM name=\"q\" value=\"").append(queryXML).append("\" original_value=\"").append(queryXML).append("\"/>\n");
        sb.append("<PARAM name=\"start\" value=\"").append(Integer.toString(start)).append("\" original_value=\"").append(Integer.toString(start)).append("\"/>\n");
        sb.append("<PARAM name=\"num\" value=\"").append(Integer.toString(num)).append("\" original_value=\"").append(Integer.toString(num)).append("\"/>\n");
        sb.append("<PARAM name=\"site\" value=\"").append(XML.escape(site)).append("\" original_value=\"").append(XML.escape(site)).append("\"/>\n");
       
        // RES
        sb.append("<RES SN=\"" + (start + 1) + "\" EN=\"" + (start + result.size()) + "\">\n"); //SN; The index number (1-based) of this search result; EN: Indicates the index (1-based) of the last search result returned in this result set.
        sb.append("<M>").append(Integer.toString(query.hitCount)).append("</M>\n"); // this should show the estimated total number of results
        sb.append("<FI/>\n");
        //sb.append("<NB><NU>").append(getAPIPath()).append("?q=\"").append(queryXML).append("\"&amp;site=&amp;lr=&amp;ie=UTF-8&amp;oe=UTF-8&amp;output=xml_no_dtd&amp;client=&amp;access=&amp;sort=&amp;start=").append(Integer.toString(start)).append("&amp;num=").append(Integer.toString(num)).append("&amp;sa=N</NU></NB>\n");
        
        // List
        final AtomicInteger hit = new AtomicInteger(1);
        for (int hitc = 0; hitc < result.size(); hitc++) {
            Map<String, Object> map = result.get(hitc);
            Map<String, HighlightField> highlights = query.highlights.get(hitc);
            List<?> title = (List<?>) map.get(WebMapping.title.getSolrFieldName());
            String titleXML = title == null || title.isEmpty() ? "" : XML.escape(title.iterator().next().toString());
            Object link = map.get(WebMapping.url_s.getSolrFieldName());
            if (Classification.ContentDomain.IMAGE == contentdom) link = YaCyQuery.pickBestImage(map, (String) link);
            String linkXML = XML.escape(link.toString());
            String urlhash = Digest.encodeMD5Hex(link.toString());
            
            List<?> description = (List<?>) map.get(WebMapping.description_txt.getSolrFieldName());
            String snippetDescription = description == null || description.isEmpty() ? "" : description.iterator().next().toString();
            String snippetHighlight = highlights == null || highlights.isEmpty() ? "" : highlights.values().iterator().next().fragments()[0].toString();
            String snippetXML = snippetDescription.length() > snippetHighlight.length() ? XML.escape(snippetDescription) : XML.escape(snippetHighlight);
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
	        sb.append("<S>").append(snippetXML).append("</S>\n");
	        sb.append("<COLS>dht</COLS>\n");
	        sb.append("<HAS><L/><C SZ=\"").append(size_string).append("\" CID=\"").append(urlhash).append("\" ENC=\"UTF-8\"/></HAS>\n");
	        //sb.append("<ENT_SOURCE>yacy_v1.921_20170616_9248.tar.gz/amBzuRuUFyt6</ENT_SOURCE>\n");
	        if (explain) {
	            sb.append("<EXPLANATION><![CDATA[" +explanations.get(hitc) + "]]></EXPLANATION>\n"); 
	        }
	        sb.append("</R>\n");
        };
        
        // END RES GSP
        sb.append("</RES>\n");
        sb.append("</GSP>\n");

        return new ServiceResponse(sb.toString());
    }

    
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