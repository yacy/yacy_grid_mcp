/**
 *  ShardingMethod
 *  Copyright 08.07.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.messages;

public enum ShardingMethod {

    ROUND_ROBIN,   // go around all queues all the time
    LEAST_FILLED,  // take the one which has least entries
    HASH,          // use a hashing key to determine a queue
    LOOKUP,        // lookup a queue with the hashing key; if not determined yet, use LEAST_FILLED
    BALANCE,       // like LOOKUP, but if LOOKUP would return a queue with most entries and there exist one queue with none entries, the queue is switched to the empty one
    RANDOM,        // just a random queue
    FIRST;          // the last queue
    
}
