/**
 *  StackQueue
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

package net.yacy.grid.io.messages;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.yacy.grid.io.db.Stack;
import net.yacy.grid.mcp.Data;

/**
 * A (Message-) Queue based on a stack
 */
public class StackQueue<A> extends AbstractQueue<A> implements Queue<A> {
    
    private Stack<A> stack;
    private Semaphore semaphore;
    
    public StackQueue(Stack<A> backedStack) throws IOException {
        this.stack = backedStack;
        this.semaphore = new Semaphore(this.stack.size(), true);
    }

    @Override
    public void checkConnection() throws IOException {
        available();
    }
    
    @Override
    public Queue<A> send(A message) throws IOException {
        this.stack.push(message);
        this.semaphore.release();
        return this;
    }
    
    @Override
    public A receive(long timeout) throws IOException {
        try {
            if (timeout > 0) {
                if (!this.semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS)) return null;
            } else {
                this.semaphore.acquire();
            }
            return this.stack.pot();
        } catch (InterruptedException e) {
            Data.logger.debug("StackQueue: receive interrupted", e);
        }
        return null;
    }

    @Override
    public long available() throws IOException {
        return this.semaphore.availablePermits();
    }
    
    public void close() {
        try {
            this.stack.close();
        } catch (IOException e) {
            Data.logger.debug("StackQueue: close error", e);
        }
    }
}
