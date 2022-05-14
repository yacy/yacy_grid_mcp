/**
 *  AWSS3IO
 *  Copyright 12.05.2022 by Michael Peter Christen, @orbiterlab
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.findify.s3mock.S3Mock;

public class AWSS3IO extends AbstractIO implements GenericIO {

    final AmazonS3Client s3;
    private final String endpointURL, accessKey, secretKey;

    public AWSS3IO(final String endpointURL, final String accessKey, final String secretKey) {
        final BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        final AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(credentials);
        final EndpointConfiguration endpoint = new EndpointConfiguration(endpointURL, Regions.EU_CENTRAL_1.getName());
        this.s3 = (AmazonS3Client) AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpoint)
                .withCredentials(provider)
                .build();
        this.endpointURL = endpointURL;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getEndpointURL() {
        return this.endpointURL;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    @Override
    public void makeBucket(final String bucketName) throws IOException {
        this.s3.createBucket(bucketName);
    }

    @Override
    public boolean bucketExists(final String bucketName) throws IOException {
        return this.s3.doesBucketExistV2(bucketName);
    }

    @Override
    public List<String> listBuckets() throws IOException {
        final List<Bucket> buckets = this.s3.listBuckets();
        final List<String> bucketNames = new ArrayList<>();
        for (final Bucket bucket: buckets) {
            bucketNames.add(bucket.getName());
        }
        return bucketNames;
    }

    private Map<String, Bucket> getBucketsMap() throws IOException {
        final List<Bucket> buckets = this.s3.listBuckets();
        final Map<String, Bucket> bucketMap = new HashMap<>();
        for (final Bucket bucket: buckets) {
            bucketMap.put(bucket.getName(), bucket);
        }
        return bucketMap;
    }

    @Override
    public long bucketCreation(final String bucketName) throws IOException {
        final Bucket bucket = getBucketsMap().get(bucketName);
        if (bucket == null) throw new IOException("bucket " + bucket + " not found");
        return bucket.getCreationDate().getTime();
    }

    @Override
    public void removeBucket(final String bucketName) throws IOException {
        this.s3.deleteBucket(bucketName);
    }

    @Override
    public void write(final IOPath iop, final byte[] object) throws IOException {
        //this.s3.putObject(iop.getBucket(), iop.getPath(), new String(object, StandardCharsets.UTF_8));
        final InputStream is = new ByteArrayInputStream(object);
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream"); // was: text/plain
        metadata.setContentLength(object.length);
        this.s3.putObject(new PutObjectRequest(iop.getBucket(), iop.getPath(), is, metadata));
    }

    @Override
    public void write(final IOPath iop, final PipedOutputStream pos, final long len) throws IOException {
        final InputStream is = new PipedInputStream(pos, 4096);
        final IOException[] ea = new IOException[1];
        ea[0] = null;
        final Thread t = new Thread() {
            @Override
            public void run() {
                this.setName("AWSIO writer for " + iop.toString());
                try {
                    AWSS3IO.this.write(iop, is, len);
                } catch (final IOException e) {
                    e.printStackTrace();
                    ea[0] = e;
                    try {pos.close();} catch (final IOException e1) {} // this kills the calling process on purpose; write will throw an exception
                }
            }
        };
        t.start();
        if (ea[0] != null) throw ea[0];
    }

    /**
     * write a stream with known size (len >= 0) or unknown size (len < 0)
     * @param bucketName
     * @param objectName
     * @param stream
     * @param len
     * @throws IOException
     */
    public void write(final IOPath iop, final InputStream is, final long len) throws IOException {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream"); // was: text/plain
        if (len >= 0) metadata.setContentLength(len);
        this.s3.putObject(new PutObjectRequest(iop.getBucket(), iop.getPath(), is, metadata));
    }

    @Override
    public void copy(final IOPath fromIOp, final IOPath toIOp) throws IOException {
        this.s3.copyObject(fromIOp.getBucket(), fromIOp.getPath(), toIOp.getBucket(), toIOp.getPath());
    }

    @Override
    public InputStream read(final IOPath iop) throws IOException {
        final S3Object obj = this.s3.getObject(iop.getBucket(), iop.getPath());
        return obj.getObjectContent();
    }

    @Override
    public InputStream read(final IOPath iop, final long offset) throws IOException {
        final S3Object obj = this.s3.getObject(iop.getBucket(), iop.getPath());
        if (obj == null) throw new IOException("object does not exist: " + iop.toString());
        final S3ObjectInputStream s3is = obj.getObjectContent();
        s3is.skip(offset);
        return s3is;
    }

    @Override
    public InputStream read(final IOPath iop, final long offset, final long len) throws IOException {
        final InputStream is = read(iop, offset);
        final int l = (int) Math.min(is.available(), len);
        final byte[] a = new byte[l];
        is.read(a);
        return new ByteArrayInputStream(a);
    }

    @Override
    public void remove(final IOPath iop) throws IOException {
        this.s3.deleteObject(iop.getBucket(), iop.getPath());
    }

    @Override
    public List<IOMeta> list(final String bucketName, final String prefix) throws IOException {
        final ObjectListing ol = this.s3.listObjects(bucketName, prefix);
        final List<S3ObjectSummary> os = ol.getObjectSummaries();
        final List<IOMeta> list = new ArrayList<>();
        for (final S3ObjectSummary summary: os) {
            final IOMeta meta = new IOMeta(new IOPath(summary.getBucketName(), summary.getKey()));
            meta.setLastModified(summary.getLastModified().getTime());
            meta.setSize(summary.getSize());
            list.add(meta);
        }
        return list;
    }

    @Override
    public long diskUsage(final String bucketName, final String prefix) throws IOException {
        long du = 0;
        for (final IOMeta meta: list(bucketName, prefix)) du += meta.getSize();
        return du;
    }

    @Override
    public long lastModified(final IOPath iop) throws IOException {
        final S3Object obj = this.s3.getObject(iop.getBucket(), iop.getPath());
        if (obj == null) throw new IOException("object does not exist: " + iop.toString());
        return obj.getObjectMetadata().getLastModified().getTime();
    }

    @Override
    public long size(final IOPath iop) throws IOException {
        final S3Object obj = this.s3.getObject(iop.getBucket(), iop.getPath());
        if (obj == null) throw new IOException("object does not exist: " + iop.toString());
        return obj.getObjectMetadata().getContentLength();
    }

    @Override
    public boolean exists(final IOPath iop) {
        final S3Object obj = this.s3.getObject(iop.getBucket(), iop.getPath());
        if (obj == null) return false;
        final Date date = obj.getObjectMetadata().getExpirationTime();
        if (date == null) return true;
        return date.getTime() < System.currentTimeMillis();
    }

    public static void main(final String[] args) {
        final S3Mock s3mock = new S3Mock.Builder().withPort(8001).withInMemoryBackend().build();
        s3mock.start();

        final GenericIO io = new AWSS3IO("http://127.0.0.1:8001", "a", "a");
        try {
            io.makeBucket("test1");
            io.makeBucket("test2");
            io.makeBucket("test3");
            System.out.println("bucket creation: " + io.bucketExists("test1"));

            final List<String> buckets = io.listBuckets();
            System.out.println("bucket list: ");
            for (int i = 0; i < buckets.size(); i++) {
                System.out.println("bucket " + i + ": " + buckets.get(i));
                System.out.println("..created : " + new Date(io.bucketCreation(buckets.get(i))));
            }

            io.removeBucket("test3");
            final IOPath iop1 = new IOPath("test1", "file1");
            final IOPath iop2 = new IOPath("test1", "file2");
            io.write(iop1, "hello ".getBytes());
            System.out.println("file1: " + new String(io.readAll(iop1)));
            io.write(iop2, "world".getBytes());
            System.out.println("file2: " + new String(io.readAll(iop2)));

            final IOPath iop3 = new IOPath("test1", "file3");
            io.merge(iop1, iop2, iop3);
            System.out.println("file3: " + new String(io.readAll(iop3)));

            final IOPath iop4 = new IOPath("test1", "file4");
            io.copy(iop3, iop4);
            System.out.println("file4: " + new String(io.readAll(iop4)));

            final IOPath iop5 = new IOPath("test1", "file5");
            io.move(iop4, iop5);
            System.out.println("file5: " + new String(io.readAll(iop5)));

            io.remove(iop5);

            /*
            public final List<IOMeta> list(final String bucketName, final String prefix) throws IOException;
            public final long diskUsage(final String bucketName, final String prefix) throws IOException;
            public final long lastModified(final IOPath iop) throws IOException;
            public final long size(final IOPath iop) throws IOException;
            public final boolean exists(final IOPath iop);
             */
        } catch (final IOException e) {
            e.printStackTrace();
        }

        s3mock.stop();
        System.exit(0);
    }
}
