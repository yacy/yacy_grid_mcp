/**
 *  S3QueueFactory
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

public class S3QueueFactory extends PersistentCord implements QueueFactory {

    private URL url;
    private final String endpointURL;
    private final IOPath iop;
    private GenericIO io;

    public S3QueueFactory(final GenericIO io, final IOPath iop) {
        super(io, iop);
        this.endpointURL = (io instanceof S3IO) ? ((S3IO) io).getEndpointURL() : null;
        try {
            this.url = new URL(this.endpointURL);
        } catch (final MalformedURLException e) {
            this.url = null;
        }
        try {
            this.io = (io instanceof S3IO) ? new S3IO(this.endpointURL, ((S3IO) io).getAccessKey(), ((S3IO) io).getSecretKey()) : new FileIO(new File("data"));
        } catch (final IOException e) {
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

    private static JSONObject message2json(final byte[] message) {
        if (message == null) return new JSONObject();
        if (message[0] == '{') {
            // consider that this is already json
            return new JSONObject(new JSONTokener(new String(message, StandardCharsets.UTF_8)));
        }
        final JSONObject json = new JSONObject();
        json.put("message", Base64.getEncoder().encodeToString(message));
        return json;
    }

    private static byte[] json2message(final JSONObject json) {
        if (json.has("message")) {
            final String message = json.optString("message", "");
            if (message.length() > 0) {
                final byte[] m = Base64.getDecoder().decode(message);
                return m;
            }
        }
        return json.toString(0).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Queue getQueue(final String queueName) throws IOException {
        return new Queue() {

            @Override
            public void checkConnection() throws IOException {
                if (!S3QueueFactory.this.io.bucketExists(S3QueueFactory.this.iop.getBucket()))
                    throw new IOException("bucket " + S3QueueFactory.this.iop.getBucket() + " does not exist");
            }

            @Override
            public Queue send(final byte[] message) throws IOException {
                S3QueueFactory.this.append(message2json(message));
                return this;
            }

            @Override
            public MessageContainer receive(final long timeout, final boolean autoAck) throws IOException {
                final JSONObject json = S3QueueFactory.this.getFirst();
                return new MessageContainer(S3QueueFactory.this, json2message(json), 0 /* delivery tag */);
            }

            @Override
            public void acknowledge(final long deliveryTag) throws IOException {
                // do nothing, this class does not provide a message acknowledge function
            }

            @Override
            public void reject(final long deliveryTag) throws IOException {
                // do nothing, this class does not provide a message reject function
            }

            @Override
            public void recover() throws IOException {
                // do nothing, this class does not provide a message recover function
            }

            @Override
            public long available() throws IOException {
                return S3QueueFactory.this.size();
            }

            @Override
            public void clear() throws IOException {
                // do nothing, this class does not provide a clear function
            }

            @Override
            public void close() throws IOException {
                // nothing to close
            }

        };
    }

    @Override
    public void close() {
        this.close();
    }

}
