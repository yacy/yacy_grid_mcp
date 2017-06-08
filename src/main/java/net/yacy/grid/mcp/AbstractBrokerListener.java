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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiThought;
import net.yacy.grid.io.messages.MessageContainer;

public abstract class AbstractBrokerListener implements BrokerListener {
    
    public boolean shallRun = true;

    public abstract boolean processAction(SusiAction action, JSONArray data);
    
    @Override
    public void run() {
        while (shallRun) {
            if (Data.gridBroker == null || Service.type == null) {
                try {Thread.sleep(1000);} catch (InterruptedException ee) {}
            } else try {
                MessageContainer<byte[]> mc = Data.gridBroker.receive(Service.type.name(), Service.type.getDefaultQueue(), 10000);
                if (mc == null || mc.getPayload() == null) continue;
                JSONObject json = new JSONObject(new JSONTokener(new String(mc.getPayload(), StandardCharsets.UTF_8)));
                final SusiThought process = new SusiThought(json);
                final JSONArray data = process.getData();
                final List<SusiAction> actions = process.getActions();
                
                // loop though all actions
                actionloop: for (int ac = 0; ac < actions.size(); ac++) {
                    SusiAction a = actions.get(ac);
                    String type = a.getStringAttr("type");
                    String queue = a.getStringAttr("queue");

                    // check if the credentials to execute the queue are valid
                    if (type == null || type.length() == 0 || queue == null || queue.length() == 0) {
                        Data.logger.info("bad message in queue, continue");
                        continue actionloop;
                    }
                    
                    // check if this is the correct queue
                    if (!type.equals(Service.type.name())) {
                        Data.logger.info("wrong message in queue: " + type + ", continue");
                        try {
                            loadNextAction(a, process.getData()); // put that into the correct queue
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        continue actionloop;
                    }

                    // process the action
                    boolean processed = processAction(a, data);
                    if (processed) {
                        // send next embedded action(s) to queue
                        JSONObject ao = a.toJSONClone();
                        if (ao.has("actions")) {
                            JSONArray embeddedActions = ao.getJSONArray("actions");
                            for (int j = 0; j < embeddedActions.length(); j++) {
                                loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), process.getData());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {Thread.sleep(1000);} catch (InterruptedException ee) {}
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
        byte[] b = nextProcess.toString().getBytes(StandardCharsets.UTF_8);
        Data.gridBroker.send(type, queue, b);
    }

    @Override
    public void terminate() {
        this.shallRun = false;
    }
    
}