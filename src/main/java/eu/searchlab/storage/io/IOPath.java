/**
 *  IOPath
 *  Copyright 07.10.2021 by Michael Peter Christen, @orbiterlab
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
 * IOPath
 * An IOPath denote an object stored with a GenericIO endpoint.
 * It is used to bundle bucket and path together while leaving the information
 * which endpoint is storing the object aside. IOPaths are used for higher level
 * data structures which require a single object as denominator.
 * This object can be compared with "File" for file storage.
 * By conventional definition:
 * -  an IOPath is a file if the last element of the path
 *    after the last "/" contains an extension separator, a ".".
 * - a path does not start and does not end with a "/".
 */
public final class IOPath {

    private final String bucket, path;

    public IOPath(String bucket, String path) {
        this.bucket = bucket;
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        this.path = path;
    }

    /**
     * an IOPath is a folder if the last element of the path after the latest "/" does not contains a "."
     * @return
     */
    public final boolean isFolder() {
        int p = this.path.lastIndexOf('/');
        if (p < 0) p = 0;
        return this.path.indexOf('.', p) < 0;
    }

    /**
     * a folder is a root folder it can not be truncated
     * @return
     */
    public final boolean isRootFolder() {
        int p = this.path.lastIndexOf('/');
        assert p != 0;
        return p > 0;
    }

    public final String getBucket() {
        return this.bucket;
    }

    public final String getPath() {
        return this.path;
    }

    public IOPath append(String spath) {
        if (!isFolder()) throw new RuntimeException("IOPath must be a folder to append a path: " + this.toString());
        return new IOPath(this.bucket, this.path + "/" + spath);
    }

    public IOPath truncate() {
        int p = this.path.lastIndexOf('/');
        if (p < 0) throw new RuntimeException("IOPath must have at leas one folder to truncate: " + this.toString());
        return new IOPath(this.bucket, this.path.substring(0, p));
    }

    @Override
    public String toString() {
        return this.bucket + "/" + this.path;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
