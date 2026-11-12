package com.pumpkin.panel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Text-based pumpkin.toml editing, ported from server.js. */
final class Toml {
    private Toml() {}

    private static final Pattern HEADER = Pattern.compile("^\\s*\\[[^\\]]+\\]\\s*$");

    static boolean isHeader(String line) {
        return line != null && HEADER.matcher(line).matches();
    }

    static String format(Object value) {
        if (value instanceof Boolean || value instanceof Number) return String.valueOf(value);
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(format(arr.opt(i)));
            }
            return sb.append(']').toString();
        }
        return JSONObject.quote(value == null ? "" : String.valueOf(value));
    }

    static String set(String text, String keyPath, Object value) {
        String path = keyPath == null ? "" : keyPath;
        int dot = path.lastIndexOf('.');
        String section = dot >= 0 ? path.substring(0, dot) : "";
        String key = dot >= 0 ? path.substring(dot + 1) : path;
        if (key.isEmpty()) return text;

        List<String> lines = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            Collections.addAll(lines, text.split("\n", -1));
        }

        int start = 0;
        int end = lines.size();

        if (!section.isEmpty()) {
            int idx = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("[" + section + "]")) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                String tail = text == null ? "" : text.replaceAll("\\s+$", "");
                return tail + (tail.isEmpty() ? "" : "\n\n")
                        + "[" + section + "]\n" + key + " = " + format(value) + "\n";
            }
            start = idx + 1;
            for (int i = start; i < lines.size(); i++) {
                if (isHeader(lines.get(i))) {
                    end = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < lines.size(); i++) {
                if (isHeader(lines.get(i))) {
                    end = i;
                    break;
                }
            }
        }

        Pattern keyRe = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=");
        for (int i = start; i < end && i < lines.size(); i++) {
            if (keyRe.matcher(lines.get(i)).find()) {
                lines.set(i, key + " = " + format(value));
                return join(lines);
            }
        }

        if (end > lines.size()) end = lines.size();
        lines.add(end, key + " = " + format(value));
        return join(lines);
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /* ------------------------------------------------------------- reading */

    static String grabSection(String toml, String section, String key) {
        if (toml == null || toml.isEmpty() || section == null || section.isEmpty()) return null;
        String marker = "[" + section + "]";
        int start;
        if (toml.startsWith(marker)) {
            start = 0;
        } else {
            int idx = toml.indexOf("\n" + marker);
            if (idx < 0) return null;
            start = idx + 1;
        }
        int next = toml.indexOf("\n[", start + marker.length());
        String block = next < 0 ? toml.substring(start) : toml.substring(start, next);
        Pattern re = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=\\s*(.+)$", Pattern.MULTILINE);
        java.util.regex.Matcher m = re.matcher(block);
        return m.find() ? m.group(1).trim() : null;
    }

    static String grabRoot(String toml, String key) {
        if (toml == null || toml.isEmpty()) return null;
        Pattern re = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=\\s*(.+)$", Pattern.MULTILINE);
        java.util.regex.Matcher m = re.matcher(toml);
        return m.find() ? m.group(1).trim() : null;
    }

    static String unquote(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\""))
                || (t.startsWith("'") && t.endsWith("'")))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    static int parseIntSafe(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return def;
        }
    }
}
