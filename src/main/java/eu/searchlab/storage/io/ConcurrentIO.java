/**
 *  ConcurrentIO
 *  Copyright 10.05.2022 by Michael Peter Christen, @orbiterlab
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Use lock files to get exclusive access to shared files.
 */
public final class ConcurrentIO {

    private final GenericIO io;

    /**
     * ConcurrentIO
     * @param io
     */
    public ConcurrentIO(final GenericIO io) {
        this.io = io;
    }

    public final GenericIO getIO() {
        return this.io;
    }

    private final static IOPath[] lockFile(final IOPath... iops) {
        final IOPath[] lockFiles = new IOPath[iops.length];
        for (int i = 0; i < iops.length; i++) {
            final IOPath iop = iops[i];
            if (iop.isFolder()) throw new RuntimeException("IOPath must not be a folder: " + iop.toString());
            lockFiles[i] = new IOPath(iop.getBucket(), iop.getPath() + ".lock");
        }
        return lockFiles;
    }

    private final static IOPath[] lockFile(final IOObject... ioos) {
        final IOPath[] iops = new IOPath[ioos.length];
        for (int i = 0; i < ioos.length; i++) iops[i] = ioos[i].getPath();
        return lockFile(iops);
    }

    private final IOObject readLockFile(final IOPath lockFile) throws IOException {
        assert this.io.exists(lockFile);
        final byte[] a = this.io.readAll(lockFile);
        return new IOObject(lockFile, a);
    }

    private final void writeLockFile(final IOPath... lockFiles) throws IOException {
        for (int i = 0; i < lockFiles.length; i++) {
            final IOPath lockFile = lockFiles[i];
            assert !this.io.exists(lockFile);
            final InetAddress localhost = InetAddress.getLocalHost();
            final long time = System.currentTimeMillis();
            try {
                final JSONObject json = new JSONObject(true)
                        .put("host", localhost.getCanonicalHostName())
                        .put("ip", localhost.getHostAddress())
                        .put("time", time);
                this.io.write(lockFile, json.toString(2).getBytes(StandardCharsets.UTF_8));
            } catch (final JSONException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private final void releaseeLockFile(final IOPath... lockFiles) throws IOException {
        for (int i = 0; i < lockFiles.length; i++) {
            final IOPath lockFile = lockFiles[i];
            assert this.io.exists(lockFile);
            this.io.remove(lockFile);
        }
    }

    private final boolean waitUntilUnlock(final long waitingtime, final IOPath... lockFiles) {
        if (waitingtime <= 0) return true;
        final long timeout = System.currentTimeMillis() + waitingtime;
        waitloop: while (System.currentTimeMillis() < timeout) {
            for (int i = 0; i < lockFiles.length; i++) {
                if (this.io.exists(lockFiles[i])) {
                    try {Thread.sleep(1000);} catch (final InterruptedException e) {}
                    continue waitloop;
                }
            }
            // none of the lock files exist, this is a success!
            return true;
        }
        return false;
    }

    public final void write(final long waitingtime, final IOObject... ioos) throws IOException {
        final IOPath[] lockFiles = lockFile(ioos);
        if (waitUntilUnlock(waitingtime, lockFiles)) {
            writeLockFile(lockFiles);
            for (int i = 0; i < ioos.length; i++) {
                this.io.write(ioos[i].getPath(), ioos[i].getObject());
            }
            releaseeLockFile(lockFiles);
        } else {
            throw new IOException("timeout waiting for lock disappearance");
        }
    }

    public final void writeForced(final long waitingtime, final IOObject... ioos) throws IOException {
        try {
            write(waitingtime, ioos);
        } catch (final IOException e) {
            for (int i = 0; i < ioos.length; i++) deleteLock(ioos[i].getPath());
            write(-1, ioos);
        }
    }

    public void writeGZIPForced(final long waitingtime, final IOPath iopgz, final byte[] object) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPOutputStream zipStream = new GZIPOutputStream(baos);
        zipStream.write(object);
        zipStream.close();
        baos.close();
        writeForced(waitingtime, new IOObject(iopgz, baos.toByteArray()));
    }

    public final IOObject[] read(final long waitingtime, final IOPath... iops) throws IOException {
        final IOPath[] lockFiles = lockFile(iops);
        final IOObject[] as = new IOObject[iops.length];
        if (waitUntilUnlock(waitingtime, lockFiles)) {
            writeLockFile(lockFiles);
            for (int i = 0; i < iops.length; i++) {
                final byte[] a = this.io.readAll(iops[i]);
                as[i] = new IOObject(iops[i], a);
            }
            releaseeLockFile(lockFiles);
            return as;
        } else {
            throw new IOException("timeout waiting for lock disappearance");
        }
    }

    public final IOObject[] readForced(final long waitingtime, final IOPath... iops) throws IOException {
        try {
            return read(waitingtime, iops);
        } catch (final IOException e) {
            deleteLock(iops);
            return read(-1, iops);
        }
    }

    public final void remove(final long waitingtime, final IOPath... iops) throws IOException {
        final IOPath[] lockFiles = lockFile(iops);
        if (waitUntilUnlock(waitingtime, lockFiles)) {
            writeLockFile(lockFiles);
            for (int i = 0; i < iops.length; i++) {
                this.io.remove(iops[i]);
            }
            releaseeLockFile(lockFiles);
        } else {
            throw new IOException("timeout waiting for lock disappearance");
        }
    }

    public final void removeForced(final long waitingtime, final IOPath... iops) throws IOException {
        try {
            remove(waitingtime, iops);
        } catch (final IOException e) {
            deleteLock(iops);
            remove(-1, iops);
        }
    }

    public final boolean isLocked(final IOPath... iop) {
        final IOPath[] lockFiles = lockFile(iop);
        for (int i = 0; i < lockFiles.length; i++) {
            if (this.io.exists(lockFiles[i])) return true;
        }
        return false;
    }

    public final void deleteLock(final IOPath... iop) {
        final IOPath[] lockFiles = lockFile(iop);
        try {
            for (int i = 0; i < lockFiles.length; i++) {
                this.io.remove(lockFiles[i]);
            }
        } catch (final IOException e) {}
    }

    public final String lockedByHost(final IOPath iop) throws IOException {
        final IOPath[] lockFiles = lockFile(iop);
        for (int i = 0; i < lockFiles.length; i++) {
            if (this.io.exists(lockFiles[i])) {
                final IOObject ioo = readLockFile(lockFiles[i]);
                try {
                    return ioo.getJSONObject().getString("host");
                } catch (final JSONException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        throw new IOException("no lockfile exist");
    }

    public final String lockedByIP(final IOPath iop) throws IOException {
        final IOPath[] lockFiles = lockFile(iop);
        for (int i = 0; i < lockFiles.length; i++) {
            if (this.io.exists(lockFiles[i])) {
                final IOObject ioo = readLockFile(lockFiles[i]);
                try {
                    return ioo.getJSONObject().getString("ip");
                } catch (final JSONException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        throw new IOException("no lockfile exist");
    }

    public final long lockedByTime(final IOPath iop) throws IOException {
        final IOPath[] lockFiles = lockFile(iop);
        for (int i = 0; i < lockFiles.length; i++) {
            if (this.io.exists(lockFiles[i])) {
                final IOObject ioo = readLockFile(lockFiles[i]);
                try {
                    return ioo.getJSONObject().getLong("time");
                } catch (final JSONException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        throw new IOException("no lockfile exist");
    }

}
