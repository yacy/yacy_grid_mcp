/**
 *  LogAppender
 *  Copyright 02.01.2018 by Michael Peter Christen, @0rb1t3r
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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class LogAppender extends AppenderSkeleton implements Appender {

    private int maxlines;
    private ConcurrentLinkedQueue<String> lines;
    private AtomicInteger a;
    private Layout layout;

    public LogAppender(Layout layout, int maxlines) {
        this.layout = layout;
        super.setLayout(layout);
        this.maxlines = maxlines;
        this.lines = new ConcurrentLinkedQueue<>();
        this.a = new AtomicInteger(0);
    }

    @Override
    public Layout getLayout() {
        return this.layout;
    }

    @Override
    public void doAppend(LoggingEvent event) {
        if (event == null) return;
        String line = event.toString();
        if (line != null) this.lines.add(line);
        if (this.a.incrementAndGet() % 100 == 0) {
            clean(this.maxlines);
            this.a.set(0);
        }
    }

    @Override
    public void close() {
        this.lines.clear();
        this.lines = null;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    public ArrayList<String> getLines(int max) {
        Object[] a = this.lines.toArray();
        ArrayList<String> l = new ArrayList<>();
        int start = Math.max(0, a.length - max);
        for (int i = start; i < a.length; i++) l.add((String) a[i]);
        return l;
    }

    public void clean(int remaining) {
        int c = this.lines.size() - remaining;
        while (c-- > 0) this.lines.poll();
    }

    @Override
    public void addFilter(Filter newFilter) {
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void clearFilters() {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

}
