/**
 *  JSONLDNode
 *  Copyright 04.06.2018 by Michael Peter Christen, @0rb1t3r
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
 *
 *
 */
public class JsonLDNode extends JSONObject {

    public static final String CONTEXT = "@context";
    public static final String TYPE    = "@type";    // distinguish node type and value type
    public static final String ID      = "@id";      // the id at top node level denotes the subject of the graph
    public static final String GRAPH   = "@graph";   // if a json-ld contains several nodes on the same level, they are bundled in a graph array
    
    private final static Random random = new Random(System.currentTimeMillis()); // for temporary name generation
    
    private String tmpName = "";
    private int currentDepth = 0;

    public JsonLDNode setTemporaryName() {
        tmpName = generateTemporaryName();
        return this;
    }
    
    private static String generateTemporaryName() {
        String r = Integer.toHexString(random.nextInt());
        while (r.length() < 4) r = "0" + r;
        if (r.length() > 0) r = r.substring(0, 4);
        return r.toUpperCase();
    }
    
    public String getTmpName() {
        if (this.tmpName == null) generateTemporaryName();
        return this.tmpName;
    }
    
    public JsonLDNode() {
        super(true);
        this.currentDepth = 0;
    }
    
    public JsonLDNode setDepth(int level) {
        this.currentDepth = level;
        return this;
    }
    
    public int getDepth() {
        return this.currentDepth;
    }
    
    public boolean hasSubject() {
        return this.has(ID);
    }
    
    public String getSubject() {
        return this.optString(ID);
    }
    
    public JsonLDNode setSubject(String subject) {
        this.put(ID, subject);
        return this;
    }
    
    public boolean hasContext() {
        return this.has(CONTEXT);
    }
    
    public String getContext() {
        return this.optString(CONTEXT);
    }

    private String renameDefaultContextProperties() {
        String tmpn = generateTemporaryName();
        Set<String> keys = new HashSet<String>(this.keySet()); // we use a clone to remove dependency on original map
        for (String key: keys) {
            if (key.charAt(0) == '@') continue;
            if (key.contains(":")) continue;
            Object o = this.remove(key);
            this.put(tmpn + ":" + key, o);
        }
        return tmpn;
    }
    
    /**
     * add a context
     * for several contexts, see example 28 and 29 in https://www.w3.org/TR/2014/REC-json-ld-20140116/#advanced-context-usage
     * @param name the name of the context or NULL if 
     * @param context
     * @return this
     */
    public JsonLDNode addContext(String name, String context) {
        if (name == null) {
            // set a default vocabulary
            if (this.has(CONTEXT)) {
                Object c = this.get(CONTEXT);
                if (c instanceof String) {
                    // !his already has a default context.
                    // We can set a new default context by making the previously a named context.
                    // This transformation is made here.
                    // THIS WORKS ONLY IF THE CONTEXT IS DEFINED AFTER THE PROPERTIES ARE SET!
                    String tmpn = renameDefaultContextProperties();
                    // rewrite old default content into new one
                    JSONArray a = new JSONArray();
                    a.put(context);
                    a.put(new JSONObject(true).put(tmpn, (String) c));
                    this.put(CONTEXT, a);
                } else
                if (c instanceof JSONArray) {
                    JSONArray a = (JSONArray) c;
                    assert a.length() == 2;
                    assert a.get(0) instanceof String;
                    assert a.get(1) instanceof JSONObject;
                    // here again we make the old default context into a new one
                    String tmpn = renameDefaultContextProperties();
                    JSONObject o = (JSONObject) a.get(1);
                    o.put(tmpn, a.get(0));
                    a.put(0, context);
                } else
                if (c instanceof JSONObject) {
                    JSONArray a = new JSONArray();
                    a.put(context);
                    a.put(c);
                }
            } else {
                this.put(CONTEXT, context);
            }
        } else {
            if (this.has(CONTEXT)) {
                Object c = this.get(CONTEXT);
                if (c instanceof String) {
                    JSONArray a = new JSONArray();
                    a.put(c);
                    a.put(new JSONObject(true).put(name, context));
                    this.put(CONTEXT, a);
                } else
                if (c instanceof JSONArray) {
                    JSONArray a = (JSONArray) c;
                    for (Object obj: a) {
                        if (obj instanceof JSONObject) {
                            ((JSONObject) obj).put(name, context);
                            break;
                        }
                    }
                } else
                if (c instanceof JSONObject) {
                    ((JSONObject) c).put(name, context);
                }
            } else {
                this.put(CONTEXT, new JSONObject(true).put(name, context));
            }
        }
        return this;
    }
    
    public boolean hasContextName(String name) {
        if (this.has(CONTEXT)) {
            Object c = this.get(CONTEXT);
            if (c instanceof JSONArray) {
                JSONArray a = (JSONArray) c;
                JSONObject o = (JSONObject) a.get(1);
                return o.has(name);
            }
            if (c instanceof JSONObject) {
                return ((JSONObject) c).has(name);
            }
        }
        return false;
    }

    public boolean hasType() {
        return this.has(TYPE);
    }
    
    public String getType() {
        return this.optString(TYPE);
    }
    
    public JsonLDNode addType(String type) {
        if (this.has(TYPE)) {
            Object t = this.get(TYPE);
            if (t instanceof String) {
                JSONArray a = new JSONArray();
                a.put(t);
                a.put(type);
                this.put(TYPE, a);
            } else
            if (t instanceof JSONArray) {
                ((JSONArray) t).put(type);
            } else throw new RuntimeException("bad type for @type object");
        } else {
            this.put(TYPE, type);
        }
        return this;
    }
    
    public String getVocabulary(String name) {
        return getContext() + "/" + getType();
    }
    
    public JsonLDNode addNodeAtGraph(JsonLDNode node) {
        if (this.has(GRAPH)) {
            JSONArray a = this.getJSONArray(GRAPH);
            a.put(node);
        } else {
            JSONArray a = new JSONArray();
            a.put(node);
            this.put(GRAPH, a);
        }
        return this;
    }
    
    public JsonLDNode setPredicate(String key, Object value) {
        assert value instanceof String || value instanceof JsonLDNode;
        this.put(key, value);
        int p = key.indexOf(':');
        if (p >= 0) {
            String voc = key.substring(0, p);
            if ("og".equals(voc) && !this.hasContextName("og")) this.addContext("og", "http://ogp.me/ns#");
            if ("og".equals(voc) && !this.hasContextName("fb")) this.addContext("fb", "http://ogp.me/ns/fb#");
            
        }
        return this;
    }

    public String getPredicateName(String key) {
        return this.getContext() + "#" + key;
    }
    
    public Object getPredicateValue(String key) {
        return this.get(key);
    }

    public List<String> getPredicates() {
        ArrayList<String> predicates = new ArrayList<>();
        this.keySet().forEach(key -> {if (key.charAt(0) != '@') predicates.add(key);});
        return predicates;
    }
    
    public String toRDFTriple() {
        return toRDFTriple(this.getSubject(), this.getContext());
    }
    private String toRDFTriple(String subject, String vocabulary) {
        StringBuilder sb = new StringBuilder();
        for (String predicate: getPredicates()) {
            Object value = this.getPredicateValue(predicate);
            if (value instanceof String) sb.append("<" + subject + "> <" + vocabulary + "#" + predicate + "> <" + ((String) value) + ">\n");
            if (value instanceof JsonLDNode) sb.append(((JsonLDNode) value).toRDFTriple(subject, vocabulary));
        }
        return sb.toString();
    }
    
    /**
     * create a clone of this object with a different annotation which makes
     * the JSONObject more beautiful
     * @return the semantically same json-ld with a better presentation
     */
    public JSONObject simplify() {
        // first care about the CONTEXT object
        Object c = this.opt(CONTEXT);
        if (c != null && c instanceof JSONArray) {
            JSONArray a = (JSONArray) c;
            if (a.length() == 2 && a.get(0) instanceof String && a.get(1) instanceof JSONObject) {
                String tmpn = renameDefaultContextProperties();
                JSONObject o = (JSONObject) a.get(1);
                o.put(tmpn, a.get(0));
                this.put(CONTEXT, o);
            }
        }
        JSONObject simple = new JSONObject(true);
        if ((c = this.opt(CONTEXT)) != null) simple.put(CONTEXT, c);
        
        // then copy all objects with leading '@'
        this.keySet().forEach(key -> {if (key.charAt(0) == '@' && !CONTEXT.equals(key)) simple.put(key, this.get(key));});
        
        // finally copy all remaining properties
        this.keySet().forEach(key -> {if (key.charAt(0) != '@' && !CONTEXT.equals(key)) simple.put(key, this.get(key));});
        
        return simple;
    }
    
    public String toString() {
        return super.toString(2);
    }
    
    /*
{
  "@context": "http://schema.org",
  "@type": "Event",
  "location": {
    "@type": "Place",
    "address": {
      "@type": "PostalAddress",
      "addressLocality": "Denver",
      "addressRegion": "CO",
      "postalCode": "80209",
      "streetAddress": "7 S. Broadway"
    },
    "name": "The Hi-Dive"
  },
  "name": "Typhoon with Radiation City",
  "offers": {
    "@type": "Offer",
    "price": "13.00",
    "priceCurrency": "USD",
    "url": "http://www.ticketfly.com/purchase/309433"
  },
  "startDate": "2013-09-14T21:30"
}
     */
    
    public static void main(String[] args) {
        // user the JSON-LD playground https://json-ld.org/playground/
        // to verify the objects
        JsonLDNode event = new JsonLDNode()
                .addContext(null, "http://schema.org")
                .addType("Event")
                .setSubject("http://an.event.home.page.ninja/tomorrow.html")
                .setPredicate("name", "Typhoon with Radiation City")
                .setPredicate("startDate", "2013-09-14T21:30")
                .setPredicate("location",
                        new JsonLDNode()
                            .addType("Place")
                            .setPredicate("name", "The Hi-Dive")
                            .setPredicate("address",
                                    new JsonLDNode()
                                        .addType("PostalAddress")
                                        .setPredicate("addressLocality", "Denver")
                                        .setPredicate("addressRegion", "CO")
                                        .setPredicate("postalCode", "80209")
                                        .setPredicate("streetAddress", "7 S. Broadway")
                            )
                )
                .setPredicate("offers", 
                        new JsonLDNode()
                            .addType("Offer")
                            .setPredicate("price", "13.00")
                            .setPredicate("priceCurrency", "USD")
                            .setPredicate("postalCode", "80209")
                            .setPredicate("url", "http://www.ticketfly.com/purchase/309433")
                );
        System.out.println(event.toString(2));
        System.out.println(event.toRDFTriple());
    }
}
