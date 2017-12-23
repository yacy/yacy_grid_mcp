package net.yacy.grid.io.index;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MatchQuery.ZeroTermsQuery;

public class YaCyQuery {
    
    private static char space = ' ';
    private static char sq = '\'';
    private static char dq = '"';
    private static String seps = ".:;#*`,!$%()=?^<>/&_";
    
    public String query_original;
    private final Set<String> include_words, exclude_words;
    private final ArrayList<String> include_strings, exclude_strings;
    
    public final static String FACET_DEFAULT_PARAMETER = "host_s,url_file_ext_s,author_sxt,dates_in_content_dts,language_s,url_protocol_s,collection_sxt";
    private final static Map<WebMapping, Float> QUERY_DEFAULT_FIELDS = new HashMap<>();
    static {
        QUERY_DEFAULT_FIELDS.put(WebMapping.host_organization_s, 1000.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.url_paths_sxt, 500.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.url_file_name_s, 200.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.title, 100.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h1_txt, 50.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h2_txt, 10.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h3_txt, 6.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h4_txt, 3.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.text_t, 1.0f);
    }

    public static QueryBuilder simpleQueryBuilder(String q) {
        QueryBuilder qb = QueryBuilders
                .multiMatchQuery(q)
                .operator(Operator.AND)
                .zeroTermsQuery(ZeroTermsQuery.ALL)
                .field(WebMapping.host_organization_s.getSolrFieldName(), 1000.0f)
                .field(WebMapping.url_paths_sxt.getSolrFieldName(), 500.0f)
                .field(WebMapping.url_file_name_s.getSolrFieldName(), 200.0f)
                .field(WebMapping.title.getSolrFieldName(), 100.0f)
                .field(WebMapping.h1_txt.getSolrFieldName(), 50.0f)
                .field(WebMapping.h2_txt.getSolrFieldName(), 10.0f)
                .field(WebMapping.h3_txt.getSolrFieldName(), 6.0f)
                .field(WebMapping.h4_txt.getSolrFieldName(), 3.0f)
                .field(WebMapping.text_t.getSolrFieldName(), 1.0f);
        return qb;
    }
    
    public YaCyQuery(String query) {
        assert query != null;
        this.query_original = query;
        this.include_words = new HashSet<String>();
        this.exclude_words = new HashSet<String>();
        this.include_strings = new ArrayList<String>();
        this.exclude_strings = new ArrayList<String>();

        // remove funny symbols
        int c;
        for (int i = 0; i < seps.length(); i++) {
            while ((c = query.indexOf(seps.charAt(i))) >= 0) {
                query = query.substring(0, c) + (((c + 1) < query.length()) ? (' ' + query.substring(c + 1)) : "");
            }
        }

        // parse first quoted strings
        parseQuery(query, this.include_strings, this.exclude_strings);
        
        // .. end then take these strings apart to generate word lists
        for (String s: this.include_strings) parseQuery(s, this.include_words, this.include_words);
        for (String s: this.exclude_strings) parseQuery(s, this.exclude_words, this.exclude_words);
    }

    private static void parseQuery(String query, Collection<String> include_string, Collection<String> exclude_string) {
        while (query.length() > 0) {
            // parse query
            int p = 0;
            while (p < query.length() && query.charAt(p) == space) p++;
            query = query.substring(p);
            if (query.length() == 0) return;

            // parse phrase
            boolean inc = true;
            if (query.charAt(0) == '-') {
                inc = false;
                query = query.substring(1);
            } else if (query.charAt(0) == '+') {
                inc = true;
                query = query.substring(1);
            }
            if (query.length() == 0) return;
            
            // parse string
            char stop = space;
            if (query.charAt(0) == dq) {
                stop = query.charAt(0);
                query = query.substring(1);
            } else if (query.charAt(0) == sq) {
                stop = query.charAt(0);
                query = query.substring(1);
            }
            p = 0;
            while (p < query.length() && query.charAt(p) != stop) p++;
            String string = query.substring(0, p);
            p++; // go behind the stop character (eats up space, sq and dq)
            query = p < query.length() ? query.substring(p) : "";
            if (string.length() > 0) {
                if (inc) {
                    if (!include_string.contains(string)) include_string.add(string);
                } else {
                    if (!exclude_string.contains(string)) exclude_string.add(string);
                }
            }
        }
        // in case that the include_string contains several entries including 1-char tokens and also more-than-1-char tokens,
        // then remove the 1-char tokens to prevent that we are to strict. This will make it possible to be a bit more fuzzy
        // in the search where it is appropriate
        boolean contains_single = false, contains_multiple = false;
        for (String token: include_string) {
            if (token.length() == 1) contains_single = true; else contains_multiple = true;
        }
        if (contains_single && contains_multiple) {
            Iterator<String> i = include_string.iterator();
            while (i.hasNext()) if (i.next().length() == 1) i.remove();
        }
    }

    /**
     * Search query string (without YaCy specific modifier like site:xxx or /smb)
     * the modifier are held separately in a search paramter modifier
     *
     * @param encodeHTML
     * @return the search query string
     */
    public String getQueryString(final boolean encodeHTML) {
        if (this.query_original == null) return null;
        String ret;
        if (encodeHTML){
            try {
                ret = URLEncoder.encode(this.query_original, StandardCharsets.UTF_8.name());
            } catch (final UnsupportedEncodingException e) {
                ret = this.query_original;
            }
        } else {
            ret = this.query_original;
        }
        return ret;
    }

    /**
     * the include string may be useful (and better) for highlight/snippet computation 
     * @return the query string containing only the positive literals (includes) and without whitespace characters
     */
    public String getIncludeString() {
        if (this.include_strings.size() == 0) return "";
        StringBuilder sb = new StringBuilder(10 * include_strings.size());
        for (String s: this.include_strings) sb.append(s).append(' ');
        return sb.toString().substring(0, sb.length() - 1);
    }
    
    public boolean containsInclude(String word) {
        if (word == null || word.length() == 0) return false;
        
        String t = word.toLowerCase(Locale.ENGLISH);
        return this.include_strings.contains(t) || this.include_words.contains(t);
    }
    
    public List<String> collectionTextFilterQuery(boolean noimages) {
        final ArrayList<String> fqs = new ArrayList<>();

        // add filter to prevent that results come from failed urls
        fqs.add(WebMapping.httpstatus_i.getSolrFieldName() + ":" + HttpStatus.SC_OK);
        if (noimages) {
            fqs.add("-" + WebMapping.content_type.getSolrFieldName() + ":(image/*)");
            fqs.add("-" + WebMapping.url_file_ext_s.getSolrFieldName() + ":(jpg OR png OR gif)");
        }
        
        return fqs;
    }

}
