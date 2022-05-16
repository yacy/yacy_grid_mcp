/**
 *  MCP
 *  Copyright 14.01.2017 by Michael Peter Christen, @orbiterlab
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

import javax.servlet.Servlet;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.tools.CronBox;
import net.yacy.grid.tools.CronBox.Telemetry;
import net.yacy.grid.tools.GitTool;
import net.yacy.grid.tools.Logger;

/**
 * The Master Connect Program
 *
 */
public class MCP {

    public final static YaCyServices MCP_SERVICE = YaCyServices.mcp;
    public final static YaCyServices INDEXER_SERVICE = YaCyServices.indexer;
    public final static String DATA_PATH = "data";

    // define services
    @SuppressWarnings("unchecked")
    public final static Class<? extends Servlet>[] MCP_SERVLETS = new Class[]{
            // information services
    		net.yacy.grid.mcp.api.info.ServicesService.class,
            net.yacy.grid.mcp.api.info.StatusService.class,
            net.yacy.grid.mcp.api.info.ThreaddumpService.class,
            net.yacy.grid.mcp.api.info.LogService.class,

            // control services
            net.yacy.grid.mcp.api.control.LoaderThrottlingService.class,

            // message services
            net.yacy.grid.mcp.api.messages.AcknowledgeService.class,
            net.yacy.grid.mcp.api.messages.AvailableService.class,
            net.yacy.grid.mcp.api.messages.ClearService.class,
            net.yacy.grid.mcp.api.messages.PeekService.class,
            net.yacy.grid.mcp.api.messages.QueuesService.class,
            net.yacy.grid.mcp.api.messages.ReceiveService.class,
            net.yacy.grid.mcp.api.messages.RecoverService.class,
            net.yacy.grid.mcp.api.messages.SendService.class,

            // asset services
            net.yacy.grid.mcp.api.assets.StoreService.class,
            net.yacy.grid.mcp.api.assets.LoadService.class,
            net.yacy.grid.mcp.api.assets.AssetsService.class,

            // admin services
            net.yacy.grid.mcp.api.admin.InquirySubmitService.class,

            // index services
            net.yacy.grid.mcp.api.index.YaCySearchService.class,
            net.yacy.grid.mcp.api.index.GSASearchService.class,
            net.yacy.grid.mcp.api.index.AddService.class,
            net.yacy.grid.mcp.api.index.CheckService.class,
            net.yacy.grid.mcp.api.index.CountService.class,
            net.yacy.grid.mcp.api.index.DeleteService.class,
            net.yacy.grid.mcp.api.index.ExistService.class,
            net.yacy.grid.mcp.api.index.QueryService.class
    };

    public static class Application implements CronBox.Application {

        final Configuration config;
        final Service service;
        final IndexListener brokerApplication;
        final CronBox.Application serviceApplication;

        public Application() {
            Logger.info("Starting MCP Application...");

            // initialize configuration
            this.config = new Configuration(DATA_PATH, true, MCP_SERVICE, MCP_SERVLETS);

            // initialize REST server with services
            this.service = new Service(this.config);

            // connect backend
            this.config.connectBackend();

            // initiate broker application: listening to indexing requests at RabbitMQ
            this.brokerApplication = new IndexListener(this.service.config, INDEXER_SERVICE);

            // initiate service application: listening to REST request
            this.serviceApplication = this.service.newServer(null);
        }

        @Override
        public void run() {

            Logger.info("Grid Name: " + this.brokerApplication.config.properties.get("grid.name"));

            // starting threads
            new Thread(this.brokerApplication).start();
            this.serviceApplication.run(); // SIC! the service application is running as the core element of this run() process. If we run it concurrently, this runnable will be "dead".
        }

        @Override
        public void stop() {
            Logger.info("Stopping MCP Application...");
            this.serviceApplication.stop();
            this.brokerApplication.stop();
            this.service.stop();
            this.service.close();
            this.config.close();
        }

        @Override
        public Telemetry getTelemetry() {
            return null;
        }

    }

    public static void main(final String[] args) {
        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // Debug Info
        boolean assertionenabled = false;
        assert (assertionenabled = true) == true; // compare to true to remove warning: "Possible accidental assignement"
        if (assertionenabled) Logger.info("Asserts are enabled");

        // first greeting
        Logger.info("YaCy Grid MCP started!");
        Logger.info(new GitTool().toString());
        Logger.info("you can now search using the query api, i.e.:");
        Logger.info("curl \"http://127.0.0.1:8100/yacy/grid/mcp/index/yacysearch.json?query=test\"");

        // run application with cron
        final long cycleDelay = Long.parseLong(System.getProperty("YACYGRID_MCP_CYCLEDELAY", "" + Long.MAX_VALUE)); // by default, run only in one genesis thread
        final int cycleRandom = Integer.parseInt(System.getProperty("YACYGRID_MCP_CYCLERANDOM", "" + 1000 * 60 /*1 minute*/));
        final CronBox cron = new CronBox(Application.class, cycleDelay, cycleRandom);
        cron.cycle();

        // this line is reached if the cron process was shut down
        Logger.info("YaCy Grid MCP terminated");
    }

}
