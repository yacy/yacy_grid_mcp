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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiThought;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.messages.MessageContainer;

public abstract class AbstractBrokerListener implements BrokerListener {
    
    public boolean shallRun;
    private final String serviceName;
    private final String queueName;
    private final int threads;
    private final ThreadPoolExecutor threadPool;

    public AbstractBrokerListener(final YaCyServices service, final int threads) {
    	this(service.name(), service.getDefaultQueue(), threads);
    }
    
    public AbstractBrokerListener(final String serviceName, final String queueName, final int threads) {
    	this.serviceName = serviceName;
    	this.queueName = queueName;
    	this.threads = threads;
    	this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.threads);
    	this.shallRun = true;
    }

    public abstract boolean processAction(SusiAction action, JSONArray data);
    
    @Override
    public void run() {
        runloop: while (shallRun) {
        	String payload = "";
            if (Data.gridBroker == null) {
                try {Thread.sleep(1000);} catch (InterruptedException ee) {}
            } else try {
            	// wait until an execution thread is available
            	while (this.threadPool.getActiveCount() >= this.threads)
					try {Thread.sleep(100);} catch (InterruptedException e1) {}
            	
            	// wait until message arrives
                MessageContainer<byte[]> mc = Data.gridBroker.receive(this.serviceName, this.queueName, 10000);
                if (mc == null || mc.getPayload() == null) continue runloop;
                payload = new String(mc.getPayload(), StandardCharsets.UTF_8);
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
                    if (!type.equals(this.serviceName)) {
                        Data.logger.info("wrong message in queue: " + type + ", continue");
                        try {
                            loadNextAction(action, process.getData()); // put that into the correct queue
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        continue actionloop;
                    }

                    // process the action using the previously acquired execution thread
                    this.threadPool.execute(new ActionProcess(action, data));
                }
            } catch (JSONException e) {
                // happens if the payload has a wrong form
                Data.logger.info("message syntax error with '" + payload + "' in queue: " + e.getMessage(), e);
                continue runloop;
            } catch (IOException e) {
                Data.logger.info("IOException: " + e.getMessage(), e);
                try {Thread.sleep(1000);} catch (InterruptedException ee) {}
                continue runloop;
            } catch (Throwable e) {
                Data.logger.info("error: " + e.getMessage(), e);
                continue runloop;
            }
        }
    }
    
    private final class ActionProcess implements Runnable {
    	
    	private final SusiAction action;
    	private final JSONArray data;
    	
    	public ActionProcess(SusiAction action, JSONArray data) {
    		this.action = action;
    		this.data = data;
    	}
		
    	@Override
		public void run() {
			boolean processed = processAction(action, data);
	        if (processed) {
	            // send next embedded action(s) to queue
	            JSONObject ao = action.toJSONClone();
	            if (ao.has("actions")) {
	                JSONArray embeddedActions = ao.getJSONArray("actions");
	                for (int j = 0; j < embeddedActions.length(); j++) {
	                    try {
							loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), data);
						} catch (UnsupportedOperationException | JSONException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
							// do a re-try
							try {Thread.sleep(10000);} catch (InterruptedException e1) {}
							try {
								loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), data);
							} catch (UnsupportedOperationException | JSONException | IOException ee) {
								e.printStackTrace();
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
        Data.gridBroker.send(type, queue, b);
    }

    @Override
    public void terminate() {
        this.shallRun = false;
    }
    
}