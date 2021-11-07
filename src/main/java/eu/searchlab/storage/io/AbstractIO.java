/**
 *  AbstractIO
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;

public abstract class AbstractIO implements GenericIO {


    public static byte[] readAll(InputStream is, int len) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        byte[] b = new byte[16384];
        while ((c = is.read(b, 0, b.length)) != -1) {
            baos.write(b, 0, c);
            if (len > 0 && baos.size() >= len) break;
        }
        b = baos.toByteArray();
        if (len <= 0) return b;
        if (b.length < len) throw new IOException("only " + b.length + " bytes available in stream");
        if (b.length == len) return b;
        final byte[] a = new byte[(int) len];
        System.arraycopy(b, 0, a, 0, (int) len);
        return a;
    }

    @Override
    public byte[] readAll(IOPath iop) throws IOException {
        return readAll(read(iop), -1);
    }

    @Override
    public byte[] readAll(IOPath iop, long offset) throws IOException {
        return readAll(read(iop, offset), -1);
    }

    @Override
    public byte[] readAll(IOPath iop, long offset, long len) throws IOException {
        return readAll(read(iop, offset), (int) len);
    }

    @Override
    public void merge(IOPath fromIOp0, IOPath fromIOp1, IOPath toIOp) throws IOException {
        final long size0 = this.size(fromIOp0);
        final long size1 = this.size(fromIOp1);
        final long size = size0 < 0 || size1 < 0 ? -1 : size0 + size1;
        final PipedOutputStream pos = new PipedOutputStream();
        this.write(toIOp, pos, size);
        InputStream is = this.read(fromIOp0);
        final byte[] buffer = new byte[4096];
        int l;
        while ((l = is.read(buffer)) > 0) pos.write(buffer, 0, l);
        is.close();
        is = this.read(fromIOp1);
        while ((l = is.read(buffer)) > 0) pos.write(buffer, 0, l);
        is.close();
        pos.close();
    }

    @Override
    public void mergeFrom(IOPath iop, IOPath... fromIOps) throws IOException {
        long size = 0;
        for (final IOPath fromIOp: fromIOps) {
            final long sizeN = this.size(fromIOp);
            if (sizeN < 0) {
                size = -1;
                break;
            }
            size += sizeN;
        }
        final PipedOutputStream pos = new PipedOutputStream();
        this.write(iop, pos, size);
        final byte[] buffer = new byte[4096];
        for (final IOPath fromIOp: fromIOps) {
            final InputStream is = this.read(fromIOp);
            int l;
            while ((l = is.read(buffer)) > 0) pos.write(buffer, 0, l);
            is.close();
        }
        pos.close();
    }

    @Override
    public void move(IOPath fromIOp, IOPath toIOp) throws IOException {
        // there is unfortunately no server-side move
        this.copy(fromIOp, toIOp);
        this.remove(fromIOp);
    }

}
