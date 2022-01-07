/**
 *  CrawlstartMapping
 *  Copyright 9.3.2018 by Michael Peter Christen, @orbiterlab
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

public enum CrawlstartMapping implements MappingDeclaration {

    crawl_id_s(MappingType.string, true, true, false, true, true, "id of the crawl start", true),
    mustmatch_s(MappingType.string, true, true, false, true, true, "must-match pattern string of the crawl start", true),
    collection_sxt(MappingType.string, true, true, true, false, false, "tags that are attached to crawls/index generation", false, "collections", "Collections", "String", "collection"),
    start_url_s(MappingType.string, true, true, false, true, true, "The start URL", true),
    start_ssld_s(MappingType.string, true, true, false, true, true, "The smart second level domain of the start URL", true),
    init_date_dt(MappingType.date, true, true, false, false, false, "date when the crawl was started"),
    data_o(MappingType.object, false, true, false, true, true, "data object describing the crawl as defined in the api call", true);

    private Mapping mapping;

    private CrawlstartMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, false);
    }

    private CrawlstartMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory);
    }

    private CrawlstartMapping(final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory,
                       final String facetname, final String displayname, final String facettype, final String facetmodifier) {
        this.mapping = new Mapping(this.name(), type, indexed, stored, multiValued, omitNorms, searchable, comment, mandatory, facetname, displayname, facettype, facetmodifier);
    }

    @Override
    public final Mapping getMapping() {
        return this.mapping;
    }
}
