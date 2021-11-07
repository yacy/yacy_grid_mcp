/**
 *  VolatileCord
 *  Copyright 08.10.2021 by Michael Peter Christen, @orbiterlab
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

package eu.searchlab.storage.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import eu.searchlab.storage.io.GenericIO;
import eu.searchlab.storage.io.IOPath;

public class VolatileCord extends AbstractCord implements Cord {

    private boolean unwrittenChanges;

    protected VolatileCord(GenericIO io, IOPath iop) {
        super(io, iop);
        this.unwrittenChanges = false;
    }

    @Override
    public Cord append(JSONObject value) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            this.array.put(value);
            this.unwrittenChanges = true;
            return this;
        }
    }

    @Override
    public Cord prepend(JSONObject value) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            try {
                this.array.put(0, value);
                this.unwrittenChanges = true;
            } catch (final JSONException e) {
                throw new IOException(e.getMessage());
            }
            return this;
        }
    }

    @Override
    public Cord insert(JSONObject value, int p) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            try {
                this.array.put(p, value);
                this.unwrittenChanges = true;
            } catch (final JSONException e) {
                throw new IOException(e.getMessage());
            }
            return this;
        }
    }

    @Override
    public JSONObject remove(int p) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Object o = this.array.remove(p);
            this.unwrittenChanges = true;
            assert o instanceof JSONObject;
            return (JSONObject) o;
        }
    }

    @Override
    public JSONObject removeFirst() throws IOException {
        return this.remove(0);
    }

    @Override
    public JSONObject removeLast() throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Object o = this.array.remove(this.array.length() - 1);
            this.unwrittenChanges = true;
            assert o instanceof JSONObject;
            return (JSONObject) o;
        }
    }

    @Override
    public List<JSONObject> removeAllWhere(String key, String value) throws IOException{
        final List<JSONObject> list = new ArrayList<>();
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Iterator<Object> i = this.array.iterator();
            while (i.hasNext()) {
                final Object o = i.next();
                if (!(o instanceof JSONObject)) continue;
                final Object v = ((JSONObject) o).opt(key);
                if (!(v instanceof String)) continue;
                if (((String) v).equals(value)) {
                    list.add((JSONObject) o);
                    i.remove();
                    this.unwrittenChanges = true;
                }
            }
            return list;
        }
    }

    @Override
    public List<JSONObject> removeAllWhere(String key, long value) throws IOException {
        final List<JSONObject> list = new ArrayList<>();
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Iterator<Object> i = this.array.iterator();
            while (i.hasNext()) {
                final Object o = i.next();
                if (!(o instanceof JSONObject)) continue;
                final Object v = ((JSONObject) o).opt(key);
                if (!(v instanceof Long) && !(v instanceof Integer)) continue;
                if (((Long) v).longValue() == value) {
                    list.add((JSONObject) o);
                    i.remove();
                    this.unwrittenChanges = true;
                }
            }
            return list;
        }
    }

    @Override
    public JSONObject removeOneWhere(String key, String value) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Iterator<Object> i = this.array.iterator();
            while (i.hasNext()) {
                final Object o = i.next();
                if (!(o instanceof JSONObject)) continue;
                final Object v = ((JSONObject) o).opt(key);
                if (!(v instanceof String)) continue;
                if (((String) v).equals(value)) {
                    i.remove();
                    this.unwrittenChanges = true;
                    return (JSONObject) o;
                }
            }
            return null;
        }
    }

    @Override
    public JSONObject removeOneWhere(String key, long value) throws IOException {
        synchronized (this.mutex) {
            this.ensureLoaded();
            final Iterator<Object> i = this.array.iterator();
            while (i.hasNext()) {
                final Object o = i.next();
                if (!(o instanceof JSONObject)) continue;
                final Object v = ((JSONObject) o).opt(key);
                if (!(v instanceof Long) && !(v instanceof Integer)) continue;
                if (((Long) v).longValue() == value) {
                    i.remove();
                    this.unwrittenChanges = true;
                    return (JSONObject) o;
                }
            }
            return null;
        }
    }

    @Override
    public Cord commit() throws IOException {
        synchronized (this.mutex) {
            if (!this.unwrittenChanges) return this;
            this.commitInternal();
            this.unwrittenChanges = false;
            return this;
        }
    }

    @Override
    public void close() throws IOException {
        this.commit();
        this.array = null;
    }

}
