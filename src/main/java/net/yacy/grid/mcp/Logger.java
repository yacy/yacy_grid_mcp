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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import net.yacy.grid.tools.DateParser;


public class Logger {

    private static int maxlines = 10000;
    private static final ConcurrentLinkedQueue<String> lines = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger a = new AtomicInteger(0);
    private static final Map<Class<?>, org.slf4j.Logger> logger = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger dfltLogger = org.slf4j.LoggerFactory.getLogger("default");

    static {
        StreamHandler sh = new ConsoleHandler();
        java.util.logging.Logger javaLogger = java.util.logging.Logger.getGlobal();
        while (javaLogger.getParent() != null) javaLogger = javaLogger.getParent();
        Handler[] dfltHandlers = javaLogger.getHandlers();
        for (Handler handler: dfltHandlers) javaLogger.removeHandler(handler);
        sh.setFormatter(new LineFormatter());
        javaLogger.addHandler(sh);
        javaLogger.log(java.util.logging.Level.INFO, "configured logging");
    }

    /**
     * all logging is redirected to java logging and is handled by this console handler.
     */
    public static class ConsoleHandler extends StreamHandler {

        public ConsoleHandler() {
            super(System.out, new SimpleFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            super.flush();
        }
    }

    public static class LineFormatter extends Formatter {

        // format string for printing the log record
        private static final String format = "%1$s - %2$s - %3$s - %4$s%5$s%n";
        private final Date dat = new Date();

        @Override
        public synchronized String format(LogRecord record) {
            this.dat.setTime(record.getMillis());
            String source;
            if (record.getSourceClassName() != null) {
                source = record.getSourceClassName();
                if (record.getSourceMethodName() != null) {
                   source += "." + record.getSourceMethodName();
                }
            } else {
                source = record.getLoggerName();
            }
            String message = formatMessage(record);
            int p = message.indexOf('$');
            if (p > 0) {
                source = message.substring(0, p);
                message = message.substring(p + 1);
            }
            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }
            String levelname = record.getLevel().getName();
            String line = String.format(format,
                                 levelname,
                                 DateParser.formatRFC1123(this.dat),
                                 source,
                                 message,
                                 throwable);
            return line;
        }
    }

    public static void setMaxLines(int m) {
        maxlines = m;
    }

    public static ArrayList<String> getLines(int max) {
        Object[] a = lines.toArray();
        ArrayList<String> l = new ArrayList<>();
        int start = Math.max(0, a.length - max);
        for (int i = start; i < a.length; i++) l.add((String) a[i]);
        return l;
    }

    private static void append(String line) {
        if (line != null) lines.add(line);
        if (a.incrementAndGet() % 100 == 0) {
            clean(maxlines);
            a.set(0);
        }
    }

    public static void clean(int remaining) {
        int c = lines.size() - remaining;
        while (c-- > 0) lines.poll();
    }

    private static org.slf4j.Logger getLogger(Class<?> cls) {
        if (cls == null) return dfltLogger;
        org.slf4j.Logger l = logger.get(cls);
        if (l == null) {
            l = org.slf4j.LoggerFactory.getLogger(cls.getCanonicalName());
            logger.put(cls, l);
        }
        return l;
    }
    
    private final static String loggerClassName = Logger.class.getCanonicalName();

    private static Class<?> getCallerClass() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            String cn = ste.getClassName();
            if (!cn.equals(loggerClassName) && cn.indexOf("java.lang") < 0) {
                return ste.getClass();
            }
        }
        return null;
    }

    public static void debug(String msg) {
        debug(getCallerClass(), msg);
    }

    public static void debug(String msg, Throwable t) {
        debug(getCallerClass(), msg, t);
    }

    public static void debug(Class<?> cls, String msg) {
        getLogger(cls).info(cls.getCanonicalName() + '$' + msg);
        append(msg);
    }

    public static void debug(Class<?> cls, String msg, Throwable t) {
        getLogger(cls).info(cls.getCanonicalName() + '$' + msg, t);
        append(msg);
    }

    public static void info(String msg) {
        info(getCallerClass(), msg);
    }

    public static void info(Class<?> cls, String msg) {
        getLogger(cls).info(msg);
        append(msg);
    }

    public static void warn(String msg) {
        warn(getCallerClass(), msg);
    }

    public static void warn(Class<?> cls, String msg) {
        getLogger(cls).warn(cls.getCanonicalName() + '$' + msg);
        append(msg);
    }

    public static void warn(String msg, Throwable t) {
        warn(getCallerClass(), msg, t);
    }

    public static void warn(Class<?> cls, String msg, Throwable t) {
        getLogger(cls).warn(cls.getCanonicalName() + '$' + msg, t);
        append(msg);
    }

    public static void warn(Throwable t) {
        warn(getCallerClass(), t);
    }

    public static void warn(Class<?> cls, Throwable t) {
        getLogger(cls).warn(cls.getCanonicalName() + '$', t);
    }

    public static void error(String msg) {
        error(getCallerClass(), msg);
    }

    public static void error(Class<?> cls, String msg) {
        getLogger(cls).error(cls.getCanonicalName() + '$' + msg);
        append(msg);
    }

    public static void error(String msg, Throwable t) {
        error(getCallerClass(), msg, t);
    }

    public static void error(Class<?> cls, String msg, Throwable t) {
        getLogger(cls).error(cls.getCanonicalName() + '$' + msg, t);
        append(msg);
    }

    public static void error(Throwable t) {
        error(getCallerClass(), t);
    }

    public static void error(Class<?> cls, Throwable t) {
        getLogger(cls).error(cls.getCanonicalName() + '$', t);
    }


    public static void fatal(String msg) {
        fatal(getCallerClass(), msg);
    }

    public static void fatal(Class<?> cls, String msg) {
        getLogger(cls).error(cls.getCanonicalName() + '$' + msg);
        append(msg);
    }

    public static void fatal(String msg, Throwable t) {
        fatal(getCallerClass(), msg, t);
    }

    public static void fatal(Class<?> cls, String msg, Throwable t) {
        getLogger(cls).error(cls.getCanonicalName() + '$' + msg, t);
        append(msg);
    }

    public static void fatal(Throwable t) {
        fatal(getCallerClass(), t);
    }

    public static void fatal(Class<?> cls, Throwable t) {
        getLogger(cls).error(cls.getCanonicalName() + '$', t);
    }
}
