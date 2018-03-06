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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.tools.DateParser;

public class Document extends JSONObject {

    public Document(Map<String, Object> map) {
        super(map);
    }
    
    public Document(JSONObject obj) {
        super(obj.toMap());
    }
    
    public String getString(MappingDeclaration declaration, String dflt) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.string || type == MappingType.text_en_splitting_tight || type == MappingType.text_general;
        valid = valid && !declaration.getMapping().isMultiValued();
        assert valid; if (!valid) return null;
        return this.optString(declaration.getMapping().name(), dflt);
    }
    
    public List<String> getStrings(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.string || type == MappingType.text_en_splitting_tight || type == MappingType.text_general;
        valid = valid && declaration.getMapping().isMultiValued();
        assert valid; if (!valid) return null;
        Object obj = this.opt(declaration.getMapping().name());
        valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> Objects.toString(o, null)).collect(Collectors.toList());
    }
    
    public int getInt(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_integer && !declaration.getMapping().isMultiValued();
        assert valid;
        if (!valid) return 0;
        return this.optInt(declaration.getMapping().name());
    }
    
    public List<Integer> getInts(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_integer && declaration.getMapping().isMultiValued();
        assert valid;
        if (!valid) return null;
        Object obj = this.opt(declaration.getMapping().name());
        valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> (int) o).collect(Collectors.toList());
    }

    public long getLong(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_long && !declaration.getMapping().isMultiValued();
        assert valid;
        if (!valid) return 0;
        return this.optLong(declaration.getMapping().name());
    }
    
    public List<Long> getLongs(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.num_long && declaration.getMapping().isMultiValued();
        assert valid;
        if (!valid) return null;
        Object obj = this.opt(declaration.getMapping().name());
        valid = obj instanceof JSONArray;
        assert valid; if (!valid) return null;
        return ((JSONArray) obj).toList().stream().map(o -> (long) o).collect(Collectors.toList());
    }
    
    public Date getDate(MappingDeclaration declaration) {
        MappingType type = declaration.getMapping().getType();
        boolean valid = type == MappingType.date && !declaration.getMapping().isMultiValued();
        assert valid;
        if (!valid) return null;
        if (!this.has(declaration.getMapping().name())) return null;
        String date = this.getString(declaration.getMapping().name());
        return DateParser.iso8601MillisParser(date);
    }
    
}
