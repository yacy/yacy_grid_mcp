/**
 *  AbstractBrokerListener
 *  Copyright 1.06.2017 by Michael Peter Christen, @0rb1t3r
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiThought;
import net.yacy.grid.Services;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.messages.AvailableContainer;
import net.yacy.grid.io.messages.GridQueue;
import net.yacy.grid.io.messages.MessageContainer;
import net.yacy.grid.tools.Memory;

public abstract class AbstractBrokerListener implements BrokerListener {

    public boolean shallRun;
    private final Services service;
    private final GridQueue[] queueNames;
    private final int threads;
    //private final ThreadPoolExecutor threadPool;

    public AbstractBrokerListener(final Services service, final int threads) {
        this.service = service;
        this.queueNames = service.getQueues();
        this.threads = threads;
        //    this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threads);
        this.shallRun = true;
    }

    public abstract boolean processAction(SusiAction action, JSONArray data, String processName, int processNumber);

    @Override
    public void run() {
        // recover unacknowledged entries - possibly from last start
        for (GridQueue queue: this.queueNames) {
            try {
                Data.gridBroker.recover(AbstractBrokerListener.this.service, queue);
            } catch (IOException e) {
                Data.logger.fatal("Service " + this.service.name() + ": recover not possible: " + e.getMessage(), e);
            }
        }

        // print out some stats about the queues
        try {
            AvailableContainer[] ac = Data.gridBroker.available(AbstractBrokerListener.this.service, this.queueNames);
            for (int i = 0; i < ac.length; i++) {
                Data.logger.info("Service " + this.service.name() + ", queue " + ac[i].getQueue() + ": " + ac[i].getAvailable() + " entries.");
            }
        } catch (IOException e) {
            Data.logger.fatal("Service " + this.service.name() + ": AvailableContainer not available: " + e.getMessage(), e);
        }

        // start the listeners
        List<QueueListener> threads = new ArrayList<>();
        int threadsPerQueue = Math.max(1, this.threads / this.queueNames.length);
        Data.logger.info("Broker Listener: starting " + threadsPerQueue + " threads for each of the " + this.queueNames.length + " queues");
        for (GridQueue queue: this.queueNames) {
            for (int qc = 0; qc < threadsPerQueue; qc++) {
                QueueListener listener = new QueueListener(queue, qc, Data.gridBroker.isAutoAck());
                listener.start();
                threads.add(listener);
                Data.logger.info("Broker Listener for service " + this.service.name() + ", queue " + queue + " started thread " + qc);
            }
        }
        threads.forEach(thread -> {
            try {
                thread.join();
                Data.logger.info("Broker Listener for service " + this.service.name() + ", queue " + thread.queueName + " terminated");
            } catch (InterruptedException e) {
                Data.logger.info("Broker Listener for service " + this.service.name() + ", queue " + thread.queueName + " interrupted", e);
            }
        });
    }


    private class QueueListener extends Thread {
        private final GridQueue queueName;
        private final int threadCounter;
        private final boolean autoAck;

        public QueueListener(final GridQueue queueName, final int threadCounter, final boolean autoAck) {
            this.queueName = queueName;
            this.threadCounter = threadCounter;
            this.autoAck = autoAck;
        }

        @Override
        public void run() {
            try {
                AvailableContainer a = Data.gridBroker.available(AbstractBrokerListener.this.service, this.queueName);
                Data.logger.info("Started QueueListener for Queue " + a.getQueue() + ", thread " + this.threadCounter + ": " + a.getAvailable() + " entries.");
            } catch (IOException e) {
                Data.logger.fatal("Could not load AvailableContainer for Queue " + queueName + ": " + e.getMessage(), e);
            }

            while (shallRun) {
                if (Data.gridBroker == null) {
                    try {Thread.sleep(1000);} catch (InterruptedException ee) {}
                    continue; // wait until initialization complete
                }
                String payload = "";
                MessageContainer<byte[]> mc = null;
                try {
                    // check short memory status
                    if (Memory.shortStatus()) {
                        Data.logger.info("AbstractBrokerListener.QueueListener short memory status: assigned = " + Memory.assigned() + ", used = " + Memory.used());
                        Data.clearCaches();
                    }

                    // wait until message arrives
                    mc = Data.gridBroker.receive(AbstractBrokerListener.this.service, this.queueName, 10000, autoAck);
                    if (mc != null && mc.getPayload() != null && mc.getPayload().length > 0) {
                        handleMessage(mc, this.queueName.name(), this.threadCounter);
                    }
                    // try {Thread.sleep(1000);} catch (InterruptedException ee) {}
                } catch (JSONException e) {
                    // happens if the payload has a wrong form
                    Data.logger.info("QueueListener: message syntax error with '" + payload + "' in queue: " + e.getMessage(), e);
                    try {Thread.sleep(10000);} catch (InterruptedException ee) {}
                } catch (Throwable e) {
                    Data.logger.info("QueueListener: " + e.getMessage(), e);
                    try {Thread.sleep(10000);} catch (InterruptedException ee) {}
                } finally {
                    if (!this.autoAck && mc != null && mc.getDeliveryTag() > 0) {
                        // acknowledge the message
                        try {
                            Data.gridBroker.acknowledge(AbstractBrokerListener.this.service, this.queueName, mc.getDeliveryTag());
                        } catch (IOException e) {
                            Data.logger.info("QueueListener: cannot acknowledge queue: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private void handleMessage(final MessageContainer<byte[]> mc, final String processName, final int processNumber) {
        String payload = new String(mc.getPayload(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(new JSONTokener(payload));
        final SusiThought process = new SusiThought(json);
        final JSONArray data = process.getData();
        final List<SusiAction> actions = process.getActions();

        // loop though all actions
        actionloop: for (int ac = 0; ac < actions.size(); ac++) {
            SusiAction action = actions.get(ac);
            String type = action.getStringAttr("type");
            String queue = action.getStringAttr("queue");

            // check if the credentials to execute the queue are valid
            if (type == null || type.length() == 0 || queue == null || queue.length() == 0) {
                Data.logger.info("bad message in queue, continue");
                continue actionloop;
            }

            // check if this is the correct queue
            if (!type.equals(this.service.name())) {
                Data.logger.info("wrong message in queue: " + type + ", continue");
                try {
                    loadNextAction(action, process.getData()); // put that into the correct queue
                } catch (Throwable e) {
                    Data.logger.warn("", e);
                }
                continue actionloop;
            }

            // process the action using the previously acquired execution thread
            //this.threadPool.execute(new ActionProcess(action, data));
            new ActionProcess(action, data, processName, processNumber).run(); // run, not start: we execute this in the current thread
        }
    }

    private final class ActionProcess implements Runnable {

        private final SusiAction action;
        private final JSONArray data;
        private final String processName;
        private final int processNumber;

        public ActionProcess(final SusiAction action, final JSONArray data, final String processName, final int processNumber) {
            this.action = action;
            this.data = data;
            this.processName = processName;
            this.processNumber = processNumber;
        }

        @Override
        public void run() {
            Thread.currentThread().setName(this.processName + "-" + this.processNumber + "-running");
            boolean processed = processAction(this.action, this.data, this.processName, this.processNumber);
            if (processed) {
                // send next embedded action(s) to queue
                JSONObject ao = action.toJSONClone();
                if (ao.has("actions")) {
                    JSONArray embeddedActions = ao.getJSONArray("actions");
                    for (int j = 0; j < embeddedActions.length(); j++) {
                        try {
                            loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), data);
                        } catch (UnsupportedOperationException | JSONException e) {
                            Data.logger.warn("", e);
                        } catch (IOException e) {
                            Data.logger.warn("", e);
                            // do a re-try
                            try {Thread.sleep(10000);} catch (InterruptedException e1) {}
                            try {
                                loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), data);
                            } catch (UnsupportedOperationException | JSONException | IOException ee) {
                                Data.logger.warn("", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadNextAction(SusiAction action, JSONArray data) throws UnsupportedOperationException, IOException {
        String type = action.getStringAttr("type");
        if (type == null || type.length() == 0) throw new UnsupportedOperationException("missing type in action");
        String queue = action.getStringAttr("queue");
        if (queue == null || queue.length() == 0) throw new UnsupportedOperationException("missing queue in action");

        // create a new Thought and push it to the next queue
        JSONObject nextProcess = new JSONObject()
                .put("data", data)
                .put("actions", new JSONArray().put(action.toJSONClone()));
        byte[] b = nextProcess.toString(2).getBytes(StandardCharsets.UTF_8);
        Data.gridBroker.send(YaCyServices.valueOf(type), new GridQueue(queue), b);
    }

    @Override
    public void terminate() {
        this.shallRun = false;
    }

}