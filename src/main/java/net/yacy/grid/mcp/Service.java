/**
 *  Service
 *  Copyright 16.01.2017 by Michael Peter Christen, @orbiterlab
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

package net.yacy.grid.mcp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import net.yacy.grid.http.APIHandler;
import net.yacy.grid.http.FileHandler;
import net.yacy.grid.tools.CronBox;
import net.yacy.grid.tools.CronBox.Telemetry;
import net.yacy.grid.tools.Logger;

/**
 * The services enum contains information about all services implemented in this
 * grid element. It is the name of the servlets and their result object set.
 * This enum class should be replaced with a Swagger definition.
 */
public class Service {

    public static Service instance = null;

    private final static String[] servicesList = new String[]{
            "network",     // the name of the network
            "service",     // the YaCy Grid type of the service
            "name",        // an identifier which the owner of the service can assign as they want
            "host",        // host-name or IP address of the service
            "port",        // port on on the server where the service is running
            "publichost",  // host name which the service names as public access point, may be different from the recognized host access (backward routing may be different)
            "lastseen",    // ISO 8601 Time of the latest contact of the mcp to the service. Empty if the service has never been seen.
            "lastping"     // ISO 8601 Time of the latest contact of the service to the mcp
    };

    private final Set<String> fields;
    private int port = 0;
    public Configuration config;
    private final Server server;

    public Service(final Configuration config) {

        this.config = config;
        this.fields = new LinkedHashSet<String>();
        for (final String field: servicesList) this.fields.add(field);

        // overwrite the config with environment variables. Because a '.' (dot) is not allowed in system environments
        // the dot can be replaced by "_" (underscore), i.e. like:
        // grid_broker_address="anonymous:yacy@127.0.0.1:5672" java -jar build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT.jar
        final String[] keys = this.config.properties.keySet().toArray(new String[this.config.properties.size()]); // create a clone of the keys to prevent a ConcurrentModificationException
        for (final String key: keys) if (System.getenv().containsKey(key.replace('.', '_'))) this.config.properties.put(key, System.getenv().get(key.replace('.', '_')));

        // the config can further be overwritten by System Properties, i.e. like:
        // java -jar -Dgrid.broker.address="anonymous:yacy@127.0.0.1:5672" build/libs/yacy_grid_mcp-0.0.1-SNAPSHOT.jar
        for (final String key: keys) if (System.getProperties().containsKey(key)) this.config.properties.put(key, System.getProperties().getProperty(key));

        // start server
        final QueuedThreadPool pool = new QueuedThreadPool();
        pool.setMaxThreads(100);
        this.server = new Server(pool);

        // define one single static variable that is the genesis object for all other stored
        // in the application.
        instance = this;
    }

    public int getServerThreads() {
        return this.server == null ? 0 : this.server.getThreadPool().getThreads() - this.server.getThreadPool().getIdleThreads();
    }

    public int open(final int port, final String htmlPath, final boolean force) throws IOException {
        int ap = 0;
        while (true) {
            try {
                open(port + ap, htmlPath);
                return port + ap;
            } catch (final IOException e) {
                if (force || ap >= 16) {
                    Logger.warn(e);
                    throw e;
                }
                ap++;
                continue;
            }
        }
    }

    private void open(final int port, final String htmlPath) throws IOException {
        try {
            final ServerConnector connector = new ServerConnector(this.server);
            final HttpConfiguration http_config = new HttpConfiguration();
            http_config.setRequestHeaderSize(65536);
            http_config.setResponseHeaderSize(65536);
            connector.addConnectionFactory(new HttpConnectionFactory(http_config));
            connector.setPort(port);
            connector.setName("httpd:" + port);
            connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
            this.server.addConnector(connector);
            this.server.setStopAtShutdown(true);

            // add services
            final ServletContextHandler servletHandler = new ServletContextHandler();
            for (final Class<? extends Servlet> servlet: this.config.servlets)
                try {
                    final APIHandler handler = (APIHandler) (servlet.getConstructor().newInstance());
                    servletHandler.addServlet(servlet, handler.getAPIPath());
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    Logger.warn(servlet.getName() + " instantiation error", e);
                }

            final ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.setShowStacks(true);
            servletHandler.setErrorHandler(errorHandler);

            final HandlerList handlerlist2 = new HandlerList();
            if (htmlPath == null) {
                handlerlist2.setHandlers(new Handler[]{servletHandler, new DefaultHandler()});
            } else {
                final FileHandler fileHandler = new FileHandler();
                fileHandler.setDirectoriesListed(true);
                fileHandler.setWelcomeFiles(new String[]{"index.html"});
                fileHandler.setResourceBase(htmlPath);
                handlerlist2.setHandlers(new Handler[]{fileHandler, servletHandler, new DefaultHandler()});
            }
            this.server.setHandler(handlerlist2);
            this.server.start();
        } catch (final Throwable e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e.getMessage(), e);
        }
    }

    public boolean isAlive() {
        return this.server.isRunning() || this.server.isStarted() || this.server.isStarting();
    }

    public void stop() {
        try {
            if (!this.server.isStopped()) {
                this.server.stop();
            }
        } catch (final Exception e) {
            Logger.warn(e);
        }
    }

    public void join() {
        try {
            this.server.getThreadPool().join();
        } catch (final Exception e) {
            Logger.warn( e);
        }
    }

    public void close() {
        this.server.destroy();
        instance = null;
    }

    public int getPort() {
        return this.port == 0 ? Integer.parseInt(this.config.properties.get("port")) : this.port;
    }

    public RESTServer newServer(final String html_path) {
        return new RESTServer(html_path);
    }

    public class RESTServer implements CronBox.Application {

        private final String html_path;

        public RESTServer(final String html_path) {
            this.html_path = html_path;
        }

        @Override
        public void run() {

            // read the port again and then read also the configuration again because the path of the custom settings may have moved
            Service.this.port = Integer.parseInt(Service.this.config.properties.get("port"));

            // start server
            try {
                // open the server on available port
                final boolean portForce = Boolean.getBoolean(Service.this.config.properties.get("port.force"));
                Service.this.port = Service.this.open(Service.this.port, this.html_path, portForce);

                // give positive feedback
                Logger.info("Service started at port " + Service.this.port);

                // prepare shutdown signal
                boolean pidkillfileCreated = false;
                // we use two files: one kill file which can be used to stop the process and one pid file which exists until the process runs
                // in case that the deletion of the kill file does not cause a termination, still a "fuser -k" on the pid file can be used to
                // terminate the process.
                final File pidfile = new File(Service.this.config.data_dir, Service.this.config.type.name() + "-" + Service.this.port + ".pid");
                final File killfile = new File(Service.this.config.data_dir, Service.this.config.type.name() + "-" + Service.this.port + ".kill");
                if (pidfile.exists()) pidfile.delete();
                if (killfile.exists()) killfile.delete();
                if (!pidfile.exists()) try {
                    pidfile.createNewFile();
                    if (pidfile.exists()) {pidfile.deleteOnExit(); pidkillfileCreated = true;}
                } catch (final IOException e) {
                    Logger.info("pid file " + pidfile.getAbsolutePath() + " creation failed: " + e.getMessage());
                }
                if (!killfile.exists()) try {
                    killfile.createNewFile();
                    if (killfile.exists()) killfile.deleteOnExit(); else pidkillfileCreated = false;
                } catch (final IOException e) {
                    Logger.info("kill file " + killfile.getAbsolutePath() + " creation failed: " + e.getMessage());
                    pidkillfileCreated = false;
                }

                // wait for shutdown signal (kill on process)
                if (pidkillfileCreated) {
                    // we can control this by deletion of the kill file
                    Logger.info("to stop this process, delete kill file " + killfile.getAbsolutePath());
                    while (Service.this.isAlive() && killfile.exists()) {
                        try {Thread.sleep(1000);} catch (final InterruptedException e) {}
                    }
                    Service.this.stop();
                } else {
                    // something with the pid file creation did not work; fail-over to normal operation waiting for a kill command
                    Service.this.join();
                }
                Logger.info("server nominal termination requested");
            } catch (final IOException e) {
                Logger.error("Main fail", e);
            } finally {
                Service.this.stop();
                Logger.info("server terminated.");
            }
        }

        @Override
        public void stop() {
            Service.this.stop();
            Service.this.join();
        }

        @Override
        public Telemetry getTelemetry() {
            return null;
        }
    }
}
