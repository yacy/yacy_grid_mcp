/**
 *  MappingType
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

public enum MappingType {
    object("object", "o", "oxt"),                  // A json object
    string("keyword", "s", "sxt"),                 // The type is not analyzed, but indexed/stored verbatim
    text_general("string", "t", "txt"),            // tokenizes with StandardTokenizer, removes stop words from case-insensitive "stopwords.txt", down cases, applies synonyms.
    text_en_splitting_tight("string", null, null), // can insert dashes in the wrong place and still match
    location("geo_point", "p", null),              // lat,lon - format: specialized field for geospatial search.
    date("date", "dt", "dts"),                     // date format as in http://www.w3.org/TR/xmlschema-2/#dateTime with trailing 'Z'
    bool("boolean", "b", "bs", "boolean"),
    num_integer("integer", "i", "val", "int"),
    num_long("long", "l", "ls", "long"),
    num_float("float", "f", "fs", "float"),
    num_double("double", "d", "ds", "double"),
    coordinate("geo_point", "coordinate", "coordinatex", "tdouble");

    private String printName, singlevalExt, multivalExt, elasticName;
    private MappingType(final String elasticName, final String singlevalExt, final String multivalExt) {
            this.elasticName = elasticName;
        this.printName = this.name();
        this.singlevalExt = singlevalExt;
        this.multivalExt = multivalExt;
    }

    private MappingType(final String elasticName, final String singlevalExt, final String multivalExt, final String printName) {
            this.elasticName = elasticName;
        this.printName = printName;
        this.singlevalExt = singlevalExt;
        this.multivalExt = multivalExt;
    }

    public String printName() {
        return this.printName;
    }

    public String elasticName() {
        return this.elasticName;
    }

    public boolean appropriateName(final MappingDeclaration collectionSchema) {
        String field = collectionSchema.name();
        int p = field.indexOf('_');
        if (p < 0 || field.length() - p > 4) return true; // special names may have no type extension
        String ext = field.substring(p + 1);
        Mapping mapping = collectionSchema.getMapping();
        boolean ok = mapping.isMultiValued() ? this.multivalExt.equals(ext) : this.singlevalExt.equals(ext);
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(mapping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"s".equals(this.singlevalExt) || mapping.isMultiValued() || field.endsWith("s");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(mapping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"sxt".equals(this.singlevalExt) || !mapping.isMultiValued()  || field.endsWith("sxt");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(mapping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"t".equals(this.singlevalExt) || mapping.isMultiValued() || field.endsWith("t");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(mapping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        return ok;
    }

    public boolean appropriateName(final Mapping maping) {
        String field = maping.name();
        int p = field.indexOf('_');
        if (p < 0 || field.length() - p > 4) return true; // special names may have no type extension
        String ext = field.substring(p + 1);
        boolean ok = maping.isMultiValued() ? this.multivalExt.equals(ext) : this.singlevalExt.equals(ext);
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(maping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"s".equals(this.singlevalExt) || maping.isMultiValued() || field.endsWith("s");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(maping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"sxt".equals(this.singlevalExt) || !maping.isMultiValued()  || field.endsWith("sxt");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(maping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        ok = !"t".equals(this.singlevalExt) || maping.isMultiValued() || field.endsWith("t");
        assert ok : "SolrType = " + this.name() + ", field = " + field + ", ext = " + ext + ", multivalue = " + Boolean.valueOf(maping.isMultiValued()).toString() + ", singlevalExt = " + this.singlevalExt + ", multivalExt = " + this.multivalExt;
        if (!ok) return ok;
        return ok;
    }
}