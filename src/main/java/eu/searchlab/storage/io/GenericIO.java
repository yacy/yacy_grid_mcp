/**
 *  GenericIO
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.List;

/**
 * Storage Engine which makes an abstraction of the actual storage system, which can be i.e.:
 * - File System
 * - FTP
 * - SMB
 * - S3
 * To map the concept of drives (SMB) and Buckets (S3) we introduce the concept of an IO <project>:
 * - a <project> is the name of a storage tenant
 * - in File Systems, FTP and SMB the <project> becomes another sub-path in front of the given path
 * - paths and <project> are encapsulated into IOPath objetcs.
 */
public interface GenericIO {

    /**
     * make a bucket
     * @param bucketName
     * @throws IOException
     */
    public void makeBucket(final String bucketName) throws IOException;

    /**
     * test if a bucket exists
     * @param bucketName
     * @return
     * @throws IOException
     */
    public boolean bucketExists(final String bucketName) throws IOException;

    /**
     * list all buckets
     * @return
     * @throws IOException
     */
    public List<String> listBuckets() throws IOException;

    /**
     * return bucket creation time
     * @param bucketName
     * @return
     * @throws IOException
     */
    public long bucketCreation(final String bucketName) throws IOException;

    /**
     * remove bucket; that bucket must be empty or the method will fail
     * @param bucketName
     * @throws IOException
     */
    public void removeBucket(final String bucketName) throws IOException;

    /**
     * write an object from a byte array
     * @param iop
     * @param object
     * @throws IOException
     */
    public void write(final IOPath iop, final byte[] object) throws IOException;

    /**
     * write an object gzipped from a byte array
     * @param iop
     * @param object
     * @throws IOException
     */
    public void writeGZIP(final IOPath iop, final byte[] object) throws IOException;

    /**
     * write to an object until given PipedOutputStream is closed
     * @param bucketName
     * @param objectName
     * @param pos
     * @param len
     * @throws IOException
     */
    public void write(final IOPath iop, final PipedOutputStream pos, final long len) throws IOException;

    /**
     * client-side merge of two objects into a new object
     * @param fromIOp0
     * @param fromIOp1
     * @param toIOp
     * @throws IOException
     */
    public void merge(final IOPath fromIOp0, final IOPath fromIOp1, final IOPath toIOp) throws IOException;

    /**
     * merge an arbitrary number of objects into one target object
     * @param IOPath iop
     * @param fromIOps
     * @throws IOException
     */
    public void mergeFrom(final IOPath iop, final IOPath... fromIOps) throws IOException;

    /**
     * server-side copy of an object to another object
     * @param fromIOp
     * @param toIOp
     * @throws IOException
     */
    public void copy(final IOPath fromIOp, final IOPath toIOp) throws IOException;

    /**
     * renaming/moving of one object into another. This is done using client-side object duplication
     * with deletion of the original because S3 does not support renaming/moving.
     * @param fromIOp
     * @param toIOp
     * @throws IOException
     */
    public void move(final IOPath fromIOp, final IOPath toIOp) throws IOException;

    /**
     * reading of an object into a byte array
     * @param iop
     * @return whole object as byte[]
     * @throws IOException
     */
    public byte[] readAll(final IOPath iop) throws IOException;

    /**
     * reading of an object beginning with an offset into a byte array
     * @param iop
     * @param offset
     * @return whole object as byte[]
     * @throws IOException
     */
    public byte[] readAll(final IOPath iop, final long offset) throws IOException;

    /**
     * reading of an object from an offset with given length into a byte array
     * @param iop
     * @param offset
     * @param len
     * @return whole object as byte[]
     * @throws IOException
     */
    public byte[] readAll(final IOPath iop, final long offset, final long len) throws IOException;

    /**
     * reading of an object into a stream
     * @param iop
     * @return InputStream
     * @throws IOException
     */
    public InputStream read(final IOPath iop) throws IOException;

    /**
     * reading of a compressed object into a stream
     * @param iop
     * @return whole object as byte[]
     * @throws IOException
     */
    public InputStream readGZIP(final IOPath iop) throws IOException;

    /**
     * reading of an object beginning with an offset
     * @param iop
     * @param offset
     * @return InputStream
     * @throws IOException
     */
    public InputStream read(final IOPath iop, final long offset) throws IOException;

    /**
     * reading of an object from an offset with given length
     * @param iop
     * @param offset
     * @param len
     * @return InputStream
     * @throws IOException
     */
    public InputStream read(final IOPath iop, final long offset, final long len) throws IOException;

    /**
     * removal of an object
     * @param iop
     * @throws IOException
     */
    public void remove(final IOPath iop) throws IOException;

    /**
     * listing of object names in a given prefix path
     * @param bucketName
     * @param prefix
     * @return list of object names
     * @throws IOException
     */
    public List<IOMeta> list(final String bucketName, final String prefix) throws IOException;


    /**
     * calculate the disk usage in a given path
     * @param bucketName
     * @param prefix
     * @return disk usage in bytes
     * @throws IOException
     */
    public long diskUsage(final String bucketName, final String prefix) throws IOException;

    /**
     * last-modified date of an object
     * @param iop
     * @return milliseconds since epoch
     * @throws IOException
     */
    public long lastModified(final IOPath iop) throws IOException;

    /**
     * size of an object
     * @param iop
     * @return size in bytes
     * @throws IOException
     */
    public long size(final IOPath iop) throws IOException;

    /**
     * checks if an item exists
     * @param iop
     * @return true if file exists
     * @throws IOException
     */
    public boolean exists(final IOPath iop);


}