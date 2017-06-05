package net.yacy.grid.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.mind.SusiAction;
import ai.susi.mind.SusiThought;
import net.yacy.grid.YaCyServices;
import net.yacy.grid.io.messages.MessageContainer;

public abstract class AbstractBrokerListener implements BrokerListener {
    
    public boolean shallRun = true;

    public abstract boolean processAction(SusiAction a);
    
    @Override
    public void run() {
        while (shallRun) {
            if (Data.gridBroker == null) {
                try {Thread.sleep(1000);} catch (InterruptedException ee) {}
            } else try {
                MessageContainer<byte[]> mc = Data.gridBroker.receive(YaCyServices.parser.name(), YaCyServices.parser.getDefaultQueue(), 10000);
                if (mc == null || mc.getPayload() == null) continue;
                JSONObject json = new JSONObject(new JSONTokener(new String(mc.getPayload(), StandardCharsets.UTF_8)));
                SusiThought process = new SusiThought(json);
                List<SusiAction> actions = process.getActions();
                actionloop: for (int ac = 0; ac < actions.size(); ac++) {
                    SusiAction a = actions.get(ac);
                    String type = a.getStringAttr("type");
                    String queue = a.getStringAttr("queue");
                    if (type == null || type.length() == 0 || queue == null || queue.length() == 0) {
                        Data.logger.info("bad message in queue, continue");
                        continue actionloop;
                    }
                    if (!type.equals(YaCyServices.parser.name())) {
                        Data.logger.info("wrong message in queue: " + type + ", continue");
                        try {
                            loadNextAction(a, process.getData()); // put that into the correct queue
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        continue actionloop;
                    }

                    boolean processed = processAction(a);
                    if (processed) {
                        // send next embedded action(s) to queue
                        JSONArray embeddedActions = a.toJSONClone().getJSONArray("actions");
                        for (int j = 0; j < embeddedActions.length(); j++) {
                            loadNextAction(new SusiAction(embeddedActions.getJSONObject(j)), process.getData());
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
    
    public void terminate() {
        this.shallRun = false;
    }
    
}