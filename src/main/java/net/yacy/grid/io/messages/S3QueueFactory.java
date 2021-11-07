/**
 *  CordQueueFactory
 *  Copyright 07.11.2021 by Michael Peter Christen, @oribterlab
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


package net.yacy.grid.io.messages;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.json.JSONTokener;

import eu.searchlab.storage.io.FileIO;
import eu.searchlab.storage.io.GenericIO;
import eu.searchlab.storage.io.IOPath;
import eu.searchlab.storage.io.S3IO;
import eu.searchlab.storage.json.PersistentCord;

public class CordQueueFactory extends PersistentCord implements QueueFactory<byte[]> {

    private URL url;
    private String endpointURL;
    private IOPath iop;
    private GenericIO io;

    public CordQueueFactory(GenericIO io, IOPath iop) {
        super(io, iop);
        this.endpointURL = (io instanceof S3IO) ? ((S3IO) io).getEndpointURL() : null;
        try {
            this.url = new URL(this.endpointURL);
        } catch (MalformedURLException e) {
            this.url = null;
        }
        try {
            this.io = (io instanceof S3IO) ? new S3IO(this.endpointURL, ((S3IO) io).getAccessKey(), ((S3IO) io).getSecretKey()) : new FileIO(new File("data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.iop = iop;
    }

    @Override
    public String getHost() {
        return this.url == null ? null : this.url.getHost();
    }

    @Override
    public boolean hasDefaultPort() {
        return this.url == null ? true : this.url.getPort() == 9000;
    }

    @Override
    public int getPort() {
        return this.url == null ? -1 : this.url.getPort();
    }

    @Override
    public String getConnectionURL() {
        return this.endpointURL;
    }

    private static JSONObject message2json(byte[] message) {
        if (message == null) return new JSONObject();
        if (message[0] == '{') {
            // consider that this is already json
            return new JSONObject(new JSONTokener(new String(message, StandardCharsets.UTF_8)));
        }
        JSONObject json = new JSONObject();
        json.put("message", Base64.getEncoder().encodeToString(message));
        return json;
    }

    private static byte[] json2message(JSONObject json) {
        if (json.has("message")) {
            String message = json.optString("message", "");
            if (message.length() > 0) {
                byte[] m = Base64.getDecoder().decode(message);
                return m;
            }
        }
        return json.toString(0).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Queue<byte[]> getQueue(String queueName) throws IOException {
        return new Queue<byte[]>() {

            @Override
            public void checkConnection() throws IOException {
                if (!CordQueueFactory.this.io.bucketExists(CordQueueFactory.this.iop.getBucket()))
                    throw new IOException("bucket " + CordQueueFactory.this.iop.getBucket() + " does not exist");
            }

            @Override
            public Queue<byte[]> send(byte[] message) throws IOException {
                CordQueueFactory.this.append(message2json(message));
                return this;
            }

            @Override
            public MessageContainer<byte[]> receive(long timeout, boolean autoAck) throws IOException {
                JSONObject json = CordQueueFactory.this.getFirst();
                return new MessageContainer<byte[]>(CordQueueFactory.this, json2message(json), 0 /* delivery tag */);
            }

            @Override
            public void acknowledge(long deliveryTag) throws IOException {
                // do nothing, this class does not provide a message acknowledge function
            }

            @Override
            public void reject(long deliveryTag) throws IOException {
                // do nothing, this class does not provide a message reject function
            }

            @Override
            public void recover() throws IOException {
                // do nothing, this class does not provide a message recover function
            }

            @Override
            public long available() throws IOException {
                return CordQueueFactory.this.size();
            }

            @Override
            public void clear() throws IOException {
                // do nothing, this class does not provide a clear function
            }

        };
    }

    @Override
    public void close() {
        this.close();
    }

}
