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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.yacy.grid.mcp.Data;

public class FilesystemStorageFactory implements StorageFactory<byte[]> {

    private final Storage<byte[]> storage;
    private boolean deleteafterread;
    
    FilesystemStorageFactory(File basePath, boolean deleteafterread) {
        this.deleteafterread = deleteafterread;
        this.storage = new Storage<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                // do nothing
            }
            
            @Override
            public StorageFactory<byte[]> store(String path, byte[] asset) throws IOException {
                File f = new File(basePath, path);
                File f1 = new File(basePath, path + ".bkp");
                if (f1.exists()) f1.delete();
                if (f.exists()) f.renameTo(f1);
                f.getParentFile().mkdirs();
                try {
                    Files.write(f.toPath(), asset);
                } catch (IOException e) {
                    if (f1.exists()) f1.renameTo(f);
                    throw e;
                }
                if (f1.exists()) f1.delete();
                return FilesystemStorageFactory.this;
            }

            @Override
            public Asset<byte[]> load(String path) throws IOException {
                File f = new File(basePath, path);
                if (!f.exists()) throw new IOException("asset " + path + " does not exist");
                byte[] b = Files.readAllBytes(f.toPath());
                if (FilesystemStorageFactory.this.deleteafterread) try {
                    f.delete();
                    File parent = f.getParentFile();
                    if (parent.list().length == 0) parent.delete();
                } catch (Throwable e) {
                    Data.logger.warn("FileSystemStorageFactory.load ", e);
                }
                return new Asset<byte[]>(FilesystemStorageFactory.this, b);
            }

            @Override
            public void close() {
                // do nothing
            }
            
        };
    }

    @Override
    public String getSystem() {
        return "file";
    }
    
    @Override
    public String getHost() {
        return null;
    }

    @Override
    public boolean hasDefaultPort() {
        return true;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getConnectionURL() {
        return null;
    }

    @Override
    public Storage<byte[]> getStorage() throws IOException {
        return this.storage;
    }

    @Override
    public void close() {
        this.storage.close();
    }

}
