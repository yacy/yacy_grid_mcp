/**
 *  JSONLD
 *  Copyright 09.07.2018 by Michael Peter Christen, @0rb1t3r
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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * https://json-ld.org/spec/latest/json-ld/#json-ld-grammar
 * 
 *
 */
public class JsonLD {

    public static final String CONTEXT = "@context";
    public static final String TYPE    = "@type";    // distinguish node type and value type
    public static final String ID      = "@id";      // the id at top node level denotes the subject of the graph
    public static final String GRAPH   = "@graph";   // if a json-ld contains several nodes on the same level, they are bundled in a graph array

    private List<JsonLDNode> graph;
    private Object context;
    
    public JsonLD() {
        this.graph = new ArrayList<>();
        this.context = null;
    }
    
    public JsonLDNode getCurrentNode() {
        if (this.graph.size() > 0) {
            JsonLDNode element = (JsonLDNode) (this.graph.get(graph.size() - 1));
            return element;
        } else {
            JsonLDNode element = new JsonLDNode(this);
            graph.add(element);
            return element;
        }
    }

    public JsonLDNode addNode(String type) {
        JsonLDNode element = getCurrentNode();
        if (element.hasType()) {
            element = new JsonLDNode(this);
            this.graph.add(element);
        }
        element.setType(type);
        return element;
    }
    
    public boolean hasContext() {
        return this.context != null;
    }
    
    public String getContext() {
        if (this.context == null) return null;
        if (this.context instanceof String) return (String) this.context;
        if (this.context instanceof JSONObject) {
            JSONObject o = (JSONObject) this.context;
            return o.getString(o.keys().next());
        }
        return null;
    }
    
    /**
     * add a context
     * for several contexts, see example 28 and 29 in https://www.w3.org/TR/2014/REC-json-ld-20140116/#advanced-context-usage
     * @param name the name of the context or NULL if 
     * @param context
     * @return this
     */
    public JsonLD addContext(String name, String context) {
        if (name == null) {
            // set a default vocabulary; overwrite possible existing
            this.context = context;
        } else {
            if (this.context == null) {
                this.context = new JSONObject(true).put(name, context);
            } else {
                if (this.context instanceof String) {
                    String s = (String) this.context;
                    JSONObject o = new JSONObject();
                    JsonLDNode node = this.getCurrentNode();
                    String tempName = node.renameDefaultContextProperties();
                    o.put(tempName, s);
                    o.put(name, context);
                    this.context = o;
                }
                if (this.context instanceof JSONObject) {
                    JSONObject o = (JSONObject) this.context;
                    o.put(name, context);
                }
            }
        }
        return this;
    }
    
    public boolean hasContextName(String name) {
        if (this.context != null && this.context instanceof JSONObject) {
            return ((JSONObject) this.context).has(name);
        }
        return false;
    }
    
    public JSONObject getJSON() {
        JSONObject object = new JSONObject(true);
        if (this.context != null) object.put(CONTEXT, this.context);
        JSONArray graph = new JSONArray();
        for (JsonLDNode element: this.graph) {
            graph.put(element.getJSON());
        }
        object.put(GRAPH, graph);
        return object;
    }
    
    public String toString() {
        return this.getJSON().toString(2);
    }
    
    public static void main(String[] args) {
        // user the JSON-LD playground https://json-ld.org/playground/
        // to verify the objects
        JsonLD ld = new JsonLD().addContext(null, "http://schema.org");
        ld
                .addNode("Event")
                .setSubject("http://an.event.home.page.ninja/tomorrow.html")
                .setPredicate("name", "Typhoon with Radiation City")
                .setPredicate("startDate", "2013-09-14T21:30")
                .setPredicate("location",
                        new JsonLDNode(ld)
                            .setType("Place")
                            .setPredicate("name", "The Hi-Dive")
                            .setPredicate("address",
                                    new JsonLDNode(ld)
                                        .setType("PostalAddress")
                                        .setPredicate("addressLocality", "Denver")
                                        .setPredicate("addressRegion", "CO")
                                        .setPredicate("postalCode", "80209")
                                        .setPredicate("streetAddress", "7 S. Broadway")
                            )
                )
                .setPredicate("offers", 
                        new JsonLDNode(ld)
                            .setType("Offer")
                            .setPredicate("price", "13.00")
                            .setPredicate("priceCurrency", "USD")
                            .setPredicate("postalCode", "80209")
                            .setPredicate("url", "http://www.ticketfly.com/purchase/309433")
                );
        System.out.println(ld.toString());
    }
}
