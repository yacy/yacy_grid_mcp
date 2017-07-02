/**
 *  SusiAction
 *  Copyright 29.06.2016 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.mind;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import net.yacy.grid.mcp.Data;

/**
 * An action is an application on the information deduced during inferences on mind states
 * as they are represented in an argument. If we want to produce respond sentences or if
 * we want to visualize deduces data as a graph or in a picture, thats an action.
 */
public class SusiAction {

    public static enum RenderType {answer, table, piechart, rss, self, websearch, anchor, map, loader, parser, indexer;}
    public static enum SelectionType {random, roundrobin;}
    public static enum DialogType {
        answer,    // a sentence which may end a conversation
        question,  // a sentence which may cause that the user answers with a fact
        reply;     // a response of an answers of the user from a question aked by sudy
        public int getSubscore() {
            return this.ordinal();
        }
    }
    
    private JSONObject json;

    /**
     * initialize an action using a json description.
     * @param json
     */
    public SusiAction(JSONObject json) {
        this.json = json;
    }

    public static JSONObject answerAction(String[] answers) {
        JSONObject json = new JSONObject();
        JSONArray phrases = new JSONArray();
        json.put("type", RenderType.answer.name());
        json.put("select", SelectionType.random.name());
        json.put("phrases", phrases);
        for (String answer: answers) phrases.put(answer.trim());
        return json;
    }
    
    /**
     * Get the render type. That can be used to filter specific information from the action JSON object
     * to create specific activities like 'saying' a sentence, painting a graph and so on.
     * @return the action type
     */
    public RenderType getRenderType() {
        if (renderTypeCache == null) 
            renderTypeCache = this.json.has("type") ? RenderType.valueOf(this.json.getString("type")) : null;
        return renderTypeCache;
    }
    private RenderType renderTypeCache = null;

    public DialogType getDialogType() {
        if (this.getRenderType() != RenderType.answer) return DialogType.answer;
        return getDialogType(getPhrases());
    }
    
    public static DialogType getDialogType(Collection<String> phrases) {
        DialogType type = DialogType.reply;
        for (String phrase: phrases) {
            DialogType t = getDialogType(phrase);
            if (t.getSubscore() < type.getSubscore()) type = t;
        }
        return type;
    }
    
    public static DialogType getDialogType(String phrase) {
        if (phrase.indexOf('?') > 3) { // the question mark must not be at the beginning
            return phrase.indexOf(". ") >= 0 ? DialogType.reply : DialogType.question;
        }
        return DialogType.answer;
    }
    
    /**
     * If the action involves the reproduction of phrases (=strings) then they can be retrieved here
     * @return the action phrases
     */
    public ArrayList<String> getPhrases() {
        if (phrasesCache == null) {
            ArrayList<String> a = new ArrayList<>();
            // actions may have either a single expression "expression" or a phrases object with 
            if (this.json.has("expression")) {
                a.add(this.json.getString("expression"));
            } else if (this.json.has("phrases")) {
                this.json.getJSONArray("phrases").forEach(p -> a.add((String) p));
            } else return null;
            phrasesCache = a;
        }
        return phrasesCache;
    }
    private ArrayList<String> phrasesCache = null;
    
    /**
     * if the action contains more String attributes where these strings are named, they can be retrieved here
     * @param attr the name of the string attribute
     * @return the action string
     */
    public String getStringAttr(String attr) {
        return this.json.has(attr) ? this.json.getString(attr) : "";
    }
    public boolean getBooleanAttr(String attr) {
        return this.json.has(attr) ? this.json.getBoolean(attr) : false;
    }
    public long getLongAttr(String attr) {
        return this.json.has(attr) ? this.json.getLong(attr) : -1;
    }
    public double getDoubleAttr(String attr) {
        return this.json.has(attr) ? this.json.getDouble(attr) : Double.NaN;
    }
    public JSONArray getArrayAttr(String attr) {
        return this.json.has(attr) ? this.json.getJSONArray(attr) : new JSONArray();
    }
    
    /**
     * If the action contains integer attributes, they can be retrieved here
     * @param attr the name of the integer attribute
     * @return the integer number
     */
    public int getIntAttr(String attr) {
        return this.json.has(attr) ? this.json.getInt(attr) : 0;
    }

    /**
     * An action is backed with a JSON data structure. That can be retrieved here.
     * @return the json structure of the action
     */
    public JSONObject toJSONClone() {
        JSONObject j = new JSONObject(true);
        this.json.keySet().forEach(key -> j.put(key, this.json.get(key))); // make a clone
        if (j.has("expression")) {
            j.remove("phrases");
            j.remove("select");
        }
        return j;
    }
    
    /**
     * actions may have actions embedded, which act as follow-up actions.
     * @return the action inside that action, not a clone!
     */
    public JSONArray getEmbeddedActions() {
        return this.json.getJSONArray("actions");
    }
    
    // attach a binary asset to the action
    public SusiAction setBinaryAsset(String name, byte[] b) {
        JSONObject asset;
        if (this.json.has("assets")) asset = this.json.getJSONObject("asset"); else {
            asset = new JSONObject();
            this.json.put("assets", asset);
        }
        JSONObject base64;
        if (asset.has("base64")) base64 = asset.getJSONObject("base64"); else {
            base64 = new JSONObject();
            asset.put("base64", asset);
        }
        final String bAsBase64 = Base64.getEncoder().encodeToString(b);
        base64.put(name, bAsBase64);
        return this;
    }
    
    // read a binary asset from the action
    public byte[] getBinaryAsset(String name) {
        if (!this.json.has("assets")) return null;
        JSONObject assets = this.json.getJSONObject("assets");
        if (!assets.has("base64")) return null;
        String bAsBase64 = assets.getString(name);
        return Base64.getDecoder().decode(bAsBase64);
    }
    
    /**
     * toString
     * @return return the json representation of the object as a string
     */
    public String toString() {
        return toJSONClone().toString();
    }
}
