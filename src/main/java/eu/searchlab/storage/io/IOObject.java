/**
 *  IOObject
 *  Copyright 11.05.2022 by Michael Peter Christen, @orbiterlab
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

package eu.searchlab.storage.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * An IOObject is the combination of an IOPath and a byte[] object.
 * This is required in all cases where sets of such combinations are wanted,
 * which happens especially in ConcurrentIO where pseudo-simultanously read/write
 * operation are wanted.
 * This object also includes read/write operations for JSONObject and JSONArray to
 * provide a generalized view to serialized JSON objects:
 * - a JSONObject serialization always starts and ends with a single line with '{' or '}'
 *   and single lines in between for each key/value pairs
 *   without further pretty-printing and no leading spaces.
 * - a JSONArray serialization always starts with a single line with '[‘ or ']'
 *   als single lines in between for each array entry
 *   without further pretty-printing and no leading spaces.
 * This ensures a compact for large JSON while it is prepared to run grep commands against it.
 * Small JSON still appear pretty-printed because on top level the JSON is expanded to several lines.
 *
 * Files with JSON encoding like this can easily be merged without the parsing/printing overhead!
 */
public final class IOObject {

    private static final char LF = (char) 10; // we don't use '\n' or System.getProperty("line.separator"); here to be consistent over all systems.

    private final IOPath path;
    private final byte[] object;

    /**
     * Construct a IOObject from a byte array.
     * This method should never be used in case that the object was generated
     * from a JSON Object or Array. Use the specialized methods below instead.
     * @param path
     * @param object
     */
    public IOObject(final IOPath path, final byte[] object) {
        assert object.length > 0;
        assert object[0] != '[';
        assert object[0] != '{';
        this.path = path;
        this.object = object;
    }

    /**
     * Write a String into a IOObject
     * This method should never be used in case that the object was generated
     * from a JSON Object or Array. Use the specialized methods below instead.
     * @param path
     * @param string
     */
    public IOObject(final IOPath path, final String string) {
        assert string.length() > 0;
        assert string.charAt(0) != '[';
        assert string.charAt(0) != '{';
        this.path = path;
        this.object = string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Write a JSONObject into a IOObject using a specialized json pretty printer
     * which enables efficient merging.
     * @param path
     * @param json
     * @throws IOException
     */
    public IOObject(final IOPath path, final JSONObject json) throws IOException {
        this.path = path;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeJSONObject(baos, json);
        this.object = baos.toByteArray();
        baos.close();
    }

    /**
     * Write a JSONArray into a IOObject using a specialized json pretty printer
     * which enables efficient merging.
     * @param path
     * @param json
     * @throws IOException
     */
    public IOObject(final IOPath path, final JSONArray json) throws IOException {
        this.path = path;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOObject.writeJSONArray(baos, json);
        this.object = baos.toByteArray();
        baos.close();
    }

    /**
     * Construct a new IOObject by merging given IOObjects into one byte[].
     * If the given IOObject all are of same content type and either JSONObject
     * or JSONArray, the merge can be done in a very efficient way without parsing/printing
     * of JSON object types.
     * @param iop
     * @param ioos
     */
    public IOObject(final IOPath iop, final IOObject... ioos) {
        throw new UnsupportedOperationException(); // TODO to be implemented
    }

    public final IOPath getPath() {
        return this.path;
    }

    public final byte[] getObject() {
        return this.object;
    }

    public final String getString() {
        return new String(this.object, StandardCharsets.UTF_8);
    }

    public final JSONObject getJSONObject() throws IOException {
        assert this.object != null;
        assert this.object.length > 0;
        assert this.object[0] == '{';
        final JSONObject json = readJSONObject(this.object);
        return json;
    }

    public final JSONArray getJSONArray() throws IOException {
        assert this.object != null;
        assert this.object.length > 0;
        assert this.object[0] == '[';
        final JSONArray json = readJSONArray(this.object);
        return json;
    }

    public static void writeJSONObject(final OutputStream os, final JSONObject json) throws IOException {
        if (json == null) throw new IOException("json must not be null");
        final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        writer.write('{');
        writer.write(LF);
        final String[] keys = new String[json.length()];
        int p = 0;
        for (final String key: json.keySet()) keys[p++] = key; // we do this only to get a hint which key is the last one
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final Object obj = json.opt(key);
            if (obj == null) continue;
            writer.write('"'); writer.write(key); writer.write('"'); writer.write(':');
            if (obj instanceof JSONObject) {
                writer.write(((JSONObject) obj).toString());
            } else if (obj instanceof Map) {
                writer.write(new JSONObject((Map<?,?>) obj).toString());
            } else if (obj instanceof JSONArray) {
                writer.write(((JSONArray) obj).toString());
            } else if (obj instanceof Collection) {
                writer.write(new JSONArray((Collection<?>) obj).toString());
            } else if (obj instanceof String) {
                writer.write('"'); writer.write(((String) obj)); writer.write('"');
            } else {
                writer.write(obj.toString());
            }
            if (i < keys.length - 1) writer.write(',');
            writer.write(LF);
        }
        writer.write('}');
        writer.close();
    }

    public static void writeJSONArray(final OutputStream os, final JSONArray array) throws IOException {
        if (array == null) throw new IOException("array must not be null");
        final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        writer.write('[');
        writer.write(LF);
        for (int i = 0; i < array.length(); i++) {
            Object obj;
            try {
                obj = array.get(i);
            } catch (final JSONException e) {
                continue;
            }
            if (obj == null) continue;
            if (obj instanceof JSONObject) {
                writer.write(((JSONObject) obj).toString());
            } else if (obj instanceof Map) {
                writer.write(new JSONObject((Map<?,?>) obj).toString());
            } else if (obj instanceof JSONArray) {
                writer.write(((JSONArray) obj).toString());
            } else if (obj instanceof Collection) {
                writer.write(new JSONArray((Collection<?>) obj).toString());
            } else if (obj instanceof String) {
                writer.write('"'); writer.write(((String) obj)); writer.write('"');
            } else {
                writer.write(obj.toString());
            }
            if (i < array.length() - 1) writer.write(',');
            writer.write(LF);
        }
        writer.write(']');
        writer.close();
    }

    public static JSONObject readJSONObject(final byte[] b) throws IOException {
        JSONObject json = new JSONObject(true);
        // The file can be written in either of two ways:
        // - as a simple toString() or toString(2) from a JSONObject
        // - as a list of properties, one in each line with one line in the front starting with "{" and one in the end, starting with "}"
        // If the file was written in the first way, all property keys must be unique because we must use the JSONTokener to parse it.
        // if the file was written in the second way, we can apply a reader which reads the file line by line, overwriting a property
        // if it appears a second (third..) time. This has a big advantage: we can append new properties just at the end of the file.
        if (b.length == 0) return json;
        // check which variant is in b[]:
        // in a toString() output, there is no line break
        // in a toString(2) output, there is a line break but also two spaces in front
        // in a line-by-line output, there is a line break at b[1] and no spaces after that because the first char is a '"' and the second a letter
        final boolean lineByLine = (b.length == 3 && b[0] == '{' && b[1] == LF && b[2] == '}') || (b[1] < ' ' && b[2] != ' ' && b[3] != ' ');
        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        if (lineByLine) {
            final int a = bais.read();
            assert (a == '{');
            final BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("}")) break;
                    if (line.length() == 0) continue;
                    final int p = line.indexOf("\":");
                    if (p < 0) continue;
                    final String key = line.substring(1, p).trim();
                    String value = line.substring(p + 2).trim();
                    if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
                    if (value.charAt(0) == '{') {
                        json.put(key, new JSONObject(new JSONTokener(value)));
                    } else if (value.charAt(0) == '[') {
                        json.put(key, new JSONArray(new JSONTokener(value)));
                    } else if (value.charAt(0) == '"') {
                        json.put(key, value.substring(1, value.length() - 1));
                    } else if (value.indexOf('.') > 0) {
                        try {
                            json.put(key, Double.parseDouble(value));
                        } catch (final NumberFormatException e) {
                            json.put(key, value);
                        }
                    } else {
                        try {
                            json.put(key, Long.parseLong(value));
                        } catch (final NumberFormatException e) {
                            json.put(key, value);
                        }
                    }
                }
            } catch (final JSONException e) {
                throw new IOException(e);
            }
        } else {
            try {
                json = new JSONObject(new JSONTokener(new InputStreamReader(bais, StandardCharsets.UTF_8)));
            } catch (final JSONException e) {
                // could be a double key problem. In that case we should repeat the process with another approach
                throw new IOException(e);
            }
        }
        return json;
    }

    public static JSONObject readJSONObject0(final byte[] b) throws IOException {
       return readJSONObject1(readJSONMap(b));
    }

    private static JSONObject readJSONObject1(final LinkedHashMap<String, String> map) throws IOException {
        final JSONObject json = new JSONObject(true);
        for (final Map.Entry<String, String> entry: map.entrySet()) {
            try {
                final String key = entry.getKey();
                String value = entry.getValue();
                if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
                if (value.charAt(0) == '{') {
                    json.put(key, new JSONObject(new JSONTokener(value)));
                } else if (value.charAt(0) == '[') {
                    json.put(key, new JSONArray(new JSONTokener(value)));
                } else if (value.charAt(0) == '"') {
                    json.put(key, value.substring(1, value.length() - 1));
                } else if (value.indexOf('.') > 0) {
                    try {
                        json.put(key, Double.parseDouble(value));
                    } catch (final NumberFormatException e) {
                        json.put(key, value);
                    }
                } else {
                    try {
                        json.put(key, Long.parseLong(value));
                    } catch (final NumberFormatException e) {
                        json.put(key, value);
                    }
                }
            } catch (final JSONException e) {
                throw new IOException(e);
            }
        }
        return json;
    }

    private static LinkedHashMap<String, String> readJSONMap(final byte[] b) throws IOException {
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        // The file can be written in either of two ways:
        // - as a simple toString() or toString(2) from a JSONObject
        // - as a list of properties, one in each line with one line in the front starting with "{" and one in the end, starting with "}"
        // If the file was written in the first way, all property keys must be unique because we must use the JSONTokener to parse it.
        // if the file was written in the second way, we can apply a reader which reads the file line by line, overwriting a property
        // if it appears a second (third..) time. This has a big advantage: we can append new properties just at the end of the file.
        if (b.length == 0) return map;
        // check which variant is in b[]:
        // in a toString() output, there is no line break
        // in a toString(2) output, there is a line break but also two spaces in front
        // in a line-by-line output, there is a line break at b[1] and no spaces after that because the first char is a '"' and the second a letter
        final boolean lineByLine = (b.length == 3 && b[0] == '{' && b[1] == LF && b[2] == '}') || (b[1] < ' ' && b[2] != ' ' && b[3] != ' ');
        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        if (lineByLine) {
            final int a = bais.read();
            assert (a == '{');
            final BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("}")) break;
                if (line.length() == 0) continue;
                final int p = line.indexOf("\":");
                if (p < 0) continue;
                final String key = line.substring(1, p).trim();
                final String value = line.substring(p + 2).trim();
                map.put(key, value);
            }
        } else {
            try {
                final JSONObject json = new JSONObject(new JSONTokener(new InputStreamReader(bais, StandardCharsets.UTF_8)));
                for (final String key: json.keySet()) {
                    @SuppressWarnings("deprecation")
                    final Object value = json.get(key);
                    map.put(key, value.toString());
                }
            } catch (final JSONException e) {
                // could be a double key problem. In that case we should repeat the process with another approach
                throw new IOException(e);
            }
        }
        return map;
    }

    public static JSONArray readJSONArray(final byte[] b) throws IOException {
        JSONArray array = new JSONArray();
        // The file can be written in either of two ways:
        // - as a simple toString() or toString(2) from a JSONObject
        // - as a list of properties, one in each line with one line in the front starting with "{" and one in the end, starting with "}"
        // If the file was written in the first way, all property keys must be unique because we must use the JSONTokener to parse it.
        // if the file was written in the second way, we can apply a reader which reads the file line by line, overwriting a property
        // if it appears a second (third..) time. This has a big advantage: we can append new properties just at the end of the file.
        if (b.length == 0) return array;
        // check which variant is in b[]:
        // in a toString() output, there is no line break
        // in a toString(2) output, there is a line break but also two spaces in front
        // in a line-by-line output, there is a line break at b[1] and no spaces after that because the first char starts the object
        final boolean lineByLine = (b.length == 3 && b[0] == '[' && b[1] == LF && b[2] == ']') || (b[1] < ' ' && b[2] != ' ' && b[3] != ' ');
        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        if (lineByLine) {
            final int a = bais.read();
            assert (a == '[');
            final BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("]")) break;
                    if (line.length() == 0) continue;
                    if (line.endsWith(",")) line = line.substring(0, line.length() - 1);
                    if (line.charAt(0) == '{') {
                        array.put(new JSONObject(new JSONTokener(line)));
                    } else if (line.charAt(0) == '[') {
                        array.put(new JSONArray(new JSONTokener(line)));
                    } else if (line.charAt(0) == '"') {
                        array.put(line.substring(1, line.length() - 1));
                    } else if (line.indexOf('.') > 0) {
                        try {
                            array.put(Double.parseDouble(line));
                        } catch (final NumberFormatException e) {
                            array.put(line);
                        }
                    } else {
                        try {
                            array.put(Long.parseLong(line));
                        } catch (final NumberFormatException e) {
                            array.put(line);
                        }
                    }
                }
            } catch (final JSONException e) {
                throw new IOException(e);
            }
        } else {
            try {
                array = new JSONArray(new JSONTokener(new InputStreamReader(bais, StandardCharsets.UTF_8)));
            } catch (final JSONException e) {
                // could be a double key problem. In that case we should repeat the process with another approach
                throw new IOException(e);
            }
        }
        return array;
    }
}
