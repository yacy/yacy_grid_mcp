/**
 *  IOMeta
 *  Copyright 06.10.2021 by Michael Peter Christen, @orbiterlab
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

package eu.searchlab.storage.io;

/**
 * IOMeta is the collection of metadata about a file of a IOPath
 */
public class IOMeta {

    private final IOPath iop;
    private long lastModified;
    private long size;
    private boolean isDir;

    public IOMeta(final IOPath iop) {
        this.iop = iop;
        this.lastModified = -1;
        this.size = -1;
        this.isDir = iop.isFolder();
    }

    public IOMeta setLastModified(final long lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public IOMeta setSize(final long size) {
        this.size = size;
        return this;
    }

    public IOPath getIOPath() {
        return this.iop;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public long getSize() {
        return this.size;
    }

    public boolean isDir() {
        return this.isDir;
    }
}
