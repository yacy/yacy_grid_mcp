/**
 *  Document
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.tools.DateParser;

public class Document extends JSONObject {

    public Document() {
        super();
    }
    
    public Document(Map<String, Object> map) {
        super(map);
    }
    
    public Document(JSONObject obj) {
        super(obj.toMap());
    }
    
    public Document putObject(MappingDeclaration declaration, JSONObject o) {
        if (!isString(declaration)) return this;
        this.put(declaration.getMapping().name(), o);
        return this;
    }
    
    public JSONObject getObject(MappingDeclaration declaration) {
        if (!isString(declaration)) return null;
        return this.optJSONObject(declaration.getMapping().name());
    }
    
    public Document putString(MappingDeclaration declaration, String s) {
        if (!isString(declaration)) return this;
        this.put(declaration.getMapping().name(), s);
        return this;
    }
    
    public String getString(MappingDeclaration declaration, String dflt) {
        if (!isString(declaration)) return null;
        return this.optString(declaration.getMapping().name(), dflt);
    }
    
    private boolean isString(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.string || type == MappingType.text_en_splitting_tight || type == MappingType.text_general;
        valid = valid && !declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }

    public Document putStrings(MappingDeclaration declaration, Collection<String> list) {
        if (!isStrings(declaration)) return this;
        this.put(CrawlerMapping.collection_sxt.getMapping().name(), list);
        return this;
    }

    public List<String> getStrings(MappingDeclaration declaration) {
        if (!isStrings(declaration)) return null;
        Object obj = this.opt(declaration.getMapping().name());
        boolean valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> Objects.toString(o, null)).collect(Collectors.toList());
    }

    private boolean isStrings(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.string || type == MappingType.text_en_splitting_tight || type == MappingType.text_general;
        valid = valid && declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }
    
    public Document putInt(MappingDeclaration declaration, int i) {
        if (!isInt(declaration)) return this;
        this.put(declaration.getMapping().name(), i);
        return this;
    }
    
    public int getInt(MappingDeclaration declaration) {
        if (!isInt(declaration)) return 0;
        return this.optInt(declaration.getMapping().name());
    }
    
    private boolean isInt(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_integer && !declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }
    
    public Document putInts(MappingDeclaration declaration, Collection<Integer> ints) {
        if (!isInts(declaration)) return this;
        this.put(declaration.getMapping().name(), ints);
        return this;
    }
    
    public List<Integer> getInts(MappingDeclaration declaration) {
        if (!isInts(declaration)) return null;
        Object obj = this.opt(declaration.getMapping().name());
        boolean valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> (int) o).collect(Collectors.toList());
    }
    
    private boolean isInts(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_integer && declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }

    public Document putLong(MappingDeclaration declaration, long l) {
        if (!isLong(declaration)) return this;
        this.put(declaration.getMapping().name(), l);
        return this;
    }
    
    public long getLong(MappingDeclaration declaration) {
        if (!isLong(declaration)) return 0;
        return this.optLong(declaration.getMapping().name());
    }
    
    private boolean isLong(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_long && !declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }
    
    public Document putlongs(MappingDeclaration declaration, Collection<Long> longs) {
        if (!isInts(declaration)) return this;
        this.put(declaration.getMapping().name(), longs);
        return this;
    }
    
    public List<Long> getLongs(MappingDeclaration declaration) {
        if (!isLongs(declaration)) return null;
        Object obj = this.opt(declaration.getMapping().name());
        boolean valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> (long) o).collect(Collectors.toList());
    }

    private boolean isLongs(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_long && declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }

    public Document putDate(MappingDeclaration declaration, Date date) {
        if (!isDate(declaration)) return this;
        this.put(declaration.getMapping().name(), DateParser.iso8601MillisFormat.format(date));
        return this;
    }
    
    public Date getDate(MappingDeclaration declaration) {
       if (!isDate(declaration)) return null;
        if (!this.has(declaration.getMapping().name())) return null;
        String date = this.getString(declaration.getMapping().name());
        return DateParser.iso8601MillisParser(date);
    }
    
    private boolean isDate(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.date && !declaration.getMapping().isMultiValued();
        assert valid;
        return valid;
    }
}
