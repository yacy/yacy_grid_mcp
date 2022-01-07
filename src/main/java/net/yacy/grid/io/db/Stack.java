/**
 *  Stack
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

package net.yacy.grid.io.db;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for a stack which provides FIFO and FILO functionality
 * and also peek methods for stack bottom and stack top.
 */
public interface Stack<A> extends Closeable {

    /**
     * clears the content of the stack
     * @throws IOException
     */
    public Stack<A> clear() throws IOException;
    
    /**
     * ask for the number of entries
     * @return the number of entries in the stack
     */
    public int size();
    
    /**
     * ask if the stack is empty
     * @return true iff size() == 0
     */
    public boolean isEmpty();
    
    /**
     * add a value on top of the queue
     * @param value
     * @return the stack
     */
    public Stack<A> push(A value);

    /**
     * get the first entry in the stack without removing it
     * @return the first entry in the stack
     */
    public A bot();

    /**
     * get the first entry in the stack, removing it after returning
     * @return the first entry in the stack
     */
    public A pot();
    
    /**
     * get the latest entry in the stack without removing it
     * @return the latest entry in the stack
     */
    public A top();
    
    /**
     * get the latest entry in the stack, removing it after returning
     * @return the latest entry in the stack
     */
    public A pop();
    
}