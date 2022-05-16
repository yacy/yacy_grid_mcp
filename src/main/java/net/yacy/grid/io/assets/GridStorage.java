/**
 *  GridStorage
 *  Copyright 28.1.2017 by Michael Peter Christen, @orbiterlab
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicInteger;

import net.yacy.grid.tools.Logger;
import net.yacy.grid.tools.MultiProtocolURL;

public class GridStorage extends PeerStorage implements Storage<byte[]> {

    private final boolean deleteafterread;
    private final AtomicInteger s3_fail = new AtomicInteger(0);
    private final AtomicInteger ftp_fail = new AtomicInteger(0);

    // connector details
    private String host, username, password; // host has the shape of <bucket>.<endpoint-host> in case of a s3 host
    private int port;
    private boolean active;

    /**
     * create a grid storage.
     * @param deleteafterread if true, an asset is deleted from the asset store after it has beed read
     * @param basePath a local path; can be NULL which means that no local storage is wanted
     */
    public GridStorage(final boolean deleteafterread, final File basePath) {
        super(deleteafterread, basePath);
        this.deleteafterread = deleteafterread;
        //this.ftp = null;
        this.host = null;
        this.username = null;
        this.password = null;
        this.port = -1;
        this.active = true;
    }

    /**
     * connect a bucket endpoint.
     * ATTENTION: respect the correct bucket_endpoint schema
     * @param bucket_endpoint must be <bucketname>"."<hostname>
     * @param port
     * @param username
     * @param password
     * @param active
     * @return
     */
    public boolean connectS3(final String bucket_endpoint, final int port, final String username, final String password, final boolean active) {
        this.host = bucket_endpoint;
        this.port = port;
        this.username = username;
        this.password = password;
        this.active = active;
        return checkConnectionS3();
    }

    public boolean connectS3(final String url, final boolean active) {
        MultiProtocolURL u = null;
        try {
            u = new MultiProtocolURL(url);
        } catch (final MalformedURLException e) {
            Logger.debug(this.getClass(), "GridStorage.connectS3 trying to connect to the s3 server at " + url + " failed: " + e.getMessage());
            return false;
        }
        this.host = u.getHost();
        this.port = u.getPort();
        this.username = u.getUser();
        this.password = u.getPassword();
        this.active = active;
        return checkConnectionS3();
    }

    public boolean checkConnectionS3() {
        try {
            final StorageFactory<byte[]> s3 = new S3StorageFactory(this.host, this.port, this.username, this.password, this.deleteafterread);
            s3.getStorage().checkConnection(); // test the connection
            this.factory = s3;
            return true;
        } catch (final IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectS3 trying to connect to the ftp server at " + this.host + ":" + this.port + " failed");
            return false;
        }
    }

    public boolean connectFTP(final String host, final int port, final String username, final String password, final boolean active) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.active = active;
        return checkConnectionFTP();
    }

    public boolean connectFTP(final String url, final boolean active) {
        MultiProtocolURL u = null;
        try {
            u = new MultiProtocolURL(url);
        } catch (final MalformedURLException e) {
            Logger.debug(this.getClass(), "GridStorage.connectFTP trying to connect to the ftp server at " + url + " failed: " + e.getMessage());
            return false;
        }
        this.host = u.getHost();
        this.port = u.getPort();
        this.username = u.getUser();
        this.password = u.getPassword();
        this.active = active;
        return checkConnectionFTP();
    }

    public boolean checkConnectionFTP() {
        try {
            final StorageFactory<byte[]> ftp = new FTPStorageFactory(this.host, this.port, this.username, this.password, this.deleteafterread, this.active);
            ftp.getStorage().checkConnection(); // test the connection
            this.factory = ftp;
            return true;
        } catch (final IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectFTP trying to connect to the ftp server at " + this.host + ":" + this.port + " failed");
            return false;
        }
    }

    public boolean isS3Connected() {
        return this.factory != null && this.factory instanceof S3StorageFactory;
    }

    public boolean isFTPConnected() {
        return this.factory != null && this.factory instanceof FTPStorageFactory;
    }

    public boolean isMCPConnected() {
        return this.factory != null && this.factory instanceof MCPStorageFactory;
    }

    public boolean connectMCP(final String host, final int port, final boolean active) {
        try {
            this.factory = new MCPStorageFactory(this, host, port, active);
            this.factory.getStorage().checkConnection();
            return true;
        } catch (final IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectMCP trying to connect to a Storage over MCP at " + host + ":" + port + " failed");
            return false;
        }
    }
    
    @Override
    public StorageFactory<byte[]> store(final String path, final byte[] asset) throws IOException {
        if (isS3Connected() && this.s3_fail.get() < 10) {
            try {
                final StorageFactory<byte[]> sf = this.factory.getStorage().store(path, asset);
                this.s3_fail.set(0);
                return sf;
            } catch (final IOException e) {
                Logger.debug(this.getClass(), "GridStorage.store trying to connect to the ftp server failed", e);
            }
            this.s3_fail.incrementAndGet();
        }
        if (isFTPConnected() && this.ftp_fail.get() < 10) {
            retryloop: for (int retry = 0; retry < 40; retry++) {
                try {
                    final StorageFactory<byte[]> sf = this.factory.getStorage().store(path, asset);
                    this.ftp_fail.set(0);
                    return sf;
                } catch (final IOException e) {
                    final String cause = e.getMessage();
                    if (cause != null && cause.contains("refused")) break retryloop;
                    // possible causes:
                    // 421 too many connections. possible counteractions: in apacheftpd, set i.e. ftpserver.user.anonymous.maxloginnumber=200 and ftpserver.user.anonymous.maxloginperip=200
                    if (cause.indexOf("421") >= 0) {try {Thread.sleep(retry * 500);} catch (final InterruptedException e1) {} continue retryloop;}
                    Logger.debug(this.getClass(), "GridStorage.store trying to connect to the ftp server failed, attempt " + retry + ": " + cause, e);
                }
            }
            this.ftp_fail.incrementAndGet();
        }
        if (isMCPConnected()) try {
            return this.factory.getStorage().store(path, asset);
        } catch (final IOException e) {
            Logger.debug(this.getClass(), "GridStorage.store trying to connect to the mcp failed: " + e.getMessage(), e);
        }
        // failback to local storage
        return super.store(path, asset);
    }

    @Override
    public Asset<byte[]> load(final String path) throws IOException {
        try {
            // we first load from the local assets, if possible.
            // this is happening first to be able to fast-fail.
            // failing with a TCP/IP connected service would be much more costly.
            return super.load(path);
        } catch (final IOException e) {
            // do nothing, we will try again with alternative methods
        }
        if (isS3Connected() && this.s3_fail.get() < 10) {
                try {
                    final Asset<byte[]> asset = this.factory.getStorage().load(path);
                    this.s3_fail.set(0);
                    return asset;
                } catch (final IOException e) {
                    Logger.debug(this.getClass(), "GridStorage.load trying to connect to the s3 server failed", e);
                }
            this.s3_fail.incrementAndGet();
        }
        if (isFTPConnected() && this.ftp_fail.get() < 10) {
            retryloop: for (int retry = 0; retry < 40; retry++) {
                try {
                    final Asset<byte[]> asset = this.factory.getStorage().load(path);
                    this.ftp_fail.set(0);
                    return asset;
                } catch (final IOException e) {
                    final String cause = e.getMessage();
                    // possible causes:
                    // 421 too many connections. possible counteractions: in apacheftpd, set i.e. ftpserver.user.anonymous.maxloginnumber=200 and ftpserver.user.anonymous.maxloginperip=200
                    if (cause.indexOf("421") >= 0) {try {Thread.sleep(retry * 500);} catch (final InterruptedException e1) {} continue retryloop;}
                    if (cause.indexOf("refused") >= 0) break retryloop; // this will not go anywhere
                    Logger.debug(this.getClass(), "GridStorage.load trying to connect to the ftp server failed, attempt " + retry + ": " + cause, e);
                }
            }
            this.ftp_fail.incrementAndGet();
        }
        if (isMCPConnected()) try {
            return this.factory.getStorage().load(path);
        } catch (final IOException e) {
            Logger.debug(this.getClass(), "GridStorage.load trying to connect to the mcp failed: " + e.getMessage(), e);
        }
        // no options left
        throw new IOException("no storage factory available to load asset");
    }

    @Override
    public void close() {
        if (isS3Connected()) try {this.factory.close();} catch (final Throwable e) {}
        if (isFTPConnected()) try {this.factory.close();} catch (final Throwable e) {}
        try {super.close();} catch (final Throwable e) {}
    }

}
