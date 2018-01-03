/**
 *  FilesystemStorageFactory
 *  Copyright 28.1.2017 by Michael Peter Christen, @0rb1t3r
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import net.yacy.grid.mcp.Data;

public class FTPStorageFactory implements StorageFactory<byte[]> {

    private static int DEFAULT_PORT = 21;
    
    private String server, username, password;
    private int port;
    private Storage<byte[]> ftpClient;
    
    public FTPStorageFactory(String server, int port, String username, String password) throws IOException {
        this.server = server;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.port = port;

        this.ftpClient = new Storage<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                return;
            }
            
            private FTPClient initConnection() throws IOException {
                FTPClient ftp = new FTPClient();
                ftp.setDataTimeout(3000);
                ftp.setConnectTimeout(20000);
                if (FTPStorageFactory.this.port < 0 || FTPStorageFactory.this.port == DEFAULT_PORT) {
                    ftp.connect(FTPStorageFactory.this.server);
                } else {
                    ftp.connect(FTPStorageFactory.this.server, FTPStorageFactory.this.port);
                }
                ftp.enterLocalPassiveMode(); // the server opens a data port to which the client conducts data transfers
                int reply = ftp.getReplyCode();
                if(!FTPReply.isPositiveCompletion(reply)) {
                    if (ftp != null) try {ftp.disconnect();} catch (Throwable ee) {}
                    throw new IOException("bad connection to ftp server: " + reply);
                }
                if (!ftp.login(FTPStorageFactory.this.username, FTPStorageFactory.this.password)) {
                    if (ftp != null) try {ftp.disconnect();} catch (Throwable ee) {}
                    throw new IOException("login failure");
                }
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.setBufferSize(8192);
                return ftp;
            }
            
            @Override
            public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
                long t0 = System.currentTimeMillis();
                FTPClient ftp = initConnection();
                try {
                    long t1 = System.currentTimeMillis();
                    String file = cdPath(ftp, path);
                    long t2 = System.currentTimeMillis();
                    ftp.enterLocalPassiveMode();
                    boolean success = ftp.storeFile(file, new ByteArrayInputStream(asset));
                    long t3 = System.currentTimeMillis();
                    Data.logger.debug("ftp store successfull: check connection = " + (t1 - t0) + ", cdPath = " + (t2 - t1) + ", store = " + (t3 - t2));
                    if (!success) throw new IOException("storage to path " + path + " was not successful");
                } catch (IOException e) {
                    throw e;
                } finally {
                    if (ftp != null) try {ftp.disconnect();} catch (Throwable ee) {}
                }
                return FTPStorageFactory.this;
            }

            @Override
            public Asset<byte[]> load(String path) throws IOException {
                FTPClient ftp = initConnection();
                ByteArrayOutputStream baos = null;
                try {
                    String file = cdPath(ftp, path);
                    baos = new ByteArrayOutputStream();
                    ftp.retrieveFile(file, baos);
                } catch (IOException e) {
                    throw e;
                } finally {
                    if (ftp != null) try {ftp.disconnect();} catch (Throwable ee) {}
                }
                return new Asset<byte[]>(FTPStorageFactory.this, baos == null ? null : baos.toByteArray());
            }

            @Override
            public void close() {
            }
            
            private String cdPath(FTPClient ftp, String path) throws IOException {
                int success_code = ftp.cwd("/");
                if (success_code >= 300) throw new IOException("cannot cd into " + path + ": " + success_code);
                if (path.length() == 0 || path.equals("/")) return "";
                if (path.charAt(0) == '/') path = path.substring(1); // we consider that all paths are absolute to / (home)
                int p;
                while ((p = path.indexOf('/')) > 0) {
                    String dir = path.substring(0, p);
                    int code = ftp.cwd(dir);
                    if (code >= 300) {
                        // path may not exist, try to create the path
                        boolean success = ftp.makeDirectory(dir);
                        if (!success) throw new IOException("unable to create directory " + dir + " for path " + path);
                        code = ftp.cwd(dir);
                        if (code >= 300) throw new IOException("unable to cwd into directory " + dir + " for path " + path);
                    }
                    path = path.substring(p + 1);
                }
                return path;
            }
        };
    }
    
    @Override
    public String getConnectionURL() {
        return "ftp://" +
                (this.username != null && this.username.length() > 0 ? username + (this.password != null && this.password.length() > 0 ? ":" + this.password : "") + "@" : "") +
                this.getHost() + ((this.hasDefaultPort() ? "" : ":" + this.getPort()));
    }

    @Override
    public String getHost() {
        return this.server;
    }

    @Override
    public boolean hasDefaultPort() {
        return this.port == -1 || this.port == DEFAULT_PORT;
    }

    @Override
    public int getPort() {
        return hasDefaultPort() ? DEFAULT_PORT : this.port;
    }

    @Override
    public Storage<byte[]> getStorage() throws IOException {
        return this.ftpClient;
    }

    @Override
    public void close() {
        this.ftpClient.close();
    }

}
