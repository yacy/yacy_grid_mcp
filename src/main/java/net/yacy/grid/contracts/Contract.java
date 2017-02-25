/**
 *  Contract
 *  Copyright 28.01.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.contracts;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A contract is an object which should be passed around YaCy services to trigger actions
 * in the YaCy services. The json String from a contract is the message that can be pushed to
 * a grid broker. A Contract has two main parts:
 * - a table consisting of context information of a contract. All services may enrich that data.
 * - an actions list which is a pre-defined process list for all the different stages a job has to go through
 * There is also some metadata which describes the environment of such a contract, like the number of entries
 * in the data table and a possible offset. There is also the option to set a 'query' attribute which
 * makes it possible to carry a search result within a contract.
 */
public class Contract extends JSONObject {
    
    private final static String KEY_METADATA = "metadata";
    private final static String KEY_DATA     = "data";
    private final static String KEY_ACTIONS  = "actions";
    private final static String KEY_QUERY    = "query";
    private final static String KEY_OFFSET   = "offset";
    private final static String KEY_HITS     = "hits";
    private final static String KEY_COUNT    = "count";

    public Contract() {
        super(true);
        // create base data structure
        getMetadata();
        getData();
        getActions();
    }
    
    public Contract(JSONObject json) {
        this();
        if (json.has(Contract.KEY_METADATA)) this.put(KEY_METADATA, json.getJSONObject(KEY_METADATA));
        if (json.has(Contract.KEY_DATA)) this.setData(json.getJSONArray(KEY_DATA));
        if (json.has(KEY_ACTIONS)) this.put(KEY_ACTIONS, json.getJSONArray(KEY_ACTIONS));
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Contract)) return false;
        Contract t = (Contract) o;
        return this.getData().equals(t.getData());
    }
    
    public Contract setOffset(int offset) {
        getMetadata().put(KEY_OFFSET, offset);
        return this;
    }
    
    public int getOffset() {
        return getMetadata().has(KEY_OFFSET) ? getMetadata().getInt(KEY_OFFSET) : 0;
    }
    
    public int getCount() {
        return getData().length();
    }

    public boolean isFailed() {
        return getData().length() == 0;
    }

    public boolean hasEmptyDirection(String key) {
        List<String> directions = this.getDirections(key);
        return directions.size() == 0 || directions.get(0).length() == 0;
    }
    
    public Contract setHits(int hits) {
        getMetadata().put(KEY_HITS, hits);
        return this;
    }
    
    public int getHits() {
        return getMetadata().has(KEY_HITS) ? getMetadata().getInt(KEY_HITS) : 0;
    }

    public Contract setProcess(String processName) {
        getMetadata().put("process", processName);
        return this;
    }

    public Contract setQuery(String query) {
        getMetadata().put(KEY_QUERY, query);
        return this;
    }
    
    public String getQuery() {
        return getMetadata().has(KEY_QUERY) ? getMetadata().getString(KEY_QUERY) : "";
    }
    
    private JSONObject getMetadata() {
        JSONObject md;
        if (this.has(KEY_METADATA)) md = this.getJSONObject(KEY_METADATA); else {
            md = new JSONObject();
            this.put(KEY_METADATA, md);
        }
        if (!md.has(KEY_COUNT)) md.put(KEY_COUNT, getData().length());
        return md;
    }
    
    public Contract setData(JSONArray table) {
        this.put(KEY_DATA, table);
        JSONObject md = getMetadata();
        md.put(KEY_COUNT, getData().length());
        return this;
    }
    
    public JSONArray getData() {
        if (this.has(KEY_DATA)) return this.getJSONArray(KEY_DATA);
        JSONArray a = new JSONArray();
        this.put(KEY_DATA, a);
        return a;
    }
    
    public Contract mergeData(JSONArray table1) {
        JSONArray table0 = this.getData();
        int t0c = 0;
        for (int i = 0; i < table1.length(); i++) {
            JSONObject j1i = table1.getJSONObject(i);
            while (t0c < table0.length() && anyObjectKeySame(j1i, table0.getJSONObject(t0c))) {t0c++;}
            if (t0c >= table0.length()) table0.put(new JSONObject(true));
            table0.getJSONObject(t0c).putAll(table1.getJSONObject(i));
        }
        setData(table0);
        return this;
    }
    
    private final static boolean anyObjectKeySame(final JSONObject a, final JSONObject b) {
        for (String k: a.keySet()) if (b.has(k)) return true;
        return false;
    }
    
    public Contract addDirection(String featureName, String direction) {
        JSONArray data = getData();
        
        // find first occurrence of key in rows
        int rowc = 0; boolean found = false;
        while (rowc < data.length()) {
            JSONObject row = data.getJSONObject(rowc);
            if (row.has(featureName)) found = true;
            if (found) break;
            rowc++;
        }
        if (found) {
            // insert feature in front of row
            if (rowc == 0) {
                // insert a row and shift everything up
                JSONArray newData = new JSONArray();
                JSONObject row = new JSONObject();
                row.put(featureName, direction);
                newData.put(row);
                for (Object o: data) newData.put(o);
                this.setData(newData);
            } else {
                JSONObject row = data.getJSONObject(rowc - 1);
                row.put(featureName, direction);
            }
        } else {
            // insert into first line
            if (data.length() == 0) {
                JSONObject row = new JSONObject();
                row.put(featureName, direction);
                data.put(row);
            } else {
                JSONObject row = data.getJSONObject(0);
                row.put(featureName, direction);
            }
        }
        return this;
    }
    
    public List<String> getDirections(String featureName) {
        List<String> list = new ArrayList<>();
        JSONArray table = this.getData();
        if (table != null && table.length() > 0) {
            JSONObject row = table.getJSONObject(0);
            for (String key: row.keySet()) {
                if (key.equals(featureName)) list.add(row.get(key).toString());
            }
        }
        return list;
    }
    
    public Contract addActions(List<Action> actions) {
        JSONArray a = getActionsJSON();
        actions.forEach(action -> a.put(action.toJSONClone()));
        return this;
    }
    
    public Contract addAction(Action action) {
        JSONArray a = getActionsJSON();
        a.put(action.toJSONClone());
        return this;
    }
    
    public List<Action> getActions() {
        List<Action> actions = new ArrayList<>();
        getActionsJSON().forEach(action -> actions.add(new Action((JSONObject) action)));
        return actions;
    }
    
    public Action getNextAction() {
        JSONArray a = getActionsJSON();
        if (a.length() == 0) return null;
        return new Action(a.getJSONObject(0));
    }
    
    public Action removeNextAction() {
        JSONArray a = getActionsJSON();
        if (a.length() == 0) return null;
        return new Action((JSONObject) this.getJSONArray(KEY_ACTIONS).remove(0));
    }
    
    private JSONArray getActionsJSON() {
        JSONArray actions;
        if (!this.has(KEY_ACTIONS)) {
            actions = new JSONArray();
            this.put(KEY_ACTIONS, actions);
        } else {
            actions = this.getJSONArray(KEY_ACTIONS);
        }
        return actions;
    }
    
    public JSONObject toJSON() {
        return this;
    }
    
    public static void main(String[] args) {
        Contract contract = new Contract();
        contract.setQuery("*");
        contract.addDirection("url", "http://fsfe.de");
        Action action = new Action()
                .setQuery(new JSONObject().put("q", "abc"))
                .setServletName("yacysearch");
        contract.addAction(action);
        System.out.println(contract.toString(2));
    }
    
}
