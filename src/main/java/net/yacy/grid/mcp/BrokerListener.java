/**
 *  BrokerListener
 *  Copyright 1.06.2017 by Michael Peter Christen, @orbiterlab
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

import org.json.JSONArray;

import ai.susi.mind.SusiAction;
import net.yacy.grid.tools.CronBox;

/**
 * Interface for the Broker Listener
 * A Broker Listener is a class which processes actions that are placed on a broker queue
 */
public interface BrokerListener extends CronBox.Application {

    public enum ActionResult {
        SUCCESS,              // the action was performed with success AND it is wanted that embedded actions from another "actions" object is executed. If this is true, the process is not executed in the same thread, instead, the process is pushed to the broker to be executed by another thread
        FAIL_RETRY,           // the action was not performed with success but it might be successfull at a later time
        FAIL_IRREVERSIBLE;    // the action was not successfull and should bot be tried again
    }

    /**
     * Process an action from the broker
     * @param action an action
     * @param data additional data that the process can use
     * @param processName a name for the process
     * @param processNumber a number of the process within the given name space
     * @return ActionResult
     */
    public ActionResult processAction(SusiAction action, JSONArray data, String processName, int processNumber);

    /**
     * calculate the number of messages that the broker listener processes
     * @return message per minute
     */
    public int messagesPerMinute();

}
