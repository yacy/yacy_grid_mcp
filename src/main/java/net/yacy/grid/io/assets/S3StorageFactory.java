/**
 *  S3StorageFactory
 *  Copyright 07.11.2021 by Michael Peter Christen, @orbiterlab
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

import java.io.IOException;
import java.util.List;

import eu.searchlab.storage.io.GenericIO;
import eu.searchlab.storage.io.IOPath;
import eu.searchlab.storage.io.S3IO;

public class S3StorageFactory  implements StorageFactory<byte[]> {

    private String bucket, endpoint, accessKey, secretKey;
    private int port;
    private final Storage<byte[]> s3client;
    private boolean deleteafterread;

    public S3StorageFactory(String bucket_endpoint, int port, final String accessKey, final String secretKey, boolean deleteafterread) throws IOException {
        // we expect that the server is constructed as <bucket>.<endpointHost>
        // so we deconstruct the bucket and endpoint information from the given server
        int p = bucket_endpoint.indexOf('.');
        if (p < 0) throw new IOException("server must be <bucket>.<endpointHost>");
        this.bucket = bucket_endpoint.substring(0, p);
        this.endpoint = bucket_endpoint.substring(p + 1);
        this.port = port;

        this.accessKey = accessKey == null ? "" : accessKey;
        this.secretKey = secretKey == null ? "" : secretKey;
        this.deleteafterread = deleteafterread;

        this.s3client = new Storage<byte[]>() {
            private GenericIO io = null;

            private GenericIO initConnection() throws IOException {
                String endpointURL = (port == 443 ? "https://" : "http://") + S3StorageFactory.this.endpoint + (S3StorageFactory.this.port == 80 || S3StorageFactory.this.port == 443 ? "" : ":" + S3StorageFactory.this.port);
                S3IO s3io = new S3IO(endpointURL, S3StorageFactory.this.accessKey, S3StorageFactory.this.secretKey);
                List<String> buckets = s3io.listBuckets();
                // find or create bucket
                if (buckets == null || buckets.size() == 0 || !buckets.contains(S3StorageFactory.this.bucket)) {
                    s3io.makeBucket(S3StorageFactory.this.bucket);
                }
                return s3io;
            }

            @Override
            public void checkConnection() throws IOException {
                if (this.io == null) this.io = initConnection();
                List<String> buckets = this.io.listBuckets();
                // there must be at least one bucket
                if (buckets == null || buckets.size() == 0) throw new IOException("connection to s3:" + S3StorageFactory.this.endpoint + " is possible, but no buckets are available");
                // the list must also contain the addressed bucket
                if (!buckets.contains(S3StorageFactory.this.bucket)) throw new IOException("connection to s3 establishedm but bucket " + S3StorageFactory.this.bucket + " not available");
            }

            @Override
            public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
                if (this.io == null) this.io = initConnection();
                IOPath iop = new IOPath(S3StorageFactory.this.bucket, path);
                try {
                    this.io.write(iop, asset);
                } catch (IOException e) {
                    // try again with fresh connection
                    this.io = initConnection();
                    this.io.write(iop, asset);
                }
                return S3StorageFactory.this;
            }

            @Override
            public Asset<byte[]> load(String path) throws IOException {
                if (this.io == null) this.io = initConnection();
                IOPath iop = new IOPath(S3StorageFactory.this.bucket, path);
                byte[] b = null;
                try {
                    b = this.io.readAll(iop);
                } catch (IOException e) {
                    // try again
                    this.io = initConnection();
                    b = this.io.readAll(iop);
                }
                if (b == null) throw new IOException("cannot read s3://" + S3StorageFactory.this.bucket + "." + S3StorageFactory.this.endpoint + "/" + iop.toString());
                if (S3StorageFactory.this.deleteafterread) {
                    this.io.remove(iop);
                }
                return new Asset<byte[]>(S3StorageFactory.this, b);
            }

            @Override
            public void close() {
                this.io = null;
            }

        };
    }

    @Override
    public String getSystem() {
        return "s3";
    }

    @Override
    public String getHost() {
        return this.endpoint;
    }

    @Override
    public boolean hasDefaultPort() {
        return this.port == 80 || this.port == 443;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public String getConnectionURL() {
        return "s3://" + this.bucket + "." + this.endpoint + (hasDefaultPort() ? "" : ":" + this.port);
    }

    @Override
    public Storage<byte[]> getStorage() throws IOException {
        return this.s3client;
    }

    @Override
    public void close() {
        this.s3client.close();
    }

}
