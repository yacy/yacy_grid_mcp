/**
 *  Asset
 *  Copyright 28.1.2017 by Michael Peter Christen, @0rb1t3r
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

package net.yacy.grid.io.assets;

public class Asset<A> {

    private StorageFactory<A> factory;
    private A payload;
    
    public Asset(StorageFactory<A> factory, A payload) {
        this.factory = factory;
        this.payload = payload;
    }
    
    public StorageFactory<A> getFactory() {
        return this.factory;
    }
    
    public A getPayload() {
        return this.payload;
    }
    
}
