/**
 *  APIServer
 *  Copyright 14.01.2017 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * main server class as static class: we made this static on purpose because then it is always
 * known which class is the holder of the server class - no-one. That makes it possible to address
 * the server methods from everywhere in the code, even if we do not know which other classes may
 * open a server. We also never want to open more than one server port at once.
 */
public class APIServer {

   private static List<Class<? extends Servlet>> services = new ArrayList<>();
   private static Map<String, APIHandler> serviceMap = new ConcurrentHashMap<>();
   private static Server server = null;
    
    public static int getServerThreads() {
        return server.getThreadPool().getThreads() - server.getThreadPool().getIdleThreads();
    }
    
    public static void addService(Class<? extends Servlet> service) {
        try {
            APIHandler handler = (APIHandler) service.newInstance();
            services.add(service);
            serviceMap.put(handler.getAPIName(), handler);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    public static APIHandler getAPI(String name) {
        return serviceMap.get(name);
    }

    public static int open(int port, int expiresSeconds, String htmlPath, boolean force) throws IOException {
        while (true) {
            try {
                open(port, expiresSeconds, htmlPath);
                return port;
            } catch (IOException e) {
                if (force) throw e;
                port++;
                continue;
            }
        }
    }
    
    private static void open(int port, int expiresSeconds, String htmlPath) throws IOException {
        try {
            QueuedThreadPool pool = new QueuedThreadPool();
            pool.setMaxThreads(500);
            server = new Server(pool);
    
            ServerConnector connector = new ServerConnector(server);
            HttpConfiguration http_config = new HttpConfiguration();
            connector.addConnectionFactory(new HttpConnectionFactory(http_config));
            connector.setPort(port);
            connector.setName("httpd:" + port);
            connector.setIdleTimeout(20000); // timout in ms when no bytes send / received
            server.addConnector(connector);
            server.setStopAtShutdown(true);
    
            // add services
            ServletContextHandler servletHandler = new ServletContextHandler();
            for (Class<? extends Servlet> service: services)
                try {
                    servletHandler.addServlet(service, ((APIHandler) (service.getConstructor().newInstance())).getAPIPath());
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    Log.getLog().warn(service.getName() + " instantiation error", e);
                    e.printStackTrace();
                }
    
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.setShowStacks(true);
            servletHandler.setErrorHandler(errorHandler);

            HandlerList handlerlist2 = new HandlerList();
            if (htmlPath == null) {
                handlerlist2.setHandlers(new Handler[]{servletHandler, new DefaultHandler()});
            } else {
                FileHandler fileHandler = new FileHandler(expiresSeconds);
                fileHandler.setDirectoriesListed(true);
                fileHandler.setWelcomeFiles(new String[]{"index.html"});
                fileHandler.setResourceBase(htmlPath);
                handlerlist2.setHandlers(new Handler[]{fileHandler, servletHandler, new DefaultHandler()});
            }
            server.setHandler(handlerlist2);
            server.start();
        } catch (Throwable e) {
            throw new IOException(e.getMessage());
        }
    }
    
    public static void join() {
        try {
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
