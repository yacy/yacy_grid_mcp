/**
 *  YaCySearchService
 *  Copyright 05.06.2017 by Michael Peter Christen, @0rb1t3r
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

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MatchQuery.ZeroTermsQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.ObjectAPIHandler;
import net.yacy.grid.http.Query;
import net.yacy.grid.http.ServiceResponse;
import net.yacy.grid.io.index.WebMapping;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.DateParser;

/**
 * test: call
 * http://127.0.0.1:8100/yacy/grid/mcp/index/yacysearch.json?query=*
 * compare with
 * http://localhost:9200/web/crawler/_search?q=*:*
 */
public class YaCySearchService extends ObjectAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303031749975L;
    public static final String NAME = "yacysearch";
    
    @Override
    public String getAPIPath() {
        return "/yacy/grid/mcp/index/" + NAME + ".json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response) {
        String query = call.get("query", "");
        //String contentdom = call.get("contentdom", "text");
        int maximumRecords = call.get("maximumRecords", 10);
        int startRecord = call.get("startRecord", 0);
        //int meanCount = call.get("meanCount", 5);
        int timezoneOffset = call.get("timezoneOffset", 0);
        //String nav = call.get("nav", "");
        //String prefermaskfilter = call.get("prefermaskfilter", "");
        //String constraint = call.get("constraint", "");
        JSONObject json = new JSONObject(true);
        
        QueryBuilder qb = QueryBuilders.multiMatchQuery(query, new String[]{"text_t"}).operator(Operator.AND).zeroTermsQuery(ZeroTermsQuery.ALL);
        net.yacy.grid.io.index.ElasticsearchClient.Query eq = Data.index.query("web", qb, timezoneOffset, startRecord, maximumRecords, 5);
        
        JSONArray channels = new JSONArray();
        json.put("channels", channels);
        JSONObject channel = new JSONObject();
        channels.put(channel);
        JSONArray items = new JSONArray();
        channel.put("title", "Search for " + query);
        channel.put("description", "Search for " + query);
        channel.put("startIndex", "" + startRecord);
        channel.put("itemsPerPage", "" + items.length());
        channel.put("searchTerms", query);
        channel.put("items", items);
        eq.result.forEach(map -> {
            JSONObject hit = new JSONObject(true);
            List<?> title = (List<?>) map.get(WebMapping.title.getSolrFieldName());
            String titleString = title == null || title.isEmpty() ? "" : title.iterator().next().toString();
            Object link = map.get(WebMapping.url_s.getSolrFieldName());
            List<?> description = (List<?>) map.get(WebMapping.description_txt.getSolrFieldName());
            String descriptionString = description == null || description.isEmpty() ? "" : description.iterator().next().toString();
            String last_modified = (String) map.get(WebMapping.last_modified.getSolrFieldName());
            Date last_modified_date = DateParser.iso8601MillisParser(last_modified);
            Integer size = (Integer) map.get(WebMapping.size_i.getSolrFieldName());
            int sizekb = size / 1024;
            int sizemb = sizekb / 1024;
            String size_string = sizemb > 0 ? (Integer.toString(sizemb) + " mbyte") : sizekb > 0 ? (Integer.toString(sizekb) + " kbyte") : (Integer.toString(size) + " byte");
            String host = (String) map.get(WebMapping.host_s.getSolrFieldName());
	        hit.put("title", titleString);
            hit.put("link", link.toString());
            hit.put("description", descriptionString);
            hit.put("pubDate", DateParser.formatRFC1123(last_modified_date));
            hit.put("size", size.toString());
            hit.put("sizename", size_string);
            hit.put("host", host);
            items.put(hit);
        });
        return new ServiceResponse(json);
    }
    
    /*
http://192.168.1.60:8000/yacysearch.json?query=casey+neistat&Enter=&contentdom=text&former=casey+neistad&maximumRecords=10
&startRecord=0&verify=ifexist&resource=global&nav=location%2Chosts%2Cauthors%2Cnamespace%2Ctopics%2Cfiletype%2Cprotocol%2Clanguage&
prefermaskfilter=&depth=0&constraint=&meanCount=5&timezoneOffset=-120
{
  "channels": [{
    "title": "YaCy P2P-Search for casey neistat",
    "description": "Search for casey neistat",
    "link": "http://192.168.1.60:8000/yacysearch.html?query=casey+neistat&amp;resource=global&amp;contentdom=text",
    "image": {
      "url": "http://192.168.1.60:8000/env/grafics/yacy.png",
      "title": "Search for casey neistat",
      "link": "http://192.168.1.60:8000/yacysearch.html?query=casey+neistat&amp;resource=global&amp;contentdom=text"
    },
    "startIndex": "0",
    "itemsPerPage": "10",
    "searchTerms": "casey+neistat",

    "items": [

    {
      "title": "Wikitubia | Fandom powered by Wikia",
      "link": "http://youtube.wikia.com/wiki/Main_Page",
      "code": "",
      "description": "<b>Casey<\/b> <b>Neistat<\/b>",
      "pubDate": "Tue, 25 Apr 2017 00:00:00 +0000",
      
      "size": "212832",
      "sizename": "207 kbyte",
      "guid": "Z5bdkLauDnHY",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=7c44e5e1-c28b-4289-80c5-00025cd6bb6c",
      "host": "youtube.wikia.com",
      "path": "/wiki/Main_Page",
      "file": "Main_Page",
      "urlhash": "Z5bdkLauDnHY",
      "ranking": "3.3030184E7"
    }
    ,
    {
      "title": "Casey Neistat shows inaccuracies in calorie counting -- for people like us who count everything, this is a problem! : fatlogic",
      "link": "https://www.reddit.com/r/fatlogic/comments/5yyvo7/casey_neistat_shows_inaccuracies_in_calorie/",
      "code": "",
      "description": "reddit reddit.com vote comment submit",
      "pubDate": "Mon, 27 Mar 2017 00:00:00 +0000",
      
      "size": "339904",
      "sizename": "331 kbyte",
      "guid": "x2_b9D1sVEP4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=16360187-7648-40e2-b60e-3a8b5832801e",
      "host": "www.reddit.com",
      "path": "/r/fatlogic/comments/5yyvo7/casey_neistat_shows_inaccuracies_in_calorie/",
      "file": "",
      "urlhash": "x2_b9D1sVEP4",
      "ranking": "1.52066816E8"
    }
    ,
    {
      "title": "CaseyNeistat - YouTube",
      "link": "https://www.youtube.com/user/caseyneistat",
      "code": "",
      "description": "&quot;<b>neistat<\/b> brothers&quot; hbo nyc <b>casey<\/b> <b>neistat<\/b>",
      "pubDate": "Thu, 04 May 2017 00:00:00 +0000",
      
      "size": "475795",
      "sizename": "464 kbyte",
      "guid": "wfXqhgPCpqc4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=2791c4a2-91ea-4eb8-8be1-0a3c43e02276",
      "host": "www.youtube.com",
      "path": "/user/caseyneistat",
      "file": "caseyneistat",
      "urlhash": "wfXqhgPCpqc4",
      "ranking": "1.2369792E8"
    }
    ,
    {
      "title": "ÐŸÐ¾Ñ‡ÐµÐ¼Ñƒ Ð½Ðµ Ð»ÑŽÐ±ÑÑ‚ Ñ…Ð¸Ð¿ÑÑ‚ÐµÑ€Ð¾Ð² \u2014 Talks \u2014 Ð¤Ð¾Ñ€ÑƒÐ¼",
      "link": "https://www.linux.org.ru/forum/talks/13115244/page1",
      "code": "",
      "description": "http://petapixel.com/2016/11/28/casey <b>neistat<\/b> beme app just got acquired cnn 25 million/ <b>Casey<\/b> <b>Neistat<\/b> and His Beme App Just Got Acquired by CNN for $25 Million",
      "pubDate": "Wed, 11 Jan 2017 00:00:00 +0000",
      
      "size": "72879",
      "sizename": "71 kbyte",
      "guid": "QtKMfy-VMaMg",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=b14cda49-f08d-4f02-a98d-30e1de9b7a5b",
      "host": "www.linux.org.ru",
      "path": "/forum/talks/13115244/page1",
      "file": "page1",
      "urlhash": "QtKMfy-VMaMg",
      "ranking": "5.020584E7"
    }
    ,
    {
      "title": "Official YouTube Blog",
      "link": "https://youtube.googleblog.com/",
      "code": "",
      "description": "<b>Casey<\/b> <b>Neistat<\/b> Jackie Aina YouTube Red Launching new ad supported shows, exclusively on YouTube",
      "pubDate": "Sat, 06 May 2017 00:00:00 +0000",
      
      "size": "199787",
      "sizename": "195 kbyte",
      "guid": "9J58-oGU1NU5",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=ae1f9402-5fae-4e43-867c-45851aa21fc8",
      "host": "youtube.googleblog.com",
      "path": "/",
      "file": "",
      "urlhash": "9J58-oGU1NU5",
      "ranking": "6.0494744E7"
    }
    ,
    {
      "title": "Casey Neistat (@CaseyNeistat) | Twitter",
      "link": "https://twitter.com/CaseyNeistat",
      "code": "",
      "description": "twitter com CaseyNeistat <b>Casey<\/b> <b>Neistat<\/b>.",
      "pubDate": "Tue, 21 Oct 2014 00:00:00 +0000",
      
      "size": "237018",
      "sizename": "231 kbyte",
      "guid": "kiW-zsBGIES4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=d9e11c4c-e093-4037-84b1-d0a29c78367a",
      "host": "twitter.com",
      "path": "/CaseyNeistat",
      "file": "CaseyNeistat",
      "urlhash": "kiW-zsBGIES4",
      "ranking": "1.57942899E9"
    }
    ,
    {
      "title": "Casey Neistat (@CaseyNeistat) | Twitter",
      "link": "https://twitter.com/CaseyNeistat?lang=kn",
      "code": "",
      "description": "twitter com CaseyNeistat lang kn <b>Casey<\/b> <b>Neistat<\/b>.",
      "pubDate": "Thu, 19 Jan 2017 00:00:00 +0000",
      
      "size": "341455",
      "sizename": "333 kbyte",
      "guid": "Vpxa3sBGIES4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=e322a69c-80b6-49f4-bbbb-c4570eb3817f",
      "host": "twitter.com",
      "path": "/CaseyNeistat",
      "file": "CaseyNeistat",
      "urlhash": "Vpxa3sBGIES4",
      "ranking": "1.42846438E9"
    }
    ,
    {
      "title": "Journalism Tools (@Journalism2ls) on Twitter",
      "link": "https://mobile.twitter.com/Journalism2ls",
      "code": "",
      "description": "@ Journalism2ls Nov 29 CNN to Launch New Media Brand with <b>Casey<\/b> <b>Neistat<\/b> buff.ly/2fLLbAo pic.twitter.com/Hl1PfV0i7n View photo Â· Journalism Tools",
      "pubDate": "Fri, 16 Dec 2016 00:00:00 +0000",
      
      "size": "64894",
      "sizename": "63 kbyte",
      "guid": "EOluq0pKN1O4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=588c32b2-5c55-4c83-b488-33a38c4754b5",
      "host": "mobile.twitter.com",
      "path": "/Journalism2ls",
      "file": "Journalism2ls",
      "urlhash": "EOluq0pKN1O4",
      "ranking": "5.1972016E7"
    }
    ,
    {
      "title": "Casey Neistat (@CaseyNeistat) | Ð¢Ð²Ð¸Ñ‚Ñ‚ÐµÑ€",
      "link": "https://twitter.com/CaseyNeistat?lang=ru",
      "code": "",
      "description": "twitter com CaseyNeistat lang ru <b>Casey<\/b> <b>Neistat<\/b>.",
      "pubDate": "Sun, 24 Jul 2016 00:00:00 +0000",
      
      "size": "284113",
      "sizename": "277 kbyte",
      "guid": "O3vZBsBGIES4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=dad8da2d-596e-4c05-8010-ae4d55929fda",
      "host": "twitter.com",
      "path": "/CaseyNeistat",
      "file": "CaseyNeistat",
      "urlhash": "O3vZBsBGIES4",
      "ranking": "1.42846438E9"
    }
    ,
    {
      "title": "Casey Neistat (@CaseyNeistat) | Twitter",
      "link": "https://twitter.com/CaseyNeistat?ref_src=twsrc%5Egoogle%7Ctwcamp%5Eserp%7Ctwgr%5Eauthor",
      "code": "",
      "description": "<b>Casey<\/b> <b>Neistat<\/b> (@CaseyNeistat) Twitter. By using Twitter\u2019s services you agree to our. Cookie Use and. Data Transfer outside the EU. We and our partners operate globally and use cookies, including for analytics, personal",
      "pubDate": "Sun, 02 Apr 2017 00:00:00 +0000",
      
      "size": "407620",
      "sizename": "398 kbyte",
      "guid": "khdfTsBGIES4",
      "faviconUrl": "ViewFavicon.png?maxwidth=16&amp;maxheight=16&amp;isStatic=true&amp;quadratic&amp;code=72ec2610-b661-4f1f-b24a-8b7546d34189",
      "host": "twitter.com",
      "path": "/CaseyNeistat",
      "file": "CaseyNeistat",
      "urlhash": "khdfTsBGIES4",
      "ranking": "7.7424083E8"
    }
    ],
    "navigation": [{
      "facetname": "protocols",
      "displayname": "Protocol",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
        {"name": "https", "count": "587", "modifier": "%2Fhttps", "url": "yacysearch.json?query=casey+neistat+%2Fhttps&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "http", "count": "134", "modifier": "%2Fhttp", "url": "yacysearch.json?query=casey+neistat+%2Fhttp&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"}
      ]
    },{
      "facetname": "hosts",
      "displayname": "Provider",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
        {"name": "euronext.com", "count": "264", "modifier": "site%3Aeuronext.com", "url": "yacysearch.json?query=casey+neistat+site%3Aeuronext.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "youtube.com", "count": "112", "modifier": "site%3Ayoutube.com", "url": "yacysearch.json?query=casey+neistat+site%3Ayoutube.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "arstechnica.com", "count": "99", "modifier": "site%3Aarstechnica.com", "url": "yacysearch.json?query=casey+neistat+site%3Aarstechnica.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "twitter.com", "count": "60", "modifier": "site%3Atwitter.com", "url": "yacysearch.json?query=casey+neistat+site%3Atwitter.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "wnd.com", "count": "39", "modifier": "site%3Awnd.com", "url": "yacysearch.json?query=casey+neistat+site%3Awnd.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "tim.blog", "count": "17", "modifier": "site%3Atim.blog", "url": "yacysearch.json?query=casey+neistat+site%3Atim.blog&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "reddit.com", "count": "15", "modifier": "site%3Areddit.com", "url": "yacysearch.json?query=casey+neistat+site%3Areddit.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "instructables.com", "count": "10", "modifier": "site%3Ainstructables.com", "url": "yacysearch.json?query=casey+neistat+site%3Ainstructables.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "gq-magazine.co.uk", "count": "6", "modifier": "site%3Agq-magazine.co.uk", "url": "yacysearch.json?query=casey+neistat+site%3Agq-magazine.co.uk&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "plus.google.com", "count": "4", "modifier": "site%3Aplus.google.com", "url": "yacysearch.json?query=casey+neistat+site%3Aplus.google.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "edition.cnn.com", "count": "4", "modifier": "site%3Aedition.cnn.com", "url": "yacysearch.json?query=casey+neistat+site%3Aedition.cnn.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "mobilegeeks.de", "count": "3", "modifier": "site%3Amobilegeeks.de", "url": "yacysearch.json?query=casey+neistat+site%3Amobilegeeks.de&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "thetvdb.com", "count": "3", "modifier": "site%3Athetvdb.com", "url": "yacysearch.json?query=casey+neistat+site%3Athetvdb.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "smh.com.au", "count": "3", "modifier": "site%3Asmh.com.au", "url": "yacysearch.json?query=casey+neistat+site%3Asmh.com.au&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "focus.de", "count": "3", "modifier": "site%3Afocus.de", "url": "yacysearch.json?query=casey+neistat+site%3Afocus.de&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "kickstarter.com", "count": "3", "modifier": "site%3Akickstarter.com", "url": "yacysearch.json?query=casey+neistat+site%3Akickstarter.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "washingtonpost.com", "count": "3", "modifier": "site%3Awashingtonpost.com", "url": "yacysearch.json?query=casey+neistat+site%3Awashingtonpost.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "caseyneistat.tumblr.com", "count": "2", "modifier": "site%3Acaseyneistat.tumblr.com", "url": "yacysearch.json?query=casey+neistat+site%3Acaseyneistat.tumblr.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "streamingmedia.com", "count": "2", "modifier": "site%3Astreamingmedia.com", "url": "yacysearch.json?query=casey+neistat+site%3Astreamingmedia.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "businessinsider.com", "count": "2", "modifier": "site%3Abusinessinsider.com", "url": "yacysearch.json?query=casey+neistat+site%3Abusinessinsider.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "time.com", "count": "2", "modifier": "site%3Atime.com", "url": "yacysearch.json?query=casey+neistat+site%3Atime.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "techcrunch.cn", "count": "2", "modifier": "site%3Atechcrunch.cn", "url": "yacysearch.json?query=casey+neistat+site%3Atechcrunch.cn&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "storify.com", "count": "2", "modifier": "site%3Astorify.com", "url": "yacysearch.json?query=casey+neistat+site%3Astorify.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "de.engadget.com", "count": "2", "modifier": "site%3Ade.engadget.com", "url": "yacysearch.json?query=casey+neistat+site%3Ade.engadget.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "techcrunch.com", "count": "2", "modifier": "site%3Atechcrunch.com", "url": "yacysearch.json?query=casey+neistat+site%3Atechcrunch.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "hackaday.com", "count": "2", "modifier": "site%3Ahackaday.com", "url": "yacysearch.json?query=casey+neistat+site%3Ahackaday.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "blickamabend.ch", "count": "2", "modifier": "site%3Ablickamabend.ch", "url": "yacysearch.json?query=casey+neistat+site%3Ablickamabend.ch&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "gizmodo.com", "count": "2", "modifier": "site%3Agizmodo.com", "url": "yacysearch.json?query=casey+neistat+site%3Agizmodo.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "m.cinenews.be", "count": "2", "modifier": "site%3Am.cinenews.be", "url": "yacysearch.json?query=casey+neistat+site%3Am.cinenews.be&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "mobile.twitter.com", "count": "2", "modifier": "site%3Amobile.twitter.com", "url": "yacysearch.json?query=casey+neistat+site%3Amobile.twitter.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "trucktrend.com", "count": "2", "modifier": "site%3Atrucktrend.com", "url": "yacysearch.json?query=casey+neistat+site%3Atrucktrend.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "thenextweb.com", "count": "2", "modifier": "site%3Athenextweb.com", "url": "yacysearch.json?query=casey+neistat+site%3Athenextweb.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "soundcloud.com", "count": "2", "modifier": "site%3Asoundcloud.com", "url": "yacysearch.json?query=casey+neistat+site%3Asoundcloud.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "instagram.com", "count": "2", "modifier": "site%3Ainstagram.com", "url": "yacysearch.json?query=casey+neistat+site%3Ainstagram.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "onlinemarketingrockstars.com", "count": "2", "modifier": "site%3Aonlinemarketingrockstars.com", "url": "yacysearch.json?query=casey+neistat+site%3Aonlinemarketingrockstars.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "overcast.fm", "count": "2", "modifier": "site%3Aovercast.fm", "url": "yacysearch.json?query=casey+neistat+site%3Aovercast.fm&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "blogg.sydsvenskan.se", "count": "1", "modifier": "site%3Ablogg.sydsvenskan.se", "url": "yacysearch.json?query=casey+neistat+site%3Ablogg.sydsvenskan.se&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "theguardian.com.au", "count": "1", "modifier": "site%3Atheguardian.com.au", "url": "yacysearch.json?query=casey+neistat+site%3Atheguardian.com.au&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "aradon.ro", "count": "1", "modifier": "site%3Aaradon.ro", "url": "yacysearch.json?query=casey+neistat+site%3Aaradon.ro&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "ibtimes.co.uk", "count": "1", "modifier": "site%3Aibtimes.co.uk", "url": "yacysearch.json?query=casey+neistat+site%3Aibtimes.co.uk&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "derwesten.de", "count": "1", "modifier": "site%3Aderwesten.de", "url": "yacysearch.json?query=casey+neistat+site%3Aderwesten.de&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "golem.de", "count": "1", "modifier": "site%3Agolem.de", "url": "yacysearch.json?query=casey+neistat+site%3Agolem.de&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "entrepreneur.com", "count": "1", "modifier": "site%3Aentrepreneur.com", "url": "yacysearch.json?query=casey+neistat+site%3Aentrepreneur.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "cs.wikipedia.org", "count": "1", "modifier": "site%3Acs.wikipedia.org", "url": "yacysearch.json?query=casey+neistat+site%3Acs.wikipedia.org&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "sputniknews.com", "count": "1", "modifier": "site%3Asputniknews.com", "url": "yacysearch.json?query=casey+neistat+site%3Asputniknews.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "sxsw.com", "count": "1", "modifier": "site%3Asxsw.com", "url": "yacysearch.json?query=casey+neistat+site%3Asxsw.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "rcgroups.com", "count": "1", "modifier": "site%3Arcgroups.com", "url": "yacysearch.json?query=casey+neistat+site%3Arcgroups.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "eportalsite.com", "count": "1", "modifier": "site%3Aeportalsite.com", "url": "yacysearch.json?query=casey+neistat+site%3Aeportalsite.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "zootopianickthefox.deviantart.com", "count": "1", "modifier": "site%3Azootopianickthefox.deviantart.com", "url": "yacysearch.json?query=casey+neistat+site%3Azootopianickthefox.deviantart.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "dailydot.com", "count": "1", "modifier": "site%3Adailydot.com", "url": "yacysearch.json?query=casey+neistat+site%3Adailydot.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "youtube.googleblog.com", "count": "1", "modifier": "site%3Ayoutube.googleblog.com", "url": "yacysearch.json?query=casey+neistat+site%3Ayoutube.googleblog.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "linux.org.ru", "count": "1", "modifier": "site%3Alinux.org.ru", "url": "yacysearch.json?query=casey+neistat+site%3Alinux.org.ru&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "forbes.com", "count": "1", "modifier": "site%3Aforbes.com", "url": "yacysearch.json?query=casey+neistat+site%3Aforbes.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "outdoor.de", "count": "1", "modifier": "site%3Aoutdoor.de", "url": "yacysearch.json?query=casey+neistat+site%3Aoutdoor.de&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "zhihu.com", "count": "1", "modifier": "site%3Azhihu.com", "url": "yacysearch.json?query=casey+neistat+site%3Azhihu.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "fredzone.org", "count": "1", "modifier": "site%3Afredzone.org", "url": "yacysearch.json?query=casey+neistat+site%3Afredzone.org&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "casey.nyc", "count": "1", "modifier": "site%3Acasey.nyc", "url": "yacysearch.json?query=casey+neistat+site%3Acasey.nyc&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "getlinkyoutube.com", "count": "1", "modifier": "site%3Agetlinkyoutube.com", "url": "yacysearch.json?query=casey+neistat+site%3Agetlinkyoutube.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "youtube.wikia.com", "count": "1", "modifier": "site%3Ayoutube.wikia.com", "url": "yacysearch.json?query=casey+neistat+site%3Ayoutube.wikia.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "verne.elpais.com", "count": "1", "modifier": "site%3Averne.elpais.com", "url": "yacysearch.json?query=casey+neistat+site%3Averne.elpais.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "businessinsider.com.au", "count": "1", "modifier": "site%3Abusinessinsider.com.au", "url": "yacysearch.json?query=casey+neistat+site%3Abusinessinsider.com.au&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "liberation.fr", "count": "1", "modifier": "site%3Aliberation.fr", "url": "yacysearch.json?query=casey+neistat+site%3Aliberation.fr&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "en.wikipedia.org", "count": "1", "modifier": "site%3Aen.wikipedia.org", "url": "yacysearch.json?query=casey+neistat+site%3Aen.wikipedia.org&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "caseyneistat.com", "count": "1", "modifier": "site%3Acaseyneistat.com", "url": "yacysearch.json?query=casey+neistat+site%3Acaseyneistat.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "geekosystem.com", "count": "1", "modifier": "site%3Ageekosystem.com", "url": "yacysearch.json?query=casey+neistat+site%3Ageekosystem.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "sirfrogsworth.tumblr.com", "count": "1", "modifier": "site%3Asirfrogsworth.tumblr.com", "url": "yacysearch.json?query=casey+neistat+site%3Asirfrogsworth.tumblr.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "adweek.com", "count": "1", "modifier": "site%3Aadweek.com", "url": "yacysearch.json?query=casey+neistat+site%3Aadweek.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "steemit.com", "count": "1", "modifier": "site%3Asteemit.com", "url": "yacysearch.json?query=casey+neistat+site%3Asteemit.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "fourhourworkweek.com", "count": "1", "modifier": "site%3Afourhourworkweek.com", "url": "yacysearch.json?query=casey+neistat+site%3Afourhourworkweek.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "nytimes.com", "count": "1", "modifier": "site%3Anytimes.com", "url": "yacysearch.json?query=casey+neistat+site%3Anytimes.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "geektime.co.il", "count": "1", "modifier": "site%3Ageektime.co.il", "url": "yacysearch.json?query=casey+neistat+site%3Ageektime.co.il&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "vid.me", "count": "1", "modifier": "site%3Avid.me", "url": "yacysearch.json?query=casey+neistat+site%3Avid.me&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "youtube.tumblr.com", "count": "1", "modifier": "site%3Ayoutube.tumblr.com", "url": "yacysearch.json?query=casey+neistat+site%3Ayoutube.tumblr.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "thecreatorsproject.vice.com", "count": "1", "modifier": "site%3Athecreatorsproject.vice.com", "url": "yacysearch.json?query=casey+neistat+site%3Athecreatorsproject.vice.com&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "kinox.tv", "count": "1", "modifier": "site%3Akinox.tv", "url": "yacysearch.json?query=casey+neistat+site%3Akinox.tv&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "front-euronext.euronext.aw.atos.net", "count": "1", "modifier": "site%3Afront-euronext.euronext.aw.atos.net", "url": "yacysearch.json?query=casey+neistat+site%3Afront-euronext.euronext.aw.atos.net&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"}
      ]
    },{
      "facetname": "authors",
      "displayname": "Authors",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
        {"name": "Â© 2015 EURONEXT", "count": "20", "modifier": "author%3A%28%C2%A9+2015+EURONEXT%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28%C2%A9+2015+EURONEXT%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "FOCUS Online", "count": "3", "modifier": "author%3A%28FOCUS+Online%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28FOCUS+Online%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "GQ", "count": "3", "modifier": "author%3AGQ", "url": "yacysearch.json?query=casey+neistat+author%3AGQ&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "https://www.facebook.com/caitlin.dewey", "count": "3", "modifier": "author%3Ahttps%3A%2F%2Fwww.facebook.com%2Fcaitlin.dewey", "url": "yacysearch.json?query=casey+neistat+author%3Ahttps%3A%2F%2Fwww.facebook.com%2Fcaitlin.dewey&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Â© 2016 EURONEXT", "count": "2", "modifier": "author%3A%28%C2%A9+2016+EURONEXT%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28%C2%A9+2016+EURONEXT%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Louis Cheslaw", "count": "2", "modifier": "author%3A%28Louis+Cheslaw%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Louis+Cheslaw%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Engadget DE", "count": "2", "modifier": "author%3A%28Engadget+DE%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Engadget+DE%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "CASEY NEISTAT", "count": "2", "modifier": "author%3A%28CASEY+NEISTAT%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28CASEY+NEISTAT%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Sarah Buhr", "count": "2", "modifier": "author%3A%28Sarah+Buhr%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Sarah+Buhr%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Christina Zdanowicz, CNN", "count": "2", "modifier": "author%3A%28Christina+Zdanowicz%2C+CNN%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Christina+Zdanowicz%2C+CNN%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Alexandra King, CNN", "count": "2", "modifier": "author%3A%28Alexandra+King%2C+CNN%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Alexandra+King%2C+CNN%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Blick am Abend", "count": "2", "modifier": "author%3A%28Blick+am+Abend%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Blick+am+Abend%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Martin Gardt", "count": "2", "modifier": "author%3A%28Martin+Gardt%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Martin+Gardt%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Cinenews.be", "count": "2", "modifier": "author%3ACinenews.be", "url": "yacysearch.json?query=casey+neistat+author%3ACinenews.be&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Sputnik", "count": "1", "modifier": "author%3ASputnik", "url": "yacysearch.json?query=casey+neistat+author%3ASputnik&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Dave Mosher, Tech Insider", "count": "1", "modifier": "author%3A%28Dave+Mosher%2C+Tech+Insider%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Dave+Mosher%2C+Tech+Insider%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "vidme", "count": "1", "modifier": "author%3Avidme", "url": "yacysearch.json?query=casey+neistat+author%3Avidme&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Lars Wienand", "count": "1", "modifier": "author%3A%28Lars+Wienand%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Lars+Wienand%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Quora", "count": "1", "modifier": "author%3AQuora", "url": "yacysearch.json?query=casey+neistat+author%3AQuora&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Alfred Joyner", "count": "1", "modifier": "author%3A%28Alfred+Joyner%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Alfred+Joyner%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Aly Weisman", "count": "1", "modifier": "author%3A%28Aly+Weisman%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Aly+Weisman%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Nathan McAlone", "count": "1", "modifier": "author%3A%28Nathan+McAlone%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Nathan+McAlone%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Amanda DeMatto", "count": "1", "modifier": "author%3A%28Amanda+DeMatto%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Amanda+DeMatto%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Dylan Jones", "count": "1", "modifier": "author%3A%28Dylan+Jones%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Dylan+Jones%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Copyright 2017 deviantART", "count": "1", "modifier": "author%3A%28Copyright+2017+deviantART%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Copyright+2017+deviantART%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Fairfax Regional Media", "count": "1", "modifier": "author%3A%28Fairfax+Regional+Media%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Fairfax+Regional+Media%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Linda Lacina", "count": "1", "modifier": "author%3A%28Linda+Lacina%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Linda+Lacina%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Ediciones El PaÃ­s", "count": "1", "modifier": "author%3A%28Ediciones+El+Pa%C3%ADs%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Ediciones+El+Pa%C3%ADs%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "Matthew Elworthy", "count": "1", "modifier": "author%3A%28Matthew+Elworthy%29", "url": "yacysearch.json?query=casey+neistat+author%3A%28Matthew+Elworthy%29&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"}
      ]
    },{
      "facetname": "filetype",
      "displayname": "Filetype",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
        {"name": "php", "count": "29", "modifier": "filetype%3Aphp", "url": "yacysearch.json?query=casey+neistat+filetype%3Aphp&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "html", "count": "15", "modifier": "filetype%3Ahtml", "url": "yacysearch.json?query=casey+neistat+filetype%3Ahtml&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "aspx", "count": "1", "modifier": "filetype%3Aaspx", "url": "yacysearch.json?query=casey+neistat+filetype%3Aaspx&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"}
      ]
    },{
      "facetname": "topics",
      "displayname": "Topics",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
        {"name": "travel", "count": "3", "modifier": "travel", "url": "yacysearch.json?query=casey+neistat+travel&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "joseph", "count": "4", "modifier": "joseph", "url": "yacysearch.json?query=casey+neistat+joseph&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "pixel", "count": "5", "modifier": "pixel", "url": "yacysearch.json?query=casey+neistat+pixel&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "you", "count": "6", "modifier": "you", "url": "yacysearch.json?query=casey+neistat+you&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "gear", "count": "7", "modifier": "gear", "url": "yacysearch.json?query=casey+neistat+gear&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "journalism", "count": "8", "modifier": "journalism", "url": "yacysearch.json?query=casey+neistat+journalism&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "first", "count": "10", "modifier": "first", "url": "yacysearch.json?query=casey+neistat+first&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "youtube", "count": "19", "modifier": "youtube", "url": "yacysearch.json?query=casey+neistat+youtube&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "twitter", "count": "11", "modifier": "twitter", "url": "yacysearch.json?query=casey+neistat+twitter&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "techcrunch", "count": "9", "modifier": "techcrunch", "url": "yacysearch.json?query=casey+neistat+techcrunch&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "tools", "count": "8", "modifier": "tools", "url": "yacysearch.json?query=casey+neistat+tools&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "mahmoud", "count": "6", "modifier": "mahmoud", "url": "yacysearch.json?query=casey+neistat+mahmoud&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "cvr", "count": "5", "modifier": "cvr", "url": "yacysearch.json?query=casey+neistat+cvr&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "edelmanergo", "count": "4", "modifier": "edelmanergo", "url": "yacysearch.json?query=casey+neistat+edelmanergo&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "paul", "count": "3", "modifier": "paul", "url": "yacysearch.json?query=casey+neistat+paul&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"},
        {"name": "amis", "count": "2", "modifier": "amis", "url": "yacysearch.json?query=casey+neistat+amis&maximumRecords=10&resource=global&verify=ifexist&prefermaskfilter=&cat=href&constraint=&contentdom=text&former=casey+neistat&startRecord=0"}
      ]
    }
    ],
    "totalResults": "209"
  }]
}

     */
    /*
#[jsonp-start]#
{
  "channels": [{
    "title": "YaCy P2P-Search for #[rss_query]#",
    "description": "Search for #[rss_query]#",
    "link": "#[searchBaseURL]#?query=#[rss_queryenc]#&amp;resource=#[resource]#&amp;contentdom=#[contentdom]#",
    "image": {
      "url": "#[rssYacyImageURL]#",
      "title": "Search for #[rss_query]#",
      "link": "#[searchBaseURL]#?query=#[rss_queryenc]#&amp;resource=#[resource]#&amp;contentdom=#[contentdom]#"
    },
    "startIndex": "#[num-results_offset]#",
    "itemsPerPage": "#[num-results_itemsPerPage]#",
    "searchTerms": "#[rss_queryenc]#",

    "items": [
#{results}#
<!--#include virtual="yacysearchitem.json?item=#[item]#&eventID=#[eventID]#" -->
#(content)#::#(nl)#::    ,#(/nl)#
    {
      "title": "#[title-json]#",
      "link": "#[link]#",
      "code": "#[code]#",
      "description": "#[description-json]#",
      "pubDate": "#[date822]#",
      #(showEvent)#::"eventDate": "#[date822]#",#(/showEvent)#
      "size": "#[size]#",
      "sizename": "#[sizename]#",
      "guid": "#[urlhash]#",
      #(favicon)#::"faviconUrl": "#[faviconUrl]#",#(/favicon)#
      "host": "#[host]#",
      "path": "#[path]#",
      "file": "#[file]#",
      "urlhash": "#[urlhash]#",
      "ranking": "#[ranking]#"
    }::#(item)#::#(nl)#::    ,#(/nl)#
    {
      "title": "#[name]#",
      "icon": "/ViewImage.png?maxwidth=96&amp;maxheight=96&amp;code=#[code]#",
      "image": "#[href]#",
      "cache": "#[hrefCache]#",
      "url": "#[source]#",
      "urlhash": "#[urlhash]#",
      "host": "#[sourcedom]#",
      "width": "#[width]#",
      "height": "#[height]#"
    }#(/item)#::
::
::
::
#(/content)#
#{/results}#
    ],
<!--#include virtual="yacysearchtrailer.json?eventID=#[eventID]#" -->
    "navigation": [#(nav-dates)#::{
      "facetname": "dates",
      "displayname": "Date",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    },#(/nav-dates)##(nav-filetypes)#::{
      "facetname": "filetypes",
      "displayname": "Filetype",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    },#(/nav-filetypes)##(nav-protocols)#::{
      "facetname": "protocols",
      "displayname": "Protocol",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#", "modifier": "#[modifier]#", "url": "#[url]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    },#(/nav-protocols)##{navs}#{
      "facetname": "#[name]#",
      "displayname": "#[displayname]#",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#", "modifier": "#[modifier]#", "url": "#[url]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    },#{/navs}##{nav-vocabulary}#{
      "facetname": "#[navname]#",
      "displayname": "#[navname]#",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#", "modifier": "#[modifier]#", "url": "#[url]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    },#{/nav-vocabulary}##(nav-topics)#{}::{
      "facetname": "topics",
      "displayname": "Topics",
      "type": "String",
      "min": "0",
      "max": "0",
      "mean": "0",
      "elements": [
#{element}#
        {"name": "#[name]#", "count": "#[count]#", "modifier": "#[modifier]#", "url": "#[url]#"}#(nl)#::,#(/nl)#
#{/element}#
      ]
    }#(/nav-topics)#
    ],
    "totalResults": "#[num-results_totalcount]#"
  }]
}
#[jsonp-end]#
     */
    
    /*
    
    public static class YJsonResponseWriter {

        // define a list of simple YaCySchema -> json Token matchings
        private static final Map<String, String> field2tag = new HashMap<String, String>();
        static {
            field2tag.put(WebMapping.url_protocol_s.name(), "protocol");
            field2tag.put(WebMapping.host_s.name(), "host");
            field2tag.put(WebMapping.url_file_ext_s.name(), "ext");
        }
         
        private String title;

        public YJsonResponseWriter() {
            super();
        }

        public void setTitle(String searchPageTitle) {
            this.title = searchPageTitle;
        }

        @Override
        public void write(final Writer writer, final SolrQueryRequest request, final SolrQueryResponse rsp) throws IOException {

            NamedList<?> values = rsp.getValues();
            
            assert values.get("responseHeader") != null;
            assert values.get("response") != null;

            SimpleOrderedMap<Object> responseHeader = (SimpleOrderedMap<Object>) rsp.getResponseHeader();
            DocList response = ((ResultContext) values.get("response")).docs;
            @SuppressWarnings("unchecked")
            SimpleOrderedMap<Object> facetCounts = (SimpleOrderedMap<Object>) values.get("facet_counts");
            @SuppressWarnings("unchecked")
            SimpleOrderedMap<Object> facetFields = facetCounts == null || facetCounts.size() == 0 ? null : (SimpleOrderedMap<Object>) facetCounts.get("facet_fields");
            @SuppressWarnings("unchecked")
            SimpleOrderedMap<Object> highlighting = (SimpleOrderedMap<Object>) values.get("highlighting");
            Map<String, LinkedHashSet<String>> snippets = OpensearchResponseWriter.highlighting(highlighting);

            // parse response header
            ResHead resHead = new ResHead();
            NamedList<?> val0 = (NamedList<?>) responseHeader.get("params");
            resHead.rows = Integer.parseInt((String) val0.get("rows"));
            resHead.offset = response.offset(); // equal to 'start'
            resHead.numFound = response.matches();

            String jsonp = request.getParams().get("callback"); // check for JSONP
            if (jsonp != null) {
                writer.write(jsonp.toCharArray());
                writer.write("([".toCharArray());
            }
            
            // write header
            writer.write(("{\"channels\": [{\n").toCharArray());
            solitaireTag(writer, "totalResults", Integer.toString(resHead.numFound));
            solitaireTag(writer, "startIndex", Integer.toString(resHead.offset));
            solitaireTag(writer, "itemsPerPage", Integer.toString(resHead.rows));
            solitaireTag(writer, "title", this.title);
            solitaireTag(writer, "description", "Search Result");
            writer.write("\"items\": [\n".toCharArray());

            // parse body
            final int responseCount = response.size();
            SolrIndexSearcher searcher = request.getSearcher();
            DocIterator iterator = response.iterator();
            for (int i = 0; i < responseCount; i++) {
                try {
                writer.write("{\n".toCharArray());
                int id = iterator.nextDoc();
                Document doc = searcher.doc(id, OpensearchResponseWriter.SOLR_FIELDS);
                List<IndexableField> fields = doc.getFields();
                int fieldc = fields.size();
                MultiProtocolURL url = null;
                String urlhash = null;
                List<String> descriptions = new ArrayList<String>();
                String title = "";
                StringBuilder path = new StringBuilder(80);
                for (int j = 0; j < fieldc; j++) {
                    IndexableField value = fields.get(j);
                    String fieldName = value.name();

                    // apply generic matching rule
                    String stag = field2tag.get(fieldName);
                    if (stag != null) {
                        solitaireTag(writer, stag, value.stringValue());
                        continue;
                    }
                    // some special handling here
                    if (CollectionSchema.sku.getSolrFieldName().equals(fieldName)) {
                        String u = value.stringValue();
                        try {
                            url = new MultiProtocolURL(u);
                            String filename = url.getFileName();
                            solitaireTag(writer, "link", u);
                            solitaireTag(writer, "file", filename);
                        } catch (final MalformedURLException e) {}
                        continue;
                    }
                    if (CollectionSchema.title.getSolrFieldName().equals(fieldName)) {
                        title = value.stringValue();
                        continue;
                    }
                    if (CollectionSchema.description_txt.getSolrFieldName().equals(fieldName)) {
                        String description = value.stringValue();
                        descriptions.add(description);
                        continue;
                    }
                    if (CollectionSchema.id.getSolrFieldName().equals(fieldName)) {
                        urlhash = value.stringValue();
                        solitaireTag(writer, "guid", urlhash);
                        continue;
                    }
                    if (CollectionSchema.url_paths_sxt.getSolrFieldName().equals(fieldName)) {
                        path.append('/').append(value.stringValue());
                        continue;
                    }
                    if (CollectionSchema.last_modified.getSolrFieldName().equals(fieldName)) {
                        Date d = new Date(Long.parseLong(value.stringValue()));
                        solitaireTag(writer, "pubDate", HeaderFramework.formatRFC1123(d));
                        continue;
                    }
                    if (CollectionSchema.size_i.getSolrFieldName().equals(fieldName)) {
                        int size = value.stringValue() != null && value.stringValue().length() > 0 ? Integer.parseInt(value.stringValue()) : -1;
                        int sizekb = size / 1024;
                        int sizemb = sizekb / 1024;
                        solitaireTag(writer, "size", value.stringValue());
                        solitaireTag(writer, "sizename", sizemb > 0 ? (Integer.toString(sizemb) + " mbyte") : sizekb > 0 ? (Integer.toString(sizekb) + " kbyte") : (Integer.toString(size) + " byte"));
                        continue;
                    }

                    //missing: "code","faviconCode"
                }
                
                // compute snippet from texts            
                solitaireTag(writer, "path", path.toString());
                solitaireTag(writer, "title", title.length() == 0 ? path.toString() : title.replaceAll("\"", "'"));
                LinkedHashSet<String> snippet = urlhash == null ? null : snippets.get(urlhash);
                if (snippet == null) {snippet = new LinkedHashSet<>(); snippet.addAll(descriptions);}
                OpensearchResponseWriter.removeSubsumedTitle(snippet, title);
                String snippetstring = snippet == null || snippet.size() == 0 ? (descriptions.size() > 0 ? descriptions.get(0) : "") : OpensearchResponseWriter.getLargestSnippet(snippet);
                if (snippetstring != null && snippetstring.length() > 140) {
                    snippetstring = snippetstring.substring(0, 140);
                    int sp = snippetstring.lastIndexOf(' ');
                    if (sp >= 0) snippetstring = snippetstring.substring(0, sp) + " ..."; else snippetstring = snippetstring + "...";
                }
                writer.write("\"description\":"); writer.write(JSONObject.quote(snippetstring)); writer.write("\n}\n");
                if (i < responseCount - 1) {
                    writer.write(",\n".toCharArray());
                }
                } catch (final Throwable ee) {
                    ConcurrentLog.logException(ee);
                    writer.write("\"description\":\"\"\n}\n");
                    if (i < responseCount - 1) {
                        writer.write(",\n".toCharArray());
                    }
                }
            }
            writer.write("],\n".toCharArray());
            
            
            writer.write("\"navigation\":[\n");

            // the facets can be created with the options &facet=true&facet.mincount=1&facet.field=host_s&facet.field=url_file_ext_s&facet.field=url_protocol_s&facet.field=author_sxt
            @SuppressWarnings("unchecked")
            NamedList<Integer> domains = facetFields == null ? null : (NamedList<Integer>) facetFields.get(CollectionSchema.host_s.getSolrFieldName());
            @SuppressWarnings("unchecked")
            NamedList<Integer> filetypes = facetFields == null ? null : (NamedList<Integer>) facetFields.get(CollectionSchema.url_file_ext_s.getSolrFieldName());
            @SuppressWarnings("unchecked")
            NamedList<Integer> protocols = facetFields == null ? null : (NamedList<Integer>) facetFields.get(CollectionSchema.url_protocol_s.getSolrFieldName());
            @SuppressWarnings("unchecked")
            NamedList<Integer> authors = facetFields == null ? null : (NamedList<Integer>) facetFields.get(CollectionSchema.author_sxt.getSolrFieldName());
            @SuppressWarnings("unchecked")
            NamedList<Integer> collections = facetFields == null ? null : (NamedList<Integer>) facetFields.get(CollectionSchema.collection_sxt.getSolrFieldName());

            int facetcount = 0;
            if (domains != null) {
                writer.write(facetcount > 0 ? ",\n" : "\n");
                writer.write("{\"facetname\":\"domains\",\"displayname\":\"Provider\",\"type\":\"String\",\"min\":\"0\",\"max\":\"0\",\"mean\":\"0\",\"elements\":[\n".toCharArray());
                for (int i = 0; i < domains.size(); i++) {
                    facetEntry(writer, "site", domains.getName(i), Integer.toString(domains.getVal(i)));
                    if (i < domains.size() - 1) writer.write(',');
                    writer.write("\n");
                }
                writer.write("]}".toCharArray());
                facetcount++;
            }
            if (filetypes != null) {
                writer.write(facetcount > 0 ? ",\n" : "\n");
                writer.write("{\"facetname\":\"filetypes\",\"displayname\":\"Filetypes\",\"type\":\"String\",\"min\":\"0\",\"max\":\"0\",\"mean\":\"0\",\"elements\":[\n".toCharArray());
                List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String,Integer>>();
                for (Map.Entry<String, Integer> e: filetypes) {
                    if (e.getKey().length() <= 6) l.add(e);
                    if (l.size() >= 16) break;
                }
                for (int i = 0; i < l.size(); i++) {
                    Map.Entry<String, Integer> e = l.get(i);
                    facetEntry(writer, "filetype", e.getKey(), Integer.toString(e.getValue()));
                    if (i < l.size() - 1) writer.write(',');
                    writer.write("\n");
                }
                writer.write("]}".toCharArray());
                facetcount++;
            }
            if (protocols != null) {
                writer.write(facetcount > 0 ? ",\n" : "\n");
                writer.write("{\"facetname\":\"protocols\",\"displayname\":\"Protocol\",\"type\":\"String\",\"min\":\"0\",\"max\":\"0\",\"mean\":\"0\",\"elements\":[\n".toCharArray());
                for (int i = 0; i < protocols.size(); i++) {
                    facetEntry(writer, "protocol", protocols.getName(i), Integer.toString(protocols.getVal(i)));
                    if (i < protocols.size() - 1) writer.write(',');
                    writer.write("\n");
                }
                writer.write("]}".toCharArray());
                facetcount++;
            }
            if (authors != null) {
                writer.write(facetcount > 0 ? ",\n" : "\n");
                writer.write("{\"facetname\":\"authors\",\"displayname\":\"Authors\",\"type\":\"String\",\"min\":\"0\",\"max\":\"0\",\"mean\":\"0\",\"elements\":[\n".toCharArray());
                for (int i = 0; i < authors.size(); i++) {
                    facetEntry(writer, "author", authors.getName(i), Integer.toString(authors.getVal(i)));
                    if (i < authors.size() - 1) writer.write(',');
                    writer.write("\n");
                }
                writer.write("]}".toCharArray());
                facetcount++;
            }
            if (collections != null) {
                writer.write(facetcount > 0 ? ",\n" : "\n");
                writer.write("{\"facetname\":\"collections\",\"displayname\":\"Collections\",\"type\":\"String\",\"min\":\"0\",\"max\":\"0\",\"mean\":\"0\",\"elements\":[\n".toCharArray());
                for (int i = 0; i < collections.size(); i++) {
                    facetEntry(writer, "collection", collections.getName(i), Integer.toString(collections.getVal(i)));
                    if (i < collections.size() - 1) writer.write(',');
                    writer.write("\n");
                }
                writer.write("]}".toCharArray());
                facetcount++;
            }
            writer.write("\n]}]}\n".toCharArray());
            
            if (jsonp != null) {
                writer.write("])".toCharArray());
            }
        }

        public static void solitaireTag(final Writer writer, final String tagname, String value) throws IOException {
            if (value == null) return;
            writer.write('"'); writer.write(tagname); writer.write("\":"); writer.write(JSONObject.quote(value)); writer.write(','); writer.write('\n');
        }

        private static void facetEntry(final Writer writer, String modifier, String propname, final String value) throws IOException {
            modifier = modifier.replaceAll("\"", "'").trim();
            propname = propname.replaceAll("\"", "'").trim();
            writer.write("{\"name\":"); writer.write(JSONObject.quote(propname));
            writer.write(",\"count\":"); writer.write(JSONObject.quote(value.replaceAll("\"", "'").trim())); 
            writer.write(",\"modifier\":"); writer.write(JSONObject.quote(modifier+"%3A"+propname));
            writer.write("}");
        }
    }
    */
    
}
