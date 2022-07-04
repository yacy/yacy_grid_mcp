/**
 *  WebMapping
 *  Copyright 2011 by Michael Peter Christen
 *  First released 14.04.2011 at http://yacy.net
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public enum WebMapping implements MappingDeclaration {

    // mandatory
    url_s(MappingType.string, true, true, false, true, true, "url of document", true), // a 'sku' is a stock-keeping unit, a unique identifier and a default field in unmodified solr.
    //sku(MappingType.text_en_splitting_tight, true, true, false, true, true, "url of document"), // a 'sku' is a stock-keeping unit, a unique identifier and a default field in unmodified solr.
    crawl_id_s(MappingType.string, true, true, false, true, true, "id of the crawl start", true),
    user_id_s(MappingType.string, true, true, false, true, true, "id of the user of the crawl start", true), // deprecated, see user_id_sxt
    user_id_sxt(MappingType.string, true, true, true, false, false, "ids of all the users who started that crawl", false, "users", "Users", "String", "user"),
    last_modified(MappingType.date, true, true, false, false, false, "last-modified from http header", true), // date document was last modified, needed for media search and /date operator
    load_date_dt(MappingType.date, true, true, false, false, false, "time when resource was loaded", true),
    content_type(MappingType.string, true, true, true, false, false, "mime-type of document", true),
    title(MappingType.text_general, true, true, true, false, true, "content of title tag", true),
    host_s(MappingType.string, true, true, false, false, true, "the host of the url", true, "domains", "Provider", "String", "site"),
    ssld_s(MappingType.string, true, true, false, false, true, "the smart second level domain of the url", true, "domains", "Provider", "String", "site"),
    size_i(MappingType.num_integer, true, true, false, false, false, "the size of the raw source", true),// int size();
    failreason_s(MappingType.string, true, true, false, false, false, "fail reason if a page was not loaded. if the page was loaded then this field is empty", true),
    failtype_s(MappingType.string, true, true, false, false, false, "fail type if a page was not loaded. This field is either empty, 'excl' or 'fail'", true),
    httpstatus_i(MappingType.num_integer, true, true, false, false, false, "html status return code (i.e. \"200\" for ok), -1 if not loaded", true),
    url_file_ext_s(MappingType.string, true, true, false, false, true, "the file name extension", true, "filetypes", "Filetype", "String", "filetype"),
    host_organization_s(MappingType.string, true, true, false, false, true, "either the second level domain or, if a ccSLD is used, the third level domain", true), // needed to search in the url
    inboundlinks_sxt(MappingType.string, true, true, true, false, true, "internal links", true),
    outboundlinks_sxt(MappingType.string, true, true, true, false, false, "external links", true),
    images_sxt(MappingType.string, true, true, true, false, true, "all image links", true),

    // optional but recommended, part of index distribution
    fresh_date_dt(MappingType.date, true, true, false, false, false, "date until resource shall be considered as fresh"),
    referrer_url_s(MappingType.string, true, true, false, false, false, "id of the referrer to this document, discovered during crawling"),// byte[] referrerHash();
    publisher_t(MappingType.text_general, true, true, false, false, true, "the name of the publisher of the document"),// String dc_publisher();
    language_s(MappingType.string, true, true, false, false, false, "the language used in the document"),// byte[] language();
    audiolinkscount_i(MappingType.num_integer, true, true, false, false, false, "number of links to audio resources"),// int laudio();
    videolinkscount_i(MappingType.num_integer, true, true, false, false, false, "number of links to video resources"),// int lvideo();
    applinkscount_i(MappingType.num_integer, true, true, false, false, false, "number of links to application resources"),// int lapp();

    // optional but recommended
    dates_in_content_dts(MappingType.date, true, true, true, false, true, "if date expressions can be found in the content, these dates are listed here as date objects in order of the appearances"),
    dates_in_content_count_i(MappingType.num_integer, true, true, false, false, false, "the number of entries in dates_in_content_sxt"),
    startDates_dts(MappingType.date, true, true, true, false, true, "content of itemprop attributes with content='startDate'"),
    endDates_dts(MappingType.date, true, true, true, false, true, "content of itemprop attributes with content='endDate'"),
    references_i(MappingType.num_integer, true, true, false, false, false, "number of unique http references, should be equal to references_internal_i + references_external_i"),
    references_internal_i(MappingType.num_integer, true, true, false, false, false, "number of unique http references from same host to referenced url"),
    references_external_i(MappingType.num_integer, true, true, false, false, false, "number of unique http references from external hosts"),
    references_exthosts_i(MappingType.num_integer, true, true, false, false, false, "number of external hosts which provide http references"),
    crawldepth_i(MappingType.num_integer, true, true, false, false, false, "crawl depth of web page according to the number of steps that the crawler did to get to this document; if the crawl was started at a root document, then this is equal to the clickdepth"),
    harvestkey_s(MappingType.string, true, true, false, false, false, "key from a harvest process (i.e. the crawl profile hash key) which is needed for near-realtime postprocessing. This shall be deleted as soon as postprocessing has been terminated."),
    http_unique_b(MappingType.bool, true, true, false, false, false, "unique-field which is true when an url appears the first time. If the same url which was http then appears as https (or vice versa) then the field is false"),
    www_unique_b(MappingType.bool, true, true, false, false, false, "unique-field which is true when an url appears the first time. If the same url within the subdomain www then appears without that subdomain (or vice versa) then the field is false"),
    coordinate_p(MappingType.location, true, true, false, false, false, "point in degrees of latitude,longitude as declared in WSG84"),
    coordinate_lat_d(MappingType.num_double, true, false, false, false, false, "coordinate latitude"),
    coordinate_lon_d(MappingType.num_double, true, false, false, false, false, "coordinate longitude"),
    ip_s(MappingType.string, true, true, false, false, false, "ip of host of url (after DNS lookup)"),
    author(MappingType.text_general, true, true, false, false, true, "content of author-tag"),
    author_sxt(MappingType.string, true, true, true, false, false, "content of author-tag as copy-field from author. This is used for facet generation", false, "authors", "Authors", "String", "author"),
    description_txt(MappingType.text_general, true, true, true, false, true, "content of description-tag(s)"),
    description_exact_signature_l(MappingType.num_long, true, true, false, false, false, "the 64 bit hash of the org.apache.solr.update.processor.Lookup3Signature of description, used to compute description_unique_b"),
    description_unique_b(MappingType.bool, true, true, false, false, false, "flag shows if description is unique within all indexable documents of the same host with status code 200; if yes and another document appears with same description, the unique-flag is set to false"),
    keywords(MappingType.text_general, true, true, false, false, true, "content of keywords tag; words are separated by space"),
    charset_s(MappingType.string, true, true, false, false, false, "character encoding"),
    wordcount_i(MappingType.num_integer, true, true, false, false, false, "number of words in visible area"),
    linkscount_i(MappingType.num_integer, true, true, false, false, false, "number of all outgoing links; including linksnofollowcount_i"),
    linksnofollowcount_i(MappingType.num_integer, true, true, false, false, false, "number of all outgoing inks with nofollow tag"),
    inboundlinkscount_i(MappingType.num_integer, true, true, false, false, false, "number of outgoing inbound (to same domain) links; including inboundlinksnofollowcount_i"),
    inboundlinksnofollowcount_i(MappingType.num_integer, true, true, false, false, false, "number of outgoing inbound (to same domain) links with nofollow tag"),
    outboundlinkscount_i(MappingType.num_integer, true, true, false, false, false, "number of outgoing outbound (to other domain) links, including outboundlinksnofollowcount_i"),
    outboundlinksnofollowcount_i(MappingType.num_integer, true, true, false, false, false, "number of outgoing outbound (to other domain) links with nofollow tag"),
    imagescount_i(MappingType.num_integer, true, true, false, false, false, "number of images"),
    responsetime_i(MappingType.num_integer, true, true, false, false, false, "response time of target server in milliseconds"),
    text_t(MappingType.text_general, true, true, false, false, true, "all visible text"),
    synonyms_sxt(MappingType.string, true, true, true, false, true, "additional synonyms to the words in the text"),
    h1_txt(MappingType.text_general, true, true, true, false, true, "h1 header"),
    h2_txt(MappingType.text_general, true, true, true, false, true, "h2 header"),
    h3_txt(MappingType.text_general, true, true, true, false, true, "h3 header"),
    h4_txt(MappingType.text_general, true, true, true, false, true, "h4 header"),
    h5_txt(MappingType.text_general, true, true, true, false, true, "h5 header"),
    h6_txt(MappingType.text_general, true, true, true, false, true, "h6 header"),
    score_l(MappingType.num_long, true, true, false, false, false, "custom score"),

    // optional values, not part of standard YaCy handling (but useful for external applications)
    collection_sxt(MappingType.string, true, true, true, false, false, "tags that are attached to crawls/index generation to separate the search result into user-defined subsets", false,
            "collections", "Collections", "String", "collection"),
    csscount_i(MappingType.num_integer, true, true, false, false, false, "number of entries in css_tag_txt and css_url_txt"),
    css_tag_sxt(MappingType.string, true, true, true, false, false, "full css tag with normalized url"),
    css_url_sxt(MappingType.string, true, true, true, false, false, "normalized urls within a css tag"),
    scripts_sxt(MappingType.string, true, true, true, false, false, "normalized urls within a scripts tag"),
    scriptscount_i(MappingType.num_integer, true, true, false, false, false, "number of entries in scripts_sxt"),
    // encoded as binary value into an integer:
            // bit  0: "all" contained in html header meta
            // bit  1: "index" contained in html header meta
            // bit  2: "follow" contained in html header meta
            // bit  3: "noindex" contained in html header meta
            // bit  4: "nofollow" contained in html header meta
            // bit  5: "noarchive" contained in html header meta
            // bit  8: "all" contained in http header X-Robots-Tag
            // bit  9: "noindex" contained in http header X-Robots-Tag
            // bit 10: "nofollow" contained in http header X-Robots-Tag
            // bit 11: "noarchive" contained in http header X-Robots-Tag
            // bit 12: "nosnippet" contained in http header X-Robots-Tag
            // bit 13: "noodp" contained in http header X-Robots-Tag
            // bit 14: "notranslate" contained in http header X-Robots-Tag
            // bit 15: "noimageindex" contained in http header X-Robots-Tag
            // bit 16: "unavailable_after" contained in http header X-Robots-Tag
    robots_i(MappingType.num_integer, true, true, false, false, false, "content of <meta name=\"robots\" content=#content#> tag and the \"X-Robots-Tag\" HTTP property"),
    metagenerator_t(MappingType.text_general, true, true, false, false, false, "content of <meta name=\"generator\" content=#content#> tag"),
    inboundlinks_anchortext_txt(MappingType.text_general, true, true, true, false, true, "internal links, the visible anchor text"),
    outboundlinks_anchortext_txt(MappingType.text_general, true, true, true, false, true, "external links, the visible anchor text"),

    icons_sxt(MappingType.string, true, true, true, false, true, "all icon links"),
    icons_rel_sxt(MappingType.string, true, true, true, false, false, "all icon links relationships space separated (e.g.. 'icon apple-touch-icon')"),
    icons_sizes_sxt(MappingType.num_integer, true, true, true, false, false, "all icon sizes space separated (e.g. '16x16 32x32')"),

    images_text_t(MappingType.text_general, true, true, false, false, true, "all text/words appearing in image alt texts or the tokenized url"),
    images_alt_sxt(MappingType.string, true, true, true, false, true, "all image link alt tag"), // no need to index this; don't turn it into a txt field; use images_text_t instead
    images_height_val(MappingType.num_integer, true, true, true, false, false, "size of images:height"),
    images_width_val(MappingType.num_integer, true, true, true, false, false, "size of images:width"),
    images_pixel_val(MappingType.num_integer, true, true, true, false, false, "size of images as number of pixels (easier for a search restriction than with and height)"),
    images_withalt_i(MappingType.num_integer, true, true, false, false, false, "number of image links with alt tag"),
    htags_i(MappingType.num_integer, true, true, false, false, false, "binary pattern for the existance of h1..h6 headlines"),
    canonical_s(MappingType.string, true, true, false, false, false, "url inside the canonical link element"),
    canonical_equal_sku_b(MappingType.bool, true, true, false, false, false, "flag shows if the url in canonical_t is equal to sku"),
    refresh_s(MappingType.string, true, true, false, false, false, "link from the url property inside the refresh link element"),
    li_txt(MappingType.text_general, true, true, true, false, true, "all texts in <li> tags"),
    licount_i(MappingType.num_integer, true, true, false, false, false, "number of <li> tags"),
    dt_txt(MappingType.text_general, true, true, true, false, true, "all texts in <dt> tags"),
    dtcount_i(MappingType.num_integer, true, true, false, false, false, "number of <dt> tags"),
    dd_txt(MappingType.text_general, true, true, true, false, true, "all texts in <dd> tags"),
    ddcount_i(MappingType.num_integer, true, true, false, false, false, "number of <dd> tags"),
    article_txt(MappingType.text_general, true, true, true, false, true, "all texts in <article> tags"),
    articlecount_i(MappingType.num_integer, true, true, false, false, false, "number of <article> tags"),
    bold_txt(MappingType.text_general, true, true, true, false, true, "all texts inside of <b> or <strong> tags. no doubles. listed in the order of number of occurrences in decreasing order"),
    boldcount_i(MappingType.num_integer, true, true, false, false, false, "total number of occurrences of <b> or <strong>"),
    italic_txt(MappingType.text_general, true, true, true, false, true, "all texts inside of <i> tags. no doubles. listed in the order of number of occurrences in decreasing order"),
    italiccount_i(MappingType.num_integer, true, true, false, false, false, "total number of occurrences of <i>"),
    underline_txt(MappingType.text_general, true, true, true, false, true, "all texts inside of <u> tags. no doubles. listed in the order of number of occurrences in decreasing order"),
    underlinecount_i(MappingType.num_integer, true, true, false, false, false, "total number of occurrences of <u>"),
    flash_b(MappingType.bool, true, true, false, false, false, "flag that shows if a swf file is linked"),
    frames_sxt(MappingType.string, true, true, true, false, false, "list of all links to frames"),
    framesscount_i(MappingType.num_integer, true, true, false, false, false, "number of frames_txt"),
    iframes_sxt(MappingType.string, true, true, true, false, false, "list of all links to iframes"),
    iframesscount_i(MappingType.num_integer, true, true, false, false, false, "number of iframes_txt"),

    hreflang_url_sxt(MappingType.string, true, true, true, false, false, "url of the hreflang link tag, see http://support.google.com/webmasters/bin/answer.py?hl=de&answer=189077"),
    hreflang_cc_sxt(MappingType.string, true, true, true, false, false, "country code of the hreflang link tag, see http://support.google.com/webmasters/bin/answer.py?hl=de&answer=189077"),
    navigation_url_sxt(MappingType.string, true, true, true, false, false, "page navigation url, see http://googlewebmastercentral.blogspot.de/2011/09/pagination-with-relnext-and-relprev.html"),
    navigation_type_sxt(MappingType.string, true, true, true, false, false, "page navigation rel property value, can contain one of {top,up,next,prev,first,last}"),
    publisher_url_s(MappingType.string, true, true, false, false, false, "publisher url as defined in http://support.google.com/plus/answer/1713826?hl=de"),

    url_protocol_s(MappingType.string, true, true, false, false, false, "the protocol of the url", false, "protocols", "Protocol", "String", "protocol"),
    url_file_name_s(MappingType.string, true, true, false, false, true, "the file name (which is the string after the last '/' and before the query part from '?' on) without the file extension"),
    url_file_name_tokens_t(MappingType.text_general, true, true, false, false, true, "tokens generated from url_file_name_s which can be used for better matching and result boosting"),
    url_paths_count_i(MappingType.num_integer, true, true, false, false, false, "number of all path elements in the url hpath (see: http://www.ietf.org/rfc/rfc1738.txt) without the file name"),
    url_paths_sxt(MappingType.string, true, true, true, false, true, "all path elements in the url hpath (see: http://www.ietf.org/rfc/rfc1738.txt) without the file name"),
    url_parameter_i(MappingType.num_integer, true, true, false, false, false, "number of key-value pairs in search part of the url"),
    url_parameter_key_sxt(MappingType.string, true, true, true, false, false, "the keys from key-value pairs in the search part of the url"),
    url_parameter_value_sxt(MappingType.string, true, true, true, false, false, "the values from key-value pairs in the search part of the url"),
    url_chars_i(MappingType.num_integer, true, true, false, false, false, "number of all characters in the url == length of sku field"),

    host_dnc_s(MappingType.string, true, true, false, false, true, "the Domain Class Name, either the TLD or a combination of ccSLD+TLD if a ccSLD is used."),
    host_organizationdnc_s(MappingType.string, true, true, false, false, true, "the organization and dnc concatenated with '.'"),
    host_subdomain_s(MappingType.string, true, true, false, false, true, "the remaining part of the host without organizationdnc"),
    host_extent_i(MappingType.num_integer, true, true, false, false, false, "number of documents from the same host; can be used to measure references_internal_i for likelihood computation"),

    title_count_i(MappingType.num_integer, true, true, false, false, false, "number of titles (counting the 'title' field) in the document"),
    title_chars_val(MappingType.num_integer, true, true, true, false, false, "number of characters for each title"),
    title_words_val(MappingType.num_integer, true, true, true, false, false, "number of words in each title"),

    description_count_i(MappingType.num_integer, true, true, false, false, false, "number of descriptions in the document. Its not counting the 'description' field since there is only one. But it counts the number of descriptions that appear in the document (if any)"),
    description_chars_val(MappingType.num_integer, true, true, true, false, false, "number of characters for each description"),
    description_words_val(MappingType.num_integer, true, true, true, false, false, "number of words in each description"),

    h1_i(MappingType.num_integer, true, true, false, false, false, "number of h1 header lines"),
    h2_i(MappingType.num_integer, true, true, false, false, false, "number of h2 header lines"),
    h3_i(MappingType.num_integer, true, true, false, false, false, "number of h3 header lines"),
    h4_i(MappingType.num_integer, true, true, false, false, false, "number of h4 header lines"),
    h5_i(MappingType.num_integer, true, true, false, false, false, "number of h5 header lines"),
    h6_i(MappingType.num_integer, true, true, false, false, false, "number of h6 header lines"),

    schema_org_breadcrumb_i(MappingType.num_integer, true, true, false, false, false, "number of itemprop=\"breadcrumb\" appearances in div tags"),
    opengraph_title_t(MappingType.text_general, true, true, false, false, true, "Open Graph Metadata from og:title metadata field, see http://ogp.me/ns#"),
    opengraph_type_s(MappingType.text_general, true, true, false, false, false, "Open Graph Metadata from og:type metadata field, see http://ogp.me/ns#"),
    opengraph_url_s(MappingType.text_general, true, true, false, false, false, "Open Graph Metadata from og:url metadata field, see http://ogp.me/ns#"),
    opengraph_image_s(MappingType.text_general, true, true, false, false, false, "Open Graph Metadata from og:image metadata field, see http://ogp.me/ns#"),

    // link structure for ranking
    cr_host_count_i(MappingType.num_integer, true, true, false, false, false, "the number of documents within a single host"),
    cr_host_chance_d(MappingType.num_double, true, true, false, false, false, "the chance to click on this page when randomly clicking on links within on one host"),
    cr_host_norm_i(MappingType.num_integer, true, true, false, false, false, "normalization of chance: 0 for lower halve of cr_host_count_i urls, 1 for 1/2 of the remaining and so on. the maximum number is 10"),

    // custom rating; values to influence the ranking in combination with boost rules
    rating_i(MappingType.num_integer, true, true, false, false, false, "custom rating; to be set with external rating information"),

    // special values; can only be used if '_val' type is defined in schema file; this is not standard
    bold_val(MappingType.num_integer, true, true, true, false, false, "number of occurrences of texts in bold_txt"),
    italic_val(MappingType.num_integer, true, true, true, false, false, "number of occurrences of texts in italic_txt"),
    underline_val(MappingType.num_integer, true, true, true, false, false, "number of occurrences of texts in underline_txt"),
    ext_cms_txt(MappingType.text_general, true, true, true, false, false, "names of cms attributes; if several are recognized then they are listen in decreasing order of number of matching criterias"),
    ext_cms_val(MappingType.num_integer, true, true, true, false, false, "number of attributes that count for a specific cms in ext_cms_txt"),
    ext_ads_txt(MappingType.text_general, true, true, true, false, false, "names of ad-servers/ad-services"),
    ext_ads_val(MappingType.num_integer, true, true, true, false, false, "number of attributes counts in ext_ads_txt"),
    ext_community_txt(MappingType.text_general, true, true, true, false, false, "names of recognized community functions"),
    ext_community_val(MappingType.num_integer, true, true, true, false, false, "number of attribute counts in attr_community"),
    ext_maps_txt(MappingType.text_general, true, true, true, false, false, "names of map services"),
    ext_maps_val(MappingType.num_integer, true, true, true, false, false, "number of attribute counts in ext_maps_txt"),
    ext_tracker_txt(MappingType.text_general, true, true, true, false, false, "names of tracker server"),
    ext_tracker_val(MappingType.num_integer, true, true, true, false, false, "number of attribute counts in ext_tracker_txt"),
    ext_title_txt(MappingType.text_general, true, true, true, false, false, "names matching title expressions"),
    ext_title_val(MappingType.num_integer, true, true, true, false, false, "number of matching title expressions"),
    vocabularies_sxt(MappingType.string, true, true, true, false, false, "collection of all vocabulary names that have a matcher in the document - use this to boost with vocabularies"),
    ld_o(MappingType.object, false, true, false, false, false, "JSON-LD object with node object"),
    ld_s(MappingType.string, false, true, false, false, false, "JSON-LD object with node object as string"),
    ld_context_sxt(MappingType.string, false, true, true, false, false, "all context indentifiers of ld_s");

    public final static String CORE_NAME = "collection1"; // this was the default core name up to Solr 4.4.0. This default name was stored in CoreContainer.DEFAULT_DEFAULT_CORE_NAME but was removed in Solr 4.5.0

    public final static String VOCABULARY_PREFIX = "vocabulary_"; // collects all terms that appear for each vocabulary
    public final static String VOCABULARY_TERMS_SUFFIX = "_sxt"; // suffix for the term collector that start with VOCABULARY_PREFIX - middle part is vocabulary name
    public final static String VOCABULARY_COUNT_SUFFIX = "_i"; // suffix for the term counter (>=1) that start with VOCABULARY_PREFIX - middle part is vocabulary name
    public final static String VOCABULARY_LOGCOUNT_SUFFIX = "_log_i"; // log2(VOCABULARY_COUNT)] -- can be used for ranking boosts based on the number of occurrences
    public final static String VOCABULARY_LOGCOUNTS_SUFFIX = "_log_val"; // all integers from [0 to log2(VOCABULARY_COUNT)] -- can be used for ranking boosts based on the number of occurrences

    private Mapping mapping;

    private WebMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, false);
    }

    private WebMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory);
    }

    private WebMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory,
                       final String facetname, final String displayname, final String facettype, final String facetmodifier) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory, facetname, displayname, facettype, facetmodifier);
    }

    @Override
    public final Mapping getMapping() {
        return this.mapping;
    }

    public static final Pattern catchall_pattern = Pattern.compile(".*");

    public static Map<String, Pattern> collectionParser(final String collectionString) {
        if (collectionString == null || collectionString.length() == 0) return new HashMap<>();
        final String[] cs = collectionString.split(",");
        final Map<String, Pattern> cm = new LinkedHashMap<>();
        for (final String c: cs) {
            final int p = c.indexOf(':');
            if (p < 0) cm.put(c, catchall_pattern); else cm.put(c.substring(0, p), Pattern.compile(c.substring(p + 1)));
        }
        return cm;
    }

    /**
     * Graph attributes are used by the parser to create a copy of a document which contains only links and references
     * to the indexed document identification. That graph document is used by the crawler to move on with crawling.
     */
    public final static WebMapping[] GRAPH_ATTRIBUTES = new WebMapping[]{
        WebMapping.url_s,
        WebMapping.url_protocol_s,
        WebMapping.url_file_name_s,
        WebMapping.url_file_ext_s,
        WebMapping.inboundlinkscount_i,
        WebMapping.inboundlinks_sxt,
        WebMapping.inboundlinks_anchortext_txt,
        WebMapping.inboundlinksnofollowcount_i,
        WebMapping.outboundlinkscount_i,
        WebMapping.outboundlinks_sxt,
        WebMapping.outboundlinks_anchortext_txt,
        WebMapping.outboundlinksnofollowcount_i,
        WebMapping.imagescount_i,
        WebMapping.images_sxt,
        WebMapping.images_text_t,
        WebMapping.images_alt_sxt,
        WebMapping.images_height_val,
        WebMapping.images_width_val,
        WebMapping.images_pixel_val,
        WebMapping.canonical_s,
        WebMapping.frames_sxt,
        WebMapping.framesscount_i,
        WebMapping.iframes_sxt,
        WebMapping.iframesscount_i
    };

    /**
     * helper main method to generate a mapping in elasticsearch.
     * To test this, upload the result of this main method to elasticsearch with the following line:
     * curl -XDELETE 'http://elastic:changeme@localhost:9200/web'
     * curl -XPUT http://elastic:changeme@localhost:9200/web --data-binary "@mapping.json"
     * that prepares the index to take a json index file which can be generated with i.e.
     * curl -X POST -F "sourcebytes=@publicplan.de.warc.gz;type=application/octet-stream" -F "flatfile=true" -F "elastic=true" -o "publicplan.de.elastic"  http://127.0.0.1:8500/yacy/grid/parser/parser.json
     * then, index the resulting file publicplan.de.elastic with:
     * curl -s -XPOST http://elastic:changeme@localhost:9200/web/index/_bulk --data-binary "@publicplan.de.elastic"
     * the number of documents in the index is then
     * curl http://elastic:changeme@localhost:9200/web/_count
     * @param args
     */
    public static void main(final String[] args) {
        System.out.println(Mapping.elasticsearchMapping(GridIndex.DEFAULT_INDEXNAME_WEB).toString(2));
    }
}

