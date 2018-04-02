/**
 *  MapUtil
 *  Copyright 02.10.2016 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MapUtil {

    /**
     * sort a Map<Key, Value> on the values. This algorithm is taken from
     * http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
     * provided by tunaki http://stackoverflow.com/users/1743880/tunaki
     * It was extended with a 'asc' attribute to select ascending (asc = true)
     * or descending (desc) order.
     * @param map
     * @param asc
     * @return the map, sorted by the value in ascending or descending order (according to asc)
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map, final boolean asc) {
        
        // put the given map into a list with Map.Entry objects
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        // declare a sort method for such kind of lists and sort it.
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return asc ? (o1.getValue()).compareTo(o2.getValue()) : (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // create a result map and put the ordered list inside.
        // The map is a linked map which has a defined object order
        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * read a configuration file and overload it with custom configurations
     * @param conf_dir the path where the default configuration file is stored
     * @param user_dir the path where the user writes a custom configuration
     * @param confFileName the name of the configuration file
     * @return a map from the properties in the configuration
     * @throws IOException
     */
    public static Map<String, String> readConfig(File conf_dir, File user_dir, String confFileName) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(new File(conf_dir, confFileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<Object, Object> entry: prop.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
        user_dir.mkdirs();
        File customized_config = new File(user_dir, confFileName);
        if (!customized_config.exists()) try {
            BufferedWriter w = new BufferedWriter(new FileWriter(customized_config));
            w.write("# This file can be used to customize the configuration\n");
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Properties customized_config_props = new Properties();
        try {
            customized_config_props.load(new FileInputStream(customized_config));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Map.Entry<Object, Object> entry: customized_config_props.entrySet()) config.put((String) entry.getKey(), (String) entry.getValue());
        return config;
    }
}
