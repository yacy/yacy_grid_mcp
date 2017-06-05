package net.yacy.grid.mcp;

import ai.susi.mind.SusiAction;

public interface BrokerListener extends Runnable {

    public boolean processAction(SusiAction a);

}
