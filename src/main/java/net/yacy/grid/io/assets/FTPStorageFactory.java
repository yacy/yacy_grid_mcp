/**
 *  FilesystemStorageFactory
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import net.yacy.grid.mcp.Data;
import net.yacy.grid.mcp.MCP;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.tools.Logger;

public class FTPStorageFactory implements StorageFactory<byte[]> {

    private static int DEFAULT_PORT = 21;

    private String server, username, password;
    private int port;
    private Storage<byte[]> ftpClient;
    private boolean deleteafterread, active;

    public FTPStorageFactory(String server, int port, String username, String password, boolean deleteafterread, boolean active) throws IOException {
        this.server = server;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.port = port;
        this.deleteafterread = deleteafterread;
        this.active = active;

        this.ftpClient = new Storage<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                if (this.initConnection() == null) throw new IOException("FTPClient is NULL");
            }

            private FTPClient initConnection() throws IOException {
                final FTPClient ftp = new FTPClient();
                ftp.setDataTimeout(3000);
                ftp.setConnectTimeout(20000);
                if (FTPStorageFactory.this.port < 0 || FTPStorageFactory.this.port == DEFAULT_PORT) {
                    ftp.connect(FTPStorageFactory.this.server);
                } else {
                    ftp.connect(FTPStorageFactory.this.server, FTPStorageFactory.this.port);
                }
                if (FTPStorageFactory.this.active)
                	ftp.enterLocalActiveMode(); // The data transfer process establishes the data connection
                else
                	ftp.enterLocalPassiveMode(); // The server opens a data port to which the client conducts data transfers
                final int reply = ftp.getReplyCode();
                if(!FTPReply.isPositiveCompletion(reply)) {
                    if (ftp != null) try {ftp.disconnect();} catch (final Throwable ee) {}
                    throw new IOException("bad connection to ftp server: " + reply);
                }
                if (!ftp.login(FTPStorageFactory.this.username, FTPStorageFactory.this.password)) {
                    if (ftp != null) try {ftp.disconnect();} catch (final Throwable ee) {}
                    throw new IOException("login failure");
                }
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.setBufferSize(8192);
                return ftp;
            }

            @Override
            public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
                final long t0 = System.currentTimeMillis();
                final FTPClient ftp = this.initConnection();
                try {
                    final long t1 = System.currentTimeMillis();
                    final String file = this.cdPath(ftp, path);
                    final long t2 = System.currentTimeMillis();
                    if (FTPStorageFactory.this.active)
                    	ftp.enterLocalActiveMode(); // The data transfer process establishes the data connection
                    else
                    	ftp.enterLocalPassiveMode(); // The server opens a data port to which the client conducts data transfers
                    final boolean success = ftp.storeFile(file, new ByteArrayInputStream(asset));
                    final long t3 = System.currentTimeMillis();
                    if (!success) throw new IOException("storage to path " + path + " was not successful (storeFile=false)");
                    Logger.debug(this.getClass(), "FTPStorageFactory.store ftp store successfull: check connection = " + (t1 - t0) + ", cdPath = " + (t2 - t1) + ", store = " + (t3 - t2));
                } catch (final IOException e) {
                    throw e;
                } finally {
                    if (ftp != null) try {ftp.disconnect();} catch (final Throwable ee) {}
                }
                return FTPStorageFactory.this;
            }

            @Override
            public Asset<byte[]> load(String path) throws IOException {
                final FTPClient ftp = this.initConnection();
                ByteArrayOutputStream baos = null;
                byte[] b = null;
                try {
                    final String file = this.cdPath(ftp, path);
                    baos = new ByteArrayOutputStream();
                    if (FTPStorageFactory.this.active)
                    	ftp.enterLocalActiveMode(); // The data transfer process establishes the data connection
                    else
                    	ftp.enterLocalPassiveMode(); // The server opens a data port to which the client conducts data transfers
                    ftp.retrieveFile(file, baos);
                    b = baos.toByteArray();
                    if (FTPStorageFactory.this.deleteafterread) try {
                        final boolean deleted = ftp.deleteFile(file);
                        final FTPFile[] remaining = ftp.listFiles();
                        if (remaining.length == 0) {
                            ftp.cwd("/");
                            if (path.startsWith("/")) path = path.substring(1);
                            final int p = path.indexOf('/');
                            if (p > 0) path = path.substring(0, p);
                            ftp.removeDirectory(path);
                        }
                    } catch (final Throwable e) {
                        Logger.warn(this.getClass(), "FTPStorageFactory.load failed to remove asset " + path, e );
                    }
                } catch (final IOException e) {
                    throw e;
                } finally {
                    if (ftp != null) try {ftp.disconnect();} catch (final Throwable ee) {}
                }
                return new Asset<>(FTPStorageFactory.this, b);
            }

            @Override
            public void close() {
            }

            private String cdPath(FTPClient ftp, String path) throws IOException {
                final int success_code = ftp.cwd("/");
                if (success_code >= 300) throw new IOException("cannot cd into " + path + ": " + success_code);
                if (path.length() == 0 || path.equals("/")) return "";
                if (path.charAt(0) == '/') path = path.substring(1); // we consider that all paths are absolute to / (home)
                int p;
                while ((p = path.indexOf('/')) > 0) {
                    final String dir = path.substring(0, p);
                    int code = ftp.cwd(dir);
                    if (code >= 300) {
                        // path may not exist, try to create the path
                        final boolean success = ftp.makeDirectory(dir);
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
    public String getSystem() {
        return "ftp";
    }

    @Override
    public String getConnectionURL() {
        return "ftp://" +
                (this.username != null && this.username.length() > 0 ? this.username + (this.password != null && this.password.length() > 0 ? ":" + this.password : "") + "@" : "") +
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
        return this.hasDefaultPort() ? DEFAULT_PORT : this.port;
    }

    @Override
    public Storage<byte[]> getStorage() throws IOException {
        return this.ftpClient;
    }

    @Override
    public void close() {
        this.ftpClient.close();
    }

    public static void main(String[] args) {
        try {
            final List<Class<? extends Servlet>> services = new ArrayList<>(Arrays.asList(MCP.MCP_SERVICES));
            Service.initEnvironment(MCP.MCP_SERVICE, services, MCP.DATA_PATH, true);

            //Data.init(new File("data"), new HashMap<String, String>(), true);
            final FTPStorageFactory ftpc = new FTPStorageFactory("brain.local", 2121, "admin", "admin", true, true);
            final Storage<byte[]> storage = ftpc.getStorage();
            final String path = "test/file";
            final String data = "123";
            storage.store(path, data.getBytes());
            final Asset<byte[]> b = storage.load(path);
            System.out.println(new String(b.getPayload()));
            Data.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
