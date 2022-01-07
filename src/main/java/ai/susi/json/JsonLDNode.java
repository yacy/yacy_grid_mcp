/**
 *  JSONLDNode
 *  Copyright 04.06.2018 by Michael Peter Christen, @orbiterlab
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

package ai.susi.json;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * https://json-ld.org/spec/latest/json-ld/#node-objects
 *
 * A node object represents zero or more properties of a node in the graph serialized by the JSON-LD
 *  document. A JSON object is a node object if it exists outside of a JSON-LD context and:
 *  - it is not the top-most JSON object in the JSON-LD document consisting of no other members
 *    than @graph and @context,
 *  - it does not contain the @value, @list, or @set keywords, and
 *  - it is not a graph object.
 *
 *  The properties of a node in a graph may be spread among different node objects within a document.
 *  When that happens, the keys of the different node objects need to be merged to create the
 *  properties of the resulting node.
 *
 *  A node object MUST be a JSON object. All keys which are not IRIs, compact IRIs, terms valid in
 *  the active context, or one of the following keywords (or alias of such a keyword) MUST be ignored
 *  when processed:
 *
 *  @context,
 *  @id,
 *  @graph,
 *  @nest,
 *  @type,
 *  @reverse, or
 *  @index
 */
public class JsonLDNode {

    private JSONObject object;
    private JsonLD parent;

    private final static Random random = new Random(System.currentTimeMillis()); // for temporary name generation

    private static String generateTemporaryName() {
        String r = Integer.toHexString(random.nextInt());
        while (r.length() < 4) r = "0" + r;
        if (r.length() > 0) r = r.substring(0, 4);
        return r.toUpperCase();
    }

    public String renameDefaultContextProperties() {
        String tmpn = generateTemporaryName();
        Set<String> keys = new HashSet<String>(this.getPredicates()); // we use a clone to remove dependency on original map
        for (String key: keys) {
            if (key.charAt(0) == '@') continue;
            if (key.contains(":")) continue;
            Object o = this.removePredicate(key);
            this.setPredicate(tmpn + ":" + key, o);
        }
        return tmpn;
    }

    public JsonLDNode(JsonLD parent) {
        this.object = new JSONObject(true);
        this.parent = parent;
    }

    public boolean hasSubject() {
        return this.object.has(JsonLD.ID);
    }

    public String getSubject() {
        return this.object.optString(JsonLD.ID);
    }

    public JsonLDNode setSubject(String subject) {
        this.object.put(JsonLD.ID, subject);
        return this;
    }

    public boolean hasType() {
        return this.object.has(JsonLD.TYPE);
    }

    public String getType() {
        return this.object.optString(JsonLD.TYPE);
    }

    public JsonLDNode setType(String type) {
        this.object.put(JsonLD.TYPE, type);
        return this;
    }

    public String getVocabulary(String name) {
        return this.parent.getContext() + "/" + getType();
    }

    public JsonLDNode setPredicate(String key, Object value) {
        assert value instanceof String || value instanceof JSONObject || value instanceof JSONArray || value instanceof JsonLDNode: "bad value type: " + value.getClass();
        if (value instanceof JsonLDNode) {
            this.object.put(key, ((JsonLDNode) value).getJSON());
        } else {
            this.object.put(key, value);
        }
        int p = key.indexOf(':');
        if (p >= 0) {
            String voc = key.substring(0, p);
            if ("og".equals(voc) && !this.parent.hasContextName("og")) this.parent.addContext("og", "http://ogp.me/ns#");
            if ("og".equals(voc) && !this.parent.hasContextName("fb")) this.parent.addContext("fb", "http://ogp.me/ns/fb#");
        }
        return this;
    }

    /**
     * Set new or add additional predicate with given key/name. Existing values will be converted to arrays of values.
     *
     * @param key
     * @param value
     * @return
     */
    public JsonLDNode addPredicate(String key, Object value) {
        if (!hasPredicate(key)) {
            return setPredicate(key, value);
        }

        Object predicateValue = getPredicateValue(key);
        if (predicateValue instanceof JSONObject && ((JSONObject) predicateValue).length() < 1) {
            return setPredicate(key, value);
        } else if (predicateValue instanceof JSONObject && value instanceof JSONObject) {
            JSONObject existingValue = (JSONObject) predicateValue;
            for (String subKey: ((JSONObject) value).keySet()) {
                if (existingValue.has(subKey) && existingValue.get(subKey) instanceof JSONArray) {
                    JSONArray valueArray = (JSONArray) existingValue.get(subKey);
                    valueArray.put(((JSONObject) value).get(subKey));
                    ((JSONObject) predicateValue).put(subKey, valueArray);
                } else if (existingValue.has(subKey)) {
                    JSONArray valueArray = new JSONArray();
                    valueArray.put(((JSONObject) value).get(subKey));
                    valueArray.put(((JSONObject) predicateValue).get(subKey));
                    ((JSONObject) predicateValue).put(subKey, valueArray);
                } else {
                    ((JSONObject) predicateValue).put(subKey, ((JSONObject) value).get(subKey));
                }
            }

            return setPredicate(key, predicateValue);
        }

        if (predicateValue instanceof List) {
            ((JSONArray) predicateValue).put(value);
        } else {
            JSONArray newPredicateValue = new JSONArray();
            newPredicateValue.put(predicateValue);
            newPredicateValue.put(value);
            setPredicate(key, newPredicateValue);
        }

        return this;
    }

    public Object removePredicate(String key) {
        return this.object.remove(key);
    }

    public String getPredicateName(String key) {
        return this.parent.getContext() + "#" + key;
    }

    public Object getPredicateValue(String key) {
        return this.object.get(key);
    }

    public List<String> getPredicates() {
        ArrayList<String> predicates = new ArrayList<>();
        this.object.keySet().forEach(key -> {
            if (key.isEmpty() || key.charAt(0) != '@') {
                predicates.add(key);
            }
        });
        return predicates;
    }

    public boolean hasPredicate(String key) {
        return this.object.has(key);
    }

    public JSONObject getJSON() {
        return this.object;
    }

    public String toString() {
        return this.object.toString(2);
    }

}
