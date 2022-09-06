/**
 *  ClientConnection
 *  Copyright 22.02.2015 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.yacy.grid.tools.Logger;

public class ClientConnection {

    public  static final String CHARSET = "UTF-8";
    private static final byte LF = 10;
    private static final byte CR = 13;
    public static final byte[] CRLF = {CR, LF};

    private static String userAgentDefault = ClientIdentification.browserAgent.userAgent;
    private final static CloseableHttpClient httpClient = getClosableHttpClient(userAgentDefault);

    public final static RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)           // time until a socket becomes available
            .setConnectionRequestTimeout(30000) // time until a request to the remote server is established
            .setSocketTimeout(3600000)          // time for the remote server to fulfill the request
            .setContentCompressionEnabled(true)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build();


    private static ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
            final HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                final HeaderElement he = it.nextElement();
                final String param = he.getName();
                final String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 60000;
        }
    };

    public final static CloseableHttpClient getClosableHttpClient(final String userAgent) {
        final HttpClientBuilder hcb = HttpClients.custom()
                .useSystemProperties()
                .setConnectionManager(getConnctionManager())
                .setMaxConnPerRoute(200)
                .setMaxConnTotal(Math.max(100, Runtime.getRuntime().availableProcessors() * 2))
                .setUserAgent(userAgent)
                .setDefaultRequestConfig(defaultRequestConfig)
                //.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                .setMaxConnTotal(500);
        return hcb.build();
    }

    private int status;
    public BufferedInputStream inputStream;
    private Map<String, List<String>> header;
    private final HttpRequestBase request;
    private HttpResponse httpResponse;
    private ContentType contentType;

    private static class TrustAllHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(final String hostname, final SSLSession session) {
            return true;
        }
    }

    /**
     * GET request
     * @param urlstring
     * @param useAuthentication
     * @throws IOException
     */
    public ClientConnection(final String urlstring) throws IOException {
        this.request = new HttpGet(urlstring);
        this.request.setHeader("User-Agent", ClientIdentification.getAgent(ClientIdentification.yacyInternetCrawlerAgentName).userAgent);
        this.init();
    }

    /**
     * POST request
     * @param urlstring
     * @param map
     * @param useAuthentication
     * @throws ClientProtocolException
     * @throws IOException
     */
    public ClientConnection(final String urlstring, final Map<String, byte[]> map, final boolean useAuthentication) throws ClientProtocolException, IOException {
        this.request = new HttpPost(urlstring);
        final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (final Map.Entry<String, byte[]> entry: map.entrySet()) {
            entityBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }
        ((HttpPost) this.request).setEntity(entityBuilder.build());
        this.request.setHeader("User-Agent", ClientIdentification.getAgent(ClientIdentification.yacyInternetCrawlerAgentName).userAgent);
        this.init();
    }

    /**
     * POST request
     * @param urlstring
     * @param map
     * @throws ClientProtocolException
     * @throws IOException
     */
    public ClientConnection(final String urlstring, final Map<String, byte[]> map) throws ClientProtocolException, IOException {
        this(urlstring, map, true);
    }

    /**
     * get a connection manager
     * @param trustAllCerts allow opportunistic encryption if needed
     * @return
     */
    public static PoolingHttpClientConnectionManager getConnctionManager() {

        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
        try {
            final SSLConnectionSocketFactory trustSelfSignedSocketFactory = new SSLConnectionSocketFactory(
                        new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                        new TrustAllHostNameVerifier());
            socketFactoryRegistry = RegistryBuilder
                    .<ConnectionSocketFactory> create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", trustSelfSignedSocketFactory)
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            Logger.warn(e);
        }

        final PoolingHttpClientConnectionManager cm = (socketFactoryRegistry != null) ?
                new PoolingHttpClientConnectionManager(socketFactoryRegistry):
                new PoolingHttpClientConnectionManager();

        // twitter specific options
        cm.setMaxTotal(2000);
        cm.setDefaultMaxPerRoute(200);

        return cm;
    }

    private void init() throws IOException {

        this.httpResponse = null;
        try {
            this.httpResponse = httpClient.execute(this.request);
        } catch (final UnknownHostException e) {
            this.request.releaseConnection();
            throw new IOException("client connection failed: unknown host " + this.request.getURI().getHost());
        } catch (final SocketTimeoutException e){
            this.request.releaseConnection();
            throw new IOException("client connection timeout for request: " + this.request.getURI());
        } catch (final SSLHandshakeException e){
            this.request.releaseConnection();
            throw new IOException("client connection handshake error for domain " + this.request.getURI().getHost() + ": " + e.getMessage());
        } catch (final HttpHostConnectException e) {
            this.request.releaseConnection();
            throw new IOException("client connection refused for request " + this.request.getURI() + ": " + e.getMessage());
        } catch (final Throwable e) {
            this.request.releaseConnection();
            throw new IOException("error " + this.request.getURI() + ": " + e.getMessage());
        }
        final HttpEntity httpEntity = this.httpResponse.getEntity();
        this.contentType = ContentType.get(httpEntity);
        if (httpEntity != null) {
            if (this.httpResponse.getStatusLine().getStatusCode() == 200) {
                try {
                    this.inputStream = new BufferedInputStream(httpEntity.getContent());
                } catch (final IOException e) {
                    this.request.releaseConnection();
                    throw e;
                }
                this.header = new HashMap<String, List<String>>();
                for (final Header header: this.httpResponse.getAllHeaders()) {
                    List<String> vals = this.header.get(header.getName());
                    if (vals == null) { vals = new ArrayList<String>(); this.header.put(header.getName(), vals); }
                    vals.add(header.getValue());
                }
            } else {
                this.request.releaseConnection();
                throw new IOException("client connection to " + this.request.getURI() + " fail: " + this.status + ": " + this.httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            this.request.releaseConnection();
            throw new IOException("client connection to " + this.request.getURI() + " fail: no connection");
        }
    }

    public ContentType getContentType() {
        return this.contentType == null ? ContentType.DEFAULT_BINARY : this.contentType;
    }

    /**
     * get a redirect for an url: this method shall be called if it is expected that a url
     * is redirected to another url. This method then discovers the redirect.
     * @param urlstring
     * @param useAuthentication
     * @return the redirect url for the given urlstring
     * @throws IOException if the url is not redirected
     */
    public static String getRedirect(final String urlstring) throws IOException {
        final HttpGet get = new HttpGet(urlstring);
        get.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        get.setHeader("User-Agent", ClientIdentification.getAgent(ClientIdentification.yacyInternetCrawlerAgentName).userAgent);
        final CloseableHttpClient httpClient = getClosableHttpClient(userAgentDefault);
        final HttpResponse httpResponse = httpClient.execute(get);
        final HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 301) {
                for (final Header header: httpResponse.getAllHeaders()) {
                    if (header.getName().equalsIgnoreCase("location")) {
                        EntityUtils.consumeQuietly(httpEntity);
                        return header.getValue();
                    }
                }
                EntityUtils.consumeQuietly(httpEntity);
                throw new IOException("redirect for  " + urlstring+ ": no location attribute found");
            } else {
                EntityUtils.consumeQuietly(httpEntity);
                throw new IOException("no redirect for  " + urlstring+ " fail: " + httpResponse.getStatusLine().getStatusCode() + ": " + httpResponse.getStatusLine().getReasonPhrase());
            }
        } else {
            throw new IOException("client connection to " + urlstring + " fail: no connection");
        }
    }

    public void close() {
        final HttpEntity httpEntity = this.httpResponse.getEntity();
        if (httpEntity != null) EntityUtils.consumeQuietly(httpEntity);
        try {
            this.inputStream.close();
        } catch (final IOException e) {} finally {
            this.request.releaseConnection();
        }
    }

    public static void download(final String source_url, final File target_file) {
        try {
            final ClientConnection connection = new ClientConnection(source_url);
            try {
                final OutputStream os = new BufferedOutputStream(new FileOutputStream(target_file));
                int count;
                final byte[] buffer = new byte[2048];
                try {
                    while ((count = connection.inputStream.read(buffer)) > 0) os.write(buffer, 0, count);
                } catch (final IOException e) {
                    Logger.warn(e.getMessage());
                } finally {
                    os.close();
                }
            } catch (final IOException e) {
                Logger.warn(e.getMessage());
            } finally {
                connection.close();
            }
        } catch (final IOException e) {
            Logger.warn(e.getMessage());
        }
    }

    public static void load(final String source_url, final File target_file) {
        download(source_url, target_file);
    }

    /**
     * make GET request
     * @param source_url
     * @return the response
     * @throws IOException
     */
    public static byte[] load(final String source_url) throws IOException {
        final ClientConnection connection = new ClientConnection(source_url);
        return connection.load();
    }

    /**
     * make POST request
     * @param source_url
     * @param post
     * @return the response
     * @throws IOException
     */
    public static byte[] load(final String source_url, final Map<String, byte[]> post) throws IOException {
        final ClientConnection connection = new ClientConnection(source_url, post);
        return connection.load();
    }

    public byte[] load() throws IOException {
        if (this.inputStream == null) return null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count;
        final byte[] buffer = new byte[2048];
        try {
            while ((count = this.inputStream.read(buffer)) > 0) baos.write(buffer, 0, count);
        } catch (final IOException e) {
            Logger.warn(this.getClass(), e.getMessage());
        } finally {
            this.close();
        }
        return baos.toByteArray();
    }

    public static JSONArray loadJSONArray(final String source_url) throws IOException {
        final byte[] b = load(source_url);
        return new JSONArray(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }
    public static JSONArray loadJSONArray(final String source_url, final Map<String, byte[]> params) throws IOException {
        final byte[] b = load(source_url, params);
        return new JSONArray(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }

    public static JSONObject loadJSONObject(final String source_url) throws IOException {
        final byte[] b = load(source_url);
        return new JSONObject(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }
    public static JSONObject loadJSONObject(final String source_url, final Map<String, byte[]> params) throws IOException {
        final byte[] b = load(source_url, params);
        return new JSONObject(new JSONTokener(new String(b, StandardCharsets.UTF_8)));
    }

    public static String loadFromEtherpad(final String etherpadUrlstub, final String etherpadApikey, final String padID) throws IOException {
        final String padurl = etherpadUrlstub + "/api/1/getText?apikey=" + etherpadApikey + "&padID=" + padID;
        final InputStream is = new URL(padurl).openStream();
        final JSONTokener serviceResponse = new JSONTokener(is);
        final JSONObject json = new JSONObject(serviceResponse);
        final String text = json.getJSONObject("data").getString("text");
        return text;
    }
}
