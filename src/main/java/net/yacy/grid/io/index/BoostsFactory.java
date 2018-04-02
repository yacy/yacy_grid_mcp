/**
 *  BoostsFactory
 *  Copyright 05.02.2018 by Michael Peter Christen, @0rb1t3r
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

import java.util.LinkedHashMap;
import java.util.Map;

public class BoostsFactory {
	
	private final static Map<WebMapping, Float> QUERY_DEFAULT_FIELDS = new LinkedHashMap<>();
    static {
        QUERY_DEFAULT_FIELDS.put(WebMapping.url_s, 1000.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.host_organization_s, 1000.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.url_paths_sxt, 30.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.url_file_name_s, 20.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.title, 300.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.description_txt, 100.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h1_txt, 50.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h2_txt, 10.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h3_txt, 6.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.h4_txt, 3.0f);
        QUERY_DEFAULT_FIELDS.put(WebMapping.text_t, 1.0f);
    }
	
    public BoostsFactory(final Map<String, String> defaultBoosts) {
        super();
        for (Map.Entry<String, String> entry: defaultBoosts.entrySet()) {
            WebMapping webMapping = WebMapping.valueOf(entry.getKey());
            QUERY_DEFAULT_FIELDS.put(webMapping, Float.parseFloat(entry.getValue()));
        }
    }
    
    public Boosts getBoosts() {
        return new Boosts(QUERY_DEFAULT_FIELDS);
    }
    
    public class Boosts extends LinkedHashMap<WebMapping, Float> {

        private static final long serialVersionUID = -8298697781874655425L;
        
        private Boosts(Map<WebMapping, Float> defaultMapping) {
            super();
            defaultMapping.forEach((key, boost) -> this.put(key, boost));
        }
        
        public void patchWithModifier(String modifier) {
            String[] customBoosts = modifier.split(",");
            for (String customBoost: customBoosts) {
                String[] fieldBoost = customBoost.split("\\^");
                if (fieldBoost.length == 2) try {
                    this.put(WebMapping.valueOf(fieldBoost[0]), Float.parseFloat(fieldBoost[1]));
                } catch (Throwable /*many things can go wrong here*/ e) {}
            }
        }
        
    }
}
