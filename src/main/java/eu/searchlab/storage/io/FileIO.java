/**
 *  FileIO
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileIO extends AbstractIO implements GenericIO {

    private final File basePath;

    public FileIO(final File basePath) throws IOException {
        if (!basePath.exists()) throw new IOException("base path " + basePath.toString() + " does not exist");
        if (!basePath.isDirectory()) throw new IOException("base path " + basePath.toString() + " is not a directory");
        this.basePath = basePath;
    }

    public File getBasePath() {
        return this.basePath;
    }

    private File getBucketFile(final String bucket) {
        final File f = new File(this.basePath, bucket);
        return f;
    }

    private File getObjectFile(final String bucket, final String path) {
        File f = getBucketFile(bucket);
        final String[] paths = path.split("/");
        for (final String p: paths) f = new File(f, p);
        return f;
    }

    private File getObjectFile(final IOPath iop) {
        return getObjectFile(iop.getBucket(), iop.getPath());
    }

    @Override
    public void makeBucket(final String bucketName) throws IOException {
        final File f = getBucketFile(bucketName);
        if (f.exists()) {
            if (f.isDirectory()) return;
            throw new IOException("bucket path " + f.toString() + " is a file, not a directory");
        }
        f.mkdirs();
    }

    @Override
    public boolean bucketExists(final String bucketName) throws IOException {
        final File f = getBucketFile(bucketName);
        return f.exists() && f.isDirectory();
    }

    @Override
    public List<String> listBuckets() throws IOException {
        final String[] b = this.basePath.list();
        final ArrayList<String> l = new ArrayList<>();
        for (final String s: b) {
            if (s.length() == 0) continue;
            if (s.charAt(0) == '.') continue;
            if (new File(this.basePath, s).isDirectory()) l.add(s);
        }
        return l;
    }

    @Override
    public long bucketCreation(final String bucketName) throws IOException {
        final File b = getBucketFile(bucketName);
        return b.lastModified();
    }

    @Override
    public void removeBucket(final String bucketName) throws IOException {
        final File b = getBucketFile(bucketName);
        if (b.exists()) b.delete();
    }

    @Override
    public void write(final IOPath iop, final byte[] object) throws IOException {
        final File f = getObjectFile(iop);
        final FileOutputStream fos = new FileOutputStream(f);
        fos.write(object);
        fos.close();
    }

    @Override
    public void write(final IOPath iop, final PipedOutputStream pos, final long len) throws IOException {
        final File f = getObjectFile(iop);
        final FileOutputStream fos = new FileOutputStream(f);
        final InputStream is = new PipedInputStream(pos, 4096);
        final IOException[] ea = new IOException[1];
        ea[0] = null;
        final AtomicLong ai = new AtomicLong(len);
        final Thread t = new Thread() {
            @Override
            public void run() {
                this.setName("FileIO writer for " + iop.toString());
                final byte[] buffer = new byte[4096];
                int l;
                try {
                    while ((l = is.read(buffer)) > 0) {
                        if (len >= 0) {
                            if (l > ai.get()) {
                                fos.write(buffer, 0, (int) ai.get());
                                break;
                            } else {
                                fos.write(buffer, 0, l);
                                ai.addAndGet((int) -l);
                            }
                        } else {
                            fos.write(buffer, 0, l);
                            ai.addAndGet((int) -l);
                        }
                    }
                    fos.close();
                    is.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                    ea[0] = e;
                    try {pos.close();} catch (final IOException e1) {}
                    try {is.close();} catch (final IOException e1) {}
                }
            }
        };
        t.start();
        if (ea[0] != null) throw ea[0];
    }

    @Override
    public void copy(final IOPath fromIOp, final IOPath toIOp) throws IOException {
        final File from = getObjectFile(fromIOp);
        final File to = getObjectFile(toIOp);
        final FileInputStream fis = new FileInputStream(from);
        final FileOutputStream fos = new FileOutputStream(to);
        final FileChannel src = fis.getChannel();
        final FileChannel dest = fos.getChannel();
        dest.transferFrom(src, 0, src.size());
        src.close();
        dest.close();
        fis.close();
        fos.close();
    }

    @Override
    public void move(final IOPath fromIOp, final IOPath toIOp) throws IOException {
        final File from = getObjectFile(fromIOp);
        final File to = getObjectFile(toIOp);
        if (to.exists()) to.delete();
        from.renameTo(to);
    }

    @Override
    public InputStream read(final IOPath iop) throws IOException {
        final File f = getObjectFile(iop);
        final InputStream bis = new BufferedInputStream(new FileInputStream(f));
        return bis;
    }

    @Override
    public InputStream read(final IOPath iop, final long offset) throws IOException {
        final File f = getObjectFile(iop);
        final InputStream bis = new BufferedInputStream(new FileInputStream(f));
        bis.skip(offset);
        return bis;
    }

    @Override
    public InputStream read(final IOPath iop, final long offset, final long len) throws IOException {
        final InputStream is = read(iop, offset);
        final byte[] b = readAll(is, (int) len);
        return new ByteArrayInputStream(b);
    }

    @Override
    public void remove(final IOPath iop) throws IOException {
        final File f = getObjectFile(iop);
        f.delete();
    }

    @Override
    public List<IOMeta> list(final String bucketName, final String prefix) throws IOException {
        final File f = getObjectFile(bucketName, prefix);
        final String[] u = f.list();
        final List<IOMeta> list = new ArrayList<>(u.length);
        for (final String objectName: u) {
            final File fc = new File(f, objectName);
            final IOMeta meta = new IOMeta(new IOPath(bucketName, (prefix + "/" + objectName).replaceAll("//", "/")));
            meta.setSize(fc.length()).setLastModified(fc.lastModified());
            list.add(meta);
        }
        return list;
    }

    @Override
    public long diskUsage(final String bucketName, final String prefix) throws IOException {
        final File f = getObjectFile(bucketName, prefix);
        long du = 0;
        for (final String objectName: f.list()) {
            du += new File(f, objectName).length();
        }
        return du;
    }

    @Override
    public long lastModified(final IOPath iop) {
        final File f = getObjectFile(iop);
        return f.lastModified();
    }

    @Override
    public long size(final IOPath iop) throws IOException {
        final File f = getObjectFile(iop);
        return f.length();
    }

    @Override
    public boolean exists(final IOPath iop) {
        final File f = getObjectFile(iop);
        return f.exists();
    }
}
