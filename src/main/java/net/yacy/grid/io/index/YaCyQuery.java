/**
 *  YaCyQuery
 *  Copyright 03.02.2018 by Michael Peter Christen, @orbiterlab
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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.search.MatchQuery.ZeroTermsQuery;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.yacy.grid.io.index.BoostsFactory.Boosts;
import net.yacy.grid.mcp.Data;
import net.yacy.grid.tools.Classification;
import net.yacy.grid.tools.DateParser;
import net.yacy.grid.tools.Logger;

public class YaCyQuery {

    private static char space = ' ';
    private static char sq = '\'';
    private static char dq = '"';
    //private static String seps = ".:;#*`,!$%()=?^<>/&";


    public final static String FACET_DEFAULT_PARAMETER = "host_s,url_file_ext_s,author_sxt,dates_in_content_dts,language_s,url_protocol_s,collection_sxt";

    public List<String> collectionTextFilterQuery(boolean noimages) {
        final ArrayList<String> fqs = new ArrayList<>();

        // add filter to prevent that results come from failed urls
        fqs.add(WebMapping.httpstatus_i.getMapping().name() + ":" + HttpStatus.SC_OK);
        if (noimages) {
            fqs.add("-" + WebMapping.content_type.getMapping().name() + ":(image/*)");
            fqs.add("-" + WebMapping.url_file_ext_s.getMapping().name() + ":(jpg OR png OR gif)");
        }

        return fqs;
    }

    private final static Pattern term4ORPattern = Pattern.compile("(?:^| )(\\S*(?: OR \\S*)+)(?: |$)"); // Pattern.compile("(^\\s*(?: OR ^\\s*+)+)");
    private final static Pattern tokenizerPattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*"); // tokenizes Strings into terms respecting quoted parts

    public QueryBuilder queryBuilder;
    public Date since;
    public Date until;
    public String[] collections;
    public Boosts boosts;
    public HashSet<String> yacyModifiers;
    public HashSet<String> positiveBag, negativeBag;

    public YaCyQuery(String q, String[] collections, Classification.ContentDomain contentdom, int timezoneOffset) {
        // default values for since and util
        this.since = new Date(0);
        this.until = new Date(Long.MAX_VALUE);
        this.collections = collections;
        this.boosts = Data.boostsFactory.getBoosts(); // creates a clone of a standard boost mapping
        this.yacyModifiers = new HashSet<>();
        this.positiveBag = new HashSet<>();
        this.negativeBag = new HashSet<>();

        // parse the query string
        this.queryBuilder = preparse(q, timezoneOffset);

        // handle constraints and document types
        if (this.collections != null && this.collections.length > 0) {
            // attach collection constraint
            BoolQueryBuilder qb = QueryBuilders.boolQuery().must(this.queryBuilder);
            BoolQueryBuilder collectionQuery = QueryBuilders.boolQuery();
            for (String s: this.collections) {
                int p = s.indexOf('^');
                TermQueryBuilder tq;
                if (p >= 0) {
                    tq = QueryBuilders.termQuery(WebMapping.collection_sxt.getMapping().name(), s.substring(0, p));
                    tq.boost(Float.parseFloat(s.substring(p + 1)));
                } else {
                    tq = QueryBuilders.termQuery(WebMapping.collection_sxt.getMapping().name(), s);
                }
                collectionQuery.should(tq);
            }
            qb.must(QueryBuilders.constantScoreQuery(collectionQuery));
            this.queryBuilder = qb;
        }

        // add content domain
        if (contentdom == Classification.ContentDomain.IMAGE) {
            BoolQueryBuilder qb = QueryBuilders.boolQuery().must(this.queryBuilder);
            qb.must(QueryBuilders.rangeQuery(WebMapping.imagescount_i.getMapping().name()).gt(new Integer(0)));
            this.queryBuilder = qb;
        }
        if (contentdom == Classification.ContentDomain.VIDEO) {
            BoolQueryBuilder qb = QueryBuilders.boolQuery().must(this.queryBuilder);
            qb.must(QueryBuilders.rangeQuery(WebMapping.videolinkscount_i.getMapping().name()).gt(new Integer(0)));
            this.queryBuilder = qb;
        }

        // ready
        //Logger.info(this.getClass(), "YaCyQuery: " + this.queryBuilder.toString());
        Logger.info(this.getClass(), "YaCyQuery: " + q);
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
        q = q.replaceAll("\\*:\\*", ""); // no solr hacking here
        q = q.replaceAll(" AND ", " "); // AND is default
        return q;
    }


    private QueryBuilder preparse(String q, int timezoneOffset) {
        // detect usage of OR connector usage.
        q = fixQueryMistakes(q);
        List<String> terms = splitIntoORGroups(q); // OR binds stronger than AND
        //if (terms.size() == 0) QueryBuilders.constantScoreQuery(QueryBuilders.matchAllQuery());

        // special handling: we don't need a boolean query builder on top; just return one parse object
        if (terms.size() == 1) return parse(terms.get(0), timezoneOffset);

        // generic handling: all of those OR groups MUST match. That is done with the must query
        BoolQueryBuilder aquery = QueryBuilders.boolQuery();
        for (String t: terms) {
            QueryBuilder partial = parse(t, timezoneOffset);
            aquery.must(partial);
        }
        return aquery;
    }

    private final static String[][] modifierTypes = new String[][] {
        new String[] {"id", "_id"},
        new String[] {"intitle", WebMapping.title.getMapping().name()},
        new String[] {"inurlinurl", WebMapping.url_file_name_tokens_t.getMapping().name()},
        new String[] {"filetype", WebMapping.url_file_ext_s.getMapping().name()},
        new String[] {"site", WebMapping.host_s.getMapping().name()},
        new String[] {"author", WebMapping.author_sxt.getMapping().name()},
        new String[] {"yacy", "_id"}
    };

    private QueryBuilder parse(String q, int timezoneOffset) {
        // detect usage of OR ORconnective usage. Because of the preparse step we will have only OR or only AND here.
        q = q.replaceAll(" AND ", " "); // AND is default
        boolean ORconnective = q.indexOf(" OR ") >= 0;
        q = q.replaceAll(" OR ", " "); // if we know that all terms are OR, we remove that and apply it later. Because we splitted into OR groups it is right to use OR here only

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
        Multimap<String, String> modifier = HashMultimap.create();
        Set<String> constraints_positive = new HashSet<>();
        Set<String> constraints_negative = new HashSet<>();
        for (String t: qe) {
            if (t.length() == 0) continue;
            if (t.startsWith("/")) {
                constraints_positive.add(t.substring(1));
                continue;
            } else if (t.startsWith("-/")) {
                constraints_negative.add(t.substring(2));
                continue;
            } else if (t.indexOf(':') > 0) {
                int p = t.indexOf(':');
                String name = t.substring(0, p).toLowerCase();
                String value = t.substring(p + 1);
                if (value.indexOf('|') > 0) {
                	String[] values = value.split("\\|");
                	for (String v: values) {
                    	modifier.put(name, v);
                	}
                } else {
                	modifier.put(name, value);
                }
                continue;
            } else {
                // patch characters that will confuse elasticsearch or have a different meaning
                boolean negative = t.startsWith("-");
                if (negative) t = t.substring(1);
                if (t.length() == 0) continue;
                if ((t.charAt(0) == dq && t.charAt(t.length() - 1) == dq) || (t.charAt(0) == sq && t.charAt(t.length() - 1) == sq)) {
                    t = t.substring(1, t.length() - 1);
                    if (negative) {
                        text_negative_filter.add(t);
                        this.negativeBag.add(t);
                    } else {
                        text_positive_filter.add(t);
                        this.positiveBag.add(t);
                    }
                } else if (t.indexOf('-') > 0) {
                    // this must be handled like a quoted string without the minus
                    t = t.replace('-', space);
                    if (negative) {
                        text_negative_filter.add(t);
                        this.negativeBag.add(t);
                    } else {
                        text_positive_filter.add(t);
                        this.positiveBag.add(t);
                    }
                } else {
                    if (negative) {
                        text_negative_match.add(t);
                        this.negativeBag.add(t);
                    } else {
                        text_positive_match.add(t);
                        this.positiveBag.add(t);
                    }
                }
                continue;
            }
        }

        // construct a ranking
        if (modifier.containsKey("boost")) {
            this.boosts.patchWithModifier(modifier.get("boost").iterator().next());
        }

        // compose query for text
        List<QueryBuilder> queries = new ArrayList<>();
        // fuzzy matching
        if (!text_positive_match.isEmpty()) queries.add(simpleQueryBuilder(String.join(" ", text_positive_match), ORconnective, this.boosts));
        if (!text_negative_match.isEmpty()) queries.add(QueryBuilders.boolQuery().mustNot(simpleQueryBuilder(String.join(" ", text_negative_match), ORconnective, this.boosts)));
        // exact matching
        for (String text: text_positive_filter) {
            queries.add(exactMatchQueryBuilder(text, this.boosts));
        }
        for (String text: text_negative_filter) {
            queries.add(QueryBuilders.boolQuery().mustNot(exactMatchQueryBuilder(text, this.boosts)));
        }

        // apply modifiers
        Collection<String> values;
        modifier_handling: for (String[] modifierType: modifierTypes) {
            String modifier_name = modifierType[0];
            String index_name = modifierType[1];

            if ((values = modifier.get(modifier_name)).size() > 0) {
                if (modifier_name.equals("yacy")) {
                    values.forEach(y -> this.yacyModifiers.add(y));
                    continue modifier_handling;
                }
                if (modifier_name.equals("site") && values.size() == 1) {
                    String host = values.iterator().next();
                    if (host.startsWith("www.")) values.add(host.substring(4)); else values.add("www." + host);
                }
                queries.add(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery(index_name, values)));
                continue modifier_handling;
            }

            if ((values = modifier.get("-" + modifier_name)).size() > 0) {
                if (modifier_name.equals("site") && values.size() == 1) {
                    String host = values.iterator().next();
                    if (host.startsWith("www.")) values.add(host.substring(4)); else values.add("www." + host);
                }
                queries.add(QueryBuilders.boolQuery().mustNot(QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery(index_name, values))));
                continue modifier_handling;
            }
        }
        if (modifier.containsKey("collection") && (this.collections == null || this.collections.length == 0)) {
            Collection<String> c = modifier.get("collection");
            this.collections = c.toArray(new String[c.size()]);
        }
        if (modifier.containsKey("daterange")) {
            String dr = modifier.get("daterange").iterator().next();
            if (dr.length() > 0) {
                String from_to[] = dr.endsWith("..") ? new String[]{dr.substring(0, dr.length() - 2), ""} : dr.startsWith("..") ? new String[]{"", dr.substring(2)} : dr.split("\\.\\.");
                if (from_to.length == 2)  {
                    if (from_to[0] != null && from_to[0].length() > 0) try {
                        modifier.put("since", DateParser.dayDateFormat.format(DateParser.parse(from_to[0], timezoneOffset).getTime()));
                    } catch (ParseException e) {}
                    if (from_to[1] != null && from_to[1].length() > 0) try {
                        modifier.put("until", DateParser.dayDateFormat.format(DateParser.parse(from_to[1], timezoneOffset).getTime()));
                    } catch (ParseException e) {}
                }
            }
        }
        if (modifier.containsKey("since")) try {
            Calendar since = DateParser.parse(modifier.get("since").iterator().next(), timezoneOffset);
            this.since = since.getTime();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(WebMapping.last_modified.getMapping().name()).from(DateParser.formatGSAFS(this.since));
            if (modifier.containsKey("until")) {
                Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
                if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                    // until must be the day which is included in results.
                    // To get the result within the same day, we must add one day.
                    until.add(Calendar.DATE, 1);
                }
                this.until = until.getTime();
                rangeQuery.to(DateParser.formatGSAFS(this.until));
            } else {
                this.until = new Date(Long.MAX_VALUE);
            }
            queries.add(rangeQuery);
        } catch (ParseException e) {} else if (modifier.containsKey("until")) try {
            Calendar until = DateParser.parse(modifier.get("until").iterator().next(), timezoneOffset);
            if (until.get(Calendar.HOUR) == 0 && until.get(Calendar.MINUTE) == 0) {
                // until must be the day which is included in results.
                // To get the result within the same day, we must add one day.
                until.add(Calendar.DATE, 1);
            }
            this.until = until.getTime();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(WebMapping.last_modified.getMapping().name()).to(DateParser.formatGSAFS(this.until));
            queries.add(rangeQuery);
        } catch (ParseException e) {}

        // now combine queries with OR or AND operator

        // simple case where we have one query only
        if (queries.size() == 1) {
            return queries.iterator().next();
        }

        BoolQueryBuilder b = QueryBuilders.boolQuery();
        for (QueryBuilder filter : queries){
            if (ORconnective) b.should(filter); else b.must(filter);
        }
        if (ORconnective) b.minimumShouldMatch(1);

        return b;
    }

    public static QueryBuilder simpleQueryBuilder(String q, boolean or, Boosts boosts) {
        if (q.equals("yacyall")) return new MatchAllQueryBuilder();
        final MultiMatchQueryBuilder qb = QueryBuilders
                .multiMatchQuery(q)
                .operator(or ? Operator.OR : Operator.AND)
                .zeroTermsQuery(ZeroTermsQuery.ALL);
        boosts.forEach((mapping, boost) -> qb.field(mapping.getMapping().name(), boost));
        return qb;
    }

    public static QueryBuilder exactMatchQueryBuilder(String q, Boosts boosts) {
        BoolQueryBuilder qb = QueryBuilders.boolQuery();
        boosts.forEach((mapping, boost) -> qb.should(QueryBuilders.termQuery(mapping.getMapping().name(), q)));
        qb.minimumShouldMatch(1);
        return qb;
    }

}
