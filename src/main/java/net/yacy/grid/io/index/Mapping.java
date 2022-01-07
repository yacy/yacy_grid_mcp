/**
 *  Mapping
 *  Copyright 6.3.2018 by Michael Peter Christen, @orbiterlab
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

import org.json.JSONObject;

public class Mapping {

    private String name;
    private final MappingType type;
    private final boolean indexed, stored, searchable, multiValued, omitNorms, docValues;
    private String comment;
    
    // for facets:
    private String facetname, displayname, facettype, facetmodifier;
    
    /** When true, the field must be enabled for proper YaCy operation */
    private boolean mandatory = false;
    
    public Mapping(final String name, final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment) {
        this(name, type, indexed, stored, multiValued, omitNorms, searchable, comment, false);
    }

    public Mapping(final String name, final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory) {
        this.name = name;
        this.type = type;
        this.indexed = indexed;
        this.stored = stored;
        this.multiValued = multiValued;
        this.omitNorms = omitNorms;
        this.searchable = searchable;
        this.comment = comment;
        this.mandatory = mandatory;
        this.docValues = (type == MappingType.string || type == MappingType.date || type.name().startsWith("num_"));
        // verify our naming scheme
        int p = name.indexOf('_');
        if (p > 0) {
            String ext = name.substring(p + 1);
            assert !ext.equals("i") || (type == MappingType.num_integer && !multiValued) : name;
            assert !ext.equals("l") || (type == MappingType.num_long && !multiValued) : name;
            assert !ext.equals("b") || (type == MappingType.bool && !multiValued) : name;
            assert !ext.equals("s") || (type == MappingType.string && !multiValued) : name;
            assert !ext.equals("sxt") || (type == MappingType.string && multiValued) : name;
            assert !ext.equals("dt") || (type == MappingType.date && !multiValued) : name;
            assert !ext.equals("dts") || (type == MappingType.date && multiValued) : name;
            assert !ext.equals("t") || (type == MappingType.text_general && !multiValued) : name;
            assert !ext.equals("coordinate") || (type == MappingType.coordinate && !multiValued) : name;
            assert !ext.equals("txt") || (type == MappingType.text_general && multiValued) : name;
            assert !ext.equals("val") || (type == MappingType.num_integer && multiValued) : name;
            assert !ext.equals("d") || (type == MappingType.num_double && !multiValued) : name;
        }
        assert type.appropriateName(this) : "bad configuration: " + this.name();
        this.facetname = "";
        this.displayname = "";
        this.facettype = "";
        this.facetmodifier = "";
    }
    
    public Mapping(final String name, final MappingType type, final boolean indexed, final boolean stored, final boolean multiValued, final boolean omitNorms, final boolean searchable, final String comment, final boolean mandatory,
                       final String facetname, final String displayname, final String facettype, final String facetmodifier) {
        this(name, type, indexed, stored,  multiValued, omitNorms, searchable, comment, mandatory);
        this.facetname = facetname;
        this.displayname = displayname;
        this.facettype = facettype;
        this.facetmodifier = facetmodifier;
    }
    
    /**
     * Returns the YaCy default or (if available) custom field name for Solr
     * @return SolrFieldname String
     */
    public final String name() {
        return  this.name;
    }

    public final MappingType getType() {
        return this.type;
    }

    public final boolean isIndexed() {
        return this.indexed;
    }

    public final boolean isStored() {
        return this.stored;
    }

    public final boolean isMultiValued() {
        return this.multiValued;
    }

    public final boolean isOmitNorms() {
        return this.omitNorms;
    }

    public final boolean isSearchable() {
        return this.searchable;
    }
    
    public boolean isDocValue() {
        return this.docValues;
    }

    public final String getComment() {
        return this.comment;
    }
    
    public final boolean isMandatory() {
        return this.mandatory;
    }

    public final String getFacetname() {
        return this.facetname;
    }

    public final String getDisplayname() {
        return this.displayname;
    }

    public final String getFacettype() {
        return this.facettype;
    }
    
    public final String getFacetmodifier() {
        return this.facetmodifier;
    }
    
    public final JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", getType().elasticName());
        if (getType() == MappingType.string) json.put("index", "not_analyzed");
        json.put("include_in_all", isIndexed() || isSearchable() ? "true":"false");
        return json;
    }

    public static JSONObject elasticsearchMapping(String indexName) {
        JSONObject properties = new JSONObject(true);
        for (WebMapping mapping: WebMapping.values()) {
            properties.put(mapping.name(), mapping.getMapping().toJSON());
        }
        JSONObject index = new JSONObject().put("properties", properties);
        JSONObject mappings = new JSONObject().put(indexName, index);
        JSONObject json = new JSONObject().put("mappings", mappings);
        return json;
    }

}
