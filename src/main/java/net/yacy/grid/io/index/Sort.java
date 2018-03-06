/**
 *  Sort
 *  Copyright 10.02.2018 by Michael Peter Christen, @0rb1t3r
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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class Sort {
    
    public final static Sort DEFAULT = new Sort();

    public static enum Option {
        RELEVANCE,
        DATE,
        METADATA;
    }
    
    Option option;
    SortOrder direction;
    String metafield;
    
    /**
     * default sorting is relevance with descending order
     */
    public Sort() {
        this.option = Option.RELEVANCE;
        this.direction = SortOrder.DESC;
        this.metafield = null;
    }
    
    public Sort(String description) {
        this();
        if (description.startsWith("date:")) {
            this.option = Option.DATE;
            description = description.substring(5);
            if (description.startsWith("A")) {
                this.direction = SortOrder.ASC;
            } else {
                this.direction = SortOrder.DESC;
            }
        }
        if (description.startsWith("meta:")) {
            this.option = Option.METADATA;
            description = description.substring(5);
            int p = description.indexOf(':');
            if (p >= 0) {
                this.metafield = description.substring(0, p);
                description = description.substring(p + 1);
                if (description.startsWith("A")) {
                    this.direction = SortOrder.ASC;
                } else {
                    this.direction = SortOrder.DESC;
                }
            }
        }
    }
    
    public SearchRequestBuilder sort(SearchRequestBuilder request) {
        if (this.option == Option.DATE) {
            return request.addSort(WebMapping.last_modified.getMapping().name(), this.direction);
        }
        if (this.option == Option.METADATA) {
            return request.addSort(this.metafield, this.direction);
        }
        return request;
    }
    
}
