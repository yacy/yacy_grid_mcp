/**
 *  CrawlerMapping
 *  Copyright 6.3.2018 by Michael Peter Christen, @0rb1t3r
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

public enum CrawlerMapping implements MappingDeclaration {

    crawl_id_s(MappingType.string, true, true, false, true, true, "id of the crawl start", true),
    mustmatch_s(MappingType.string, true, true, false, true, true, "must-match pattern string of the crawl start", true),
    collection_sxt(MappingType.string, true, true, true, false, false, "tags that are attached to crawls/index generation", false, "collections", "Collections", "String", "collection"),
    start_url_s(MappingType.string, true, true, false, true, true, "The start URL", true),
    start_ssld_s(MappingType.string, true, true, false, true, true, "The smart second level domain of the start URL", true),
    init_date_dt(MappingType.date, true, true, false, false, false, "date when the crawl was started"),
    status_date_dt(MappingType.date, true, true, false, false, false, "date of latest status change"),
    status_s(MappingType.string, true, true, false, true, true, "current crawl status", true),
    url_s(MappingType.string, true, true, false, true, true, "url of the document", true),
    comment_t(MappingType.text_general, true, true, false, false, true, "comment to crawl status; error messages etc.");

    private Mapping mapping;

    private CrawlerMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, false);
    }

    private CrawlerMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory);
    }

    private CrawlerMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory,
                       final String facetname, final String displayname, final String facettype, final String facetmodifier) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory, facetname, displayname, facettype, facetmodifier);
    }

    @Override
    public final Mapping getMapping() {
        return this.mapping;
    }


}
