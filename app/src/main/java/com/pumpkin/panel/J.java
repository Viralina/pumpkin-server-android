package com.pumpkin.panel;

import org.json.JSONArray;
import org.json.JSONObject;

/** Small JSON helper so endpoint code is not cluttered with try/catch. */
final class J {
    private J() {}

    static void p(JSONObject o, String key, Object value) {
        try {
            o.put(key, value);
        } catch (Exception ignored) {
        }
    }

    static void a(JSONArray arr, Object value) {
        try {
            arr.put(value);
        } catch (Exception ignored) {
        }
    }

    static String str(JSONObject o, String key, String def) {
        if (o == null || !o.has(key) || o.isNull(key)) return def;
        return o.optString(key, def);
    }

    static boolean bool(JSONObject o, String key, boolean def) {
        if (o == null || !o.has(key) || o.isNull(key)) return def;
        return o.optBoolean(key, def);
    }

    static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        for (int i = 0; i + 1 < kv.length; i += 2) p(o, String.valueOf(kv[i]), kv[i + 1]);
        return o;
    }
}
