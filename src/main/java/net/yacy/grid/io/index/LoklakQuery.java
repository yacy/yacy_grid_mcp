/**
 *  ElasticsearchQuery
 *  Copyright 24.02.2017 by Michael Peter Christen, @0rb1t3r
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.yacy.grid.tools.DateParser;


public class LoklakQuery {
    
    private final static Pattern term4ORPattern = Pattern.compile("(?:^| )(\\S*(?: OR \\S*)+)(?: |$)"); // Pattern.compile("(^\\s*(?: OR ^\\s*+)+)");
    private final static Pattern tokenizerPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // tokenizes Strings into terms respecting quoted parts
    
    public QueryBuilder queryBuilder;
    public Date since;
    public Date until;

    public LoklakQuery(String q, int timezoneOffset) {
        // default values for since and util
        this.since = new Date(0);
        this.until = new Date(Long.MAX_VALUE);
        // parse the query
        this.queryBuilder = preparse(q, timezoneOffset);
    }
    
    private static List<String> splitIntoORGroups(String q) {
        // detect usage of OR junctor usage. Right now we cannot have mixed AND and OR usage. Thats a hack right now
        q = q.replaceAll(" AND ", " "); // AND is default
        
        // tokenize the query
        ArrayList<String> list = new ArrayList<>();
        Matcher m = term4ORPattern.matcher(q);
        while (m.find()) {
            String d = m.group(1);
            q = q.replace(d, "").replace("  ", " ");
            list.add(d);
            m = term4ORPattern.matcher(q);
        }
        q = q.trim();
        if (q.length() > 0) list.add(0, q);
        return list;
    }
    
    /**
     * fixing a query mistake covers most common wrong queries from the user
     * @param q
     * @return the fixed query
     */
    public static String fixQueryMistakes(String q) {
        q = q.replaceAll("hashtag:", "#");
        q = q.replaceAll(" AND ", " "); // AND is default
        return q;
    }
    

    private QueryBuilder preparse(String q, int timezoneOffset) {
        // detect usage of OR connector usage.
        q = fixQueryMistakes(q);
        List<String> terms = splitIntoORGroups(q); // OR binds stronger than AND
        if (terms.size() == 0) return QueryBuilders.constantScoreQuery(QueryBuilders.matchAllQuery());
        
        // special handling
        if (terms.size() == 1) return parse(terms.get(0), timezoneOffset);

        // generic handling
        BoolQueryBuilder aquery = QueryBuilders.boolQuery();
        for (String t: terms) {
            QueryBuilder partial = parse(t, timezoneOffset);
            aquery.filter(partial);
        }
        return aquery;
    }
    
    
    
    private QueryBuilder parse(String q, int timezoneOffset) {
        // detect usage of OR ORconnective usage. Because of the preparse step we will have only OR or only AND here.
        q = q.replaceAll(" AND ", " "); // AND is default
        boolean ORconnective = q.indexOf(" OR ") >= 0;
        q = q.replaceAll(" OR ", " "); // if we know that all terms are OR, we remove that and apply it later
        
        // tokenize the query
        Set<String> qe = new LinkedHashSet<String>();
        Matcher m = tokenizerPattern.matcher(q);
        while (m.find()) qe.add(m.group(1));
        
        // twitter search syntax:
        //   term1 term2 term3 - all three terms shall appear
        //   "term1 term2 term3" - exact match of all terms
        //   term1 OR term2 OR term3 - any of the three terms shall appear
        //   from:user - tweets posted from that user
        //   to:user - tweets posted to that user
        //   @user - tweets which mention that user
        //   near:"location" within:xmi - tweets that are near that location
        //   #hashtag - tweets containing the given hashtag
        //   since:2015-04-01 until:2015-04-03 - tweets within given time range
        // additional constraints:
        //   /image /audio /video /place - restrict to tweets which have attached images, audio, video or place
        ArrayList<String> text_positive_match = new ArrayList<>();
        ArrayList<String> text_negative_match = new ArrayList<>();
        ArrayList<String> text_positive_filter = new ArrayList<>();
        ArrayList<String> text_negative_filter = new ArrayList<>();
        ArrayList<String> users_positive = new ArrayList<>();
        ArrayList<String> users_negative = new ArrayList<>();
        ArrayList<String> hashtags_positive = new ArrayList<>();
        ArrayList<String> hashtags_negative = new ArrayList<>();
        Multimap<String, String> modifier = HashMultimap.create();
        Set<String> constraints_positive = new HashSet<>();
        Set<String> constraints_negative = new HashSet<>();
        for (String t: qe) {
            if (t.length() == 0) continue;
            if (t.startsWith("@")) {
                users_positive.add(t.substring(1));
                continue;
            } else if (t.startsWith("-@")) {
                users_negative.add(t.substring(2));
                continue;
            } else if (t.startsWith("#")) {
                hashtags_positive.add(t.substring(1));
                continue;
            } else if (t.startsWith("-#")) {
                hashtags_negative.add(t.substring(2));
                continue;
            } else if (t.startsWith("/")) {
                constraints_positive.add(t.substring(1));
                continue;
            } else if (t.startsWith("-/")) {
                constraints_negative.add(t.substring(2));
                continue;
            } else if (t.indexOf(':') > 0) {
                int p = t.indexOf(':');
                modifier.put(t.substring(0, p).toLowerCase(), t.substring(p + 1));
                continue;
            } else {
                // patch characters that will confuse elasticsearch or have a different meaning
                boolean negative = t.startsWith("-");
                if (negative) t = t.substring(1);
                if (t.length() == 0) continue;
                if ((t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') || (t.charAt(0) == '\'' && t.charAt(t.length() - 1) == '\'')) {
                    t = t.substring(1, t.length() - 1);
                    if (negative) text_negative_filter.add(t); else text_positive_filter.add(t);
                } else if (t.indexOf('-') > 0) {
                    // this must be handled like a quoted string without the minus
                    t = t.replaceAll("-", " ");
                    if (negative) text_negative_filter.add(t); else text_positive_filter.add(t);
                } else {
                    if (negative) text_negative_match.add(t); else text_positive_match.add(t);
                }
                continue;
            }
        }
        if (modifier.containsKey("to")) users_positive.addAll(modifier.get("to"));
        if (modifier.containsKey("-to")) users_negative.addAll(modifier.get("-to"));

        // special constraints
        //boolean constraint_about = constraints_positive.remove("about");
        //if (constraints_negative.remove("about")) constraint_about = false;
        
        // compose query for text
        List<QueryBuilder> ops = new ArrayList<>();
        List<QueryBuilder> nops = new ArrayList<>();
        List<QueryBuilder> filters = new ArrayList<>();
        for (String text: text_positive_match)  {
            ops.add(QueryBuilders.constantScoreQuery(QueryBuilders.matchQuery("text", text)));
        }
        for (String text: text_negative_match) {
            // negation of terms in disjunctions would cause to retrieve almost all documents
            // this cannot be the requirement of the user. It may be valid in conjunctions, but not in disjunctions
            nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.matchQuery("text", text)));
        }
        
        // apply modifiers
        if (modifier.containsKey("id")) {
            ops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("id_str", modifier.get("id"))));
        }
        if (modifier.containsKey("-id")) {
            nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("id_str", modifier.get("-id"))));
        }

        for (String user: users_positive) {
            ops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("mentions", user)));
        }
        for (String user: users_negative) nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("mentions", user)));
        
        for (String hashtag: hashtags_positive) {
            ops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase())));
        }
        for (String hashtag: hashtags_negative) nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("hashtags", hashtag.toLowerCase())));

        if (modifier.containsKey("from")) {
            for (String screen_name: modifier.get("from")) {
                if (screen_name.indexOf(',') < 0) {
                    ops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("screen_name", screen_name)));
                } else {
                    String[] screen_names = screen_name.split(",");
                    BoolQueryBuilder disjunction = QueryBuilders.boolQuery();
                    for (String name: screen_names) disjunction.should(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("screen_name", name)));
                    disjunction.minimumShouldMatch(1);
                    ops.add(disjunction);
                }
            }
        }
        if (modifier.containsKey("-from")) {
            for (String screen_name: modifier.get("-from")) {
                if (screen_name.indexOf(',') < 0) {
                    nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("screen_name", screen_name)));
                } else {
                    String[] screen_names = screen_name.split(",");
                    for (String name: screen_names) nops.add(QueryBuilders.constantScoreQuery(QueryBuilders.termQuery("screen_name", name)));
                }
            }
        }
        if (modifier.containsKey("since")) try {
            Calendar since = DateParser.parse(modifier.get("since").iterator().next(), timezoneOffset);
            this.since = since.getTime();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").from(this.since);
            if (modifier.containsKey("until")) {
                Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
                if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                    // until must be the day which is included in results.
                    // To get the result within the same day, we must add one day.
                    until.add(Calendar.DATE, 1);
                }
                this.until = until.getTime();
                rangeQuery.to(this.until);
            } else {
                this.until = new Date(Long.MAX_VALUE);
            }
            ops.add(rangeQuery);
        } catch (ParseException e) {} else if (modifier.containsKey("until")) try {
            Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
            if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                // until must be the day which is included in results.
                // To get the result within the same day, we must add one day.
                until.add(Calendar.DATE, 1);
            }
            this.until = until.getTime();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("created_at").to(this.until);
            ops.add(rangeQuery);
        } catch (ParseException e) {}

        // apply the ops and nops
        QueryBuilder bquery = QueryBuilders.boolQuery();
        if (ops.size() == 1 && nops.size() == 0)
            bquery = ops.iterator().next();
        else if (ops.size() == 0 && nops.size() == 1)
            bquery = QueryBuilders.boolQuery().mustNot(ops.iterator().next());
        else {
            for (QueryBuilder qb: ops) {
                if (ORconnective) ((BoolQueryBuilder) bquery).should(qb); else ((BoolQueryBuilder) bquery).filter(qb);
            }
            if (ORconnective) ((BoolQueryBuilder) bquery).minimumShouldMatch(1);
            for (QueryBuilder nqb: nops) {
                ((BoolQueryBuilder) bquery).mustNot(nqb);
            }
            
        }
        
        // apply constraints as filters
        for (String text: text_positive_filter) {
            filters.add(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("text", text)));
        }
        for (String text: text_negative_filter) filters.add(QueryBuilders.boolQuery().mustNot(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("text", text))));

        BoolQueryBuilder queryFilter = QueryBuilders.boolQuery();
        for(QueryBuilder filter : filters){
            queryFilter.filter(filter);
        }
        QueryBuilder cquery = filters.size() == 0 ? bquery : QueryBuilders.boolQuery().filter(bquery).filter(queryFilter);
        return cquery;
    }
}