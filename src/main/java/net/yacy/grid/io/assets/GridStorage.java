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

    private StorageFactory<byte[]> s3 = null;
    private StorageFactory<byte[]> ftp = null;
    private StorageFactory<byte[]> mcp = null;
    private boolean deleteafterread;
    private AtomicInteger s3_fail = new AtomicInteger(0);
    private AtomicInteger ftp_fail = new AtomicInteger(0);

    // connector details
    private String host, username, password; // host has the shape of <bucket>.<endpoint-host> in case of a s3 host
    private int port;
    private boolean active;

    /**
     * create a grid storage.
     * @param deleteafterread if true, an asset is deleted from the asset store after it has beed read
     * @param basePath a local path; can be NULL which means that no local storage is wanted
     */
    public GridStorage(boolean deleteafterread, File basePath) {
        super(deleteafterread, basePath);
        this.deleteafterread = deleteafterread;
        this.ftp = null;
        this.host = null;
        this.username = null;
        this.password = null;
        this.port = -1;
        this.active = true;
    }

    public boolean connectS3(String bucket_endpoint, int port, String username, String password, boolean active) {
        this.host = bucket_endpoint;
        this.port = port;
        this.username = username;
        this.password = password;
        this.active = active;
        return checkConnectionS3();
    }

    public boolean connectS3(String url, boolean active) {
        MultiProtocolURL u = null;
        try {
            u = new MultiProtocolURL(url);
        } catch (MalformedURLException e) {
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

    private boolean checkConnectionS3() {
        try {
            StorageFactory<byte[]> s3 = new S3StorageFactory(this.host, this.port, this.username, this.password, this.deleteafterread);
            s3.getStorage().checkConnection(); // test the connection
            this.s3 = s3;
            return true;
        } catch (IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectS3 trying to connect to the ftp server at " + this.host + ":" + this.port + " failed");
            return false;
        }
    }

    public boolean connectFTP(String host, int port, String username, String password, boolean active) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.active = active;
        return checkConnectionFTP();
    }

    public boolean connectFTP(String url, boolean active) {
        MultiProtocolURL u = null;
        try {
            u = new MultiProtocolURL(url);
        } catch (MalformedURLException e) {
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

    private boolean checkConnectionFTP() {
        try {
            StorageFactory<byte[]> ftp = new FTPStorageFactory(this.host, this.port, this.username, this.password, this.deleteafterread, this.active);
            ftp.getStorage().checkConnection(); // test the connection
            this.ftp = ftp;
            return true;
        } catch (IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectFTP trying to connect to the ftp server at " + this.host + ":" + this.port + " failed");
            return false;
        }
    }

    public boolean isS3Connected() {
        return this.s3 != null;
    }

    public boolean isFTPConnected() {
        return this.ftp != null;
    }

    public boolean connectMCP(String host, int port, boolean active) {
        try {
            this.mcp = new MCPStorageFactory(this, host, port, active);
            this.mcp.getStorage().checkConnection();
            return true;
        } catch (IOException e) {
            Logger.debug(this.getClass(), "GridStorage.connectMCP trying to connect to a Storage over MCP at " + host + ":" + port + " failed");
            return false;
        }
    }

    @Override
    public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
        if (this.s3 != null && this.s3_fail.get() < 10) {
            try {
                StorageFactory<byte[]> sf = this.s3.getStorage().store(path, asset);
                this.s3_fail.set(0);
                return sf;
            } catch (IOException e) {
                Logger.debug(this.getClass(), "GridStorage.store trying to connect to the ftp server failed", e);
            }
            this.s3_fail.incrementAndGet();
        }
        if (this.ftp != null && this.ftp_fail.get() < 10) {
            retryloop: for (int retry = 0; retry < 40; retry++) {
                try {
                    StorageFactory<byte[]> sf = this.ftp.getStorage().store(path, asset);
                    this.ftp_fail.set(0);
                    return sf;
                } catch (IOException e) {
                    String cause = e.getMessage();
                    if (cause != null && cause.contains("refused")) break retryloop;
                    // possible causes:
                    // 421 too many connections. possible counteractions: in apacheftpd, set i.e. ftpserver.user.anonymous.maxloginnumber=200 and ftpserver.user.anonymous.maxloginperip=200
                    if (cause.indexOf("421") >= 0) {try {Thread.sleep(retry * 500);} catch (InterruptedException e1) {} continue retryloop;}
                    Logger.debug(this.getClass(), "GridStorage.store trying to connect to the ftp server failed, attempt " + retry + ": " + cause, e);
                }
            }
            this.ftp_fail.incrementAndGet();
        }
        if (this.mcp != null) try {
            return this.mcp.getStorage().store(path, asset);
        } catch (IOException e) {
            Logger.debug(this.getClass(), "GridStorage.store trying to connect to the mcp failed: " + e.getMessage(), e);
        }
        // failback to local storage
        return super.store(path, asset);
    }

    @Override
    public Asset<byte[]> load(String path) throws IOException {
        try {
            // we first load from the local assets, if possible.
            // this is happening first to be able to fast-fail.
            // failing with a TCP/IP connected service would be much more costly.
            return super.load(path);
        } catch (IOException e) {
            // do nothing, we will try again with alternative methods
        }
        if (this.s3 != null && this.s3_fail.get() < 10) {
                try {
                    Asset<byte[]> asset = this.s3.getStorage().load(path);
                    this.s3_fail.set(0);
                    return asset;
                } catch (IOException e) {
                    Logger.debug(this.getClass(), "GridStorage.load trying to connect to the s3 server failed", e);
                }
            this.s3_fail.incrementAndGet();
        }
        if (this.ftp != null && this.ftp_fail.get() < 10) {
            retryloop: for (int retry = 0; retry < 40; retry++) {
                try {
                    Asset<byte[]> asset = this.ftp.getStorage().load(path);
                    this.ftp_fail.set(0);
                    return asset;
                } catch (IOException e) {
                    String cause = e.getMessage();
                    // possible causes:
                    // 421 too many connections. possible counteractions: in apacheftpd, set i.e. ftpserver.user.anonymous.maxloginnumber=200 and ftpserver.user.anonymous.maxloginperip=200
                    if (cause.indexOf("421") >= 0) {try {Thread.sleep(retry * 500);} catch (InterruptedException e1) {} continue retryloop;}
                    if (cause.indexOf("refused") >= 0) break retryloop; // this will not go anywhere
                    Logger.debug(this.getClass(), "GridStorage.load trying to connect to the ftp server failed, attempt " + retry + ": " + cause, e);
                }
            }
            this.ftp_fail.incrementAndGet();
        }
        if (this.mcp != null) try {
            return this.mcp.getStorage().load(path);
        } catch (IOException e) {
            Logger.debug(this.getClass(), "GridStorage.load trying to connect to the mcp failed: " + e.getMessage(), e);
        }
        // no options left
        throw new IOException("no storage factory available to load asset");
    }

    @Override
    public void close() {
        if (this.s3 != null) try {this.s3.close();} catch (Throwable e) {}
        if (this.ftp != null) try {this.ftp.close();} catch (Throwable e) {}
        try {super.close();} catch (Throwable e) {}
    }

}
