package com.pumpkin.panel;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.security.SecureRandom;

/** Panel settings (replacement for config.json). */
final class Store {
    private final Context app;
    private final SharedPreferences sp;

    Store(Context ctx) {
        app = ctx.getApplicationContext();
        sp = app.getSharedPreferences("panel", Context.MODE_PRIVATE);
    }

    SharedPreferences raw() {
        return sp;
    }

    String name() { return sp.getString("name", "Pumpkin Server"); }
    int port() { return sp.getInt("port", 8087); }
    String host() { return sp.getString("host", "127.0.0.1"); }
    /** Web panel online = may be opened from other devices. Default off. */
    boolean panelOnline() { return sp.getBoolean("panelOnline", false); }
    String serverDir() { return sp.getString("serverDir", "/sdcard/server"); }
    String backupDir() { return sp.getString("backupDir", "/sdcard/server/backups"); }
    int maxLogLines() { return sp.getInt("maxLogLines", 2500); }
    boolean autoStart() { return sp.getBoolean("autoStart", false); }
    boolean autoRestart() { return sp.getBoolean("autoRestart", false); }
    /** An access token always exists: generated automatically on first use. */
    String token() {
        String t = sp.getString("token", "");
        if (t == null) t = "";
        t = t.trim();
        if (t.isEmpty()) {
            byte[] b = new byte[16];
            new SecureRandom().nextBytes(b);
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) sb.append(String.format("%02x", x & 0xff));
            t = sb.toString();
            sp.edit().putString("token", t).apply();
        }
        return t;
    }
    String lang() { return sp.getString("lang", "id"); }
    void setLang(String v) { sp.edit().putString("lang", "en".equals(v) ? "en" : "id").apply(); }
    String guestCmd() { return sp.getString("guestCmd", "pumpkin"); }
    /** ID of the bundled binary already extracted from the full APK. */
    String bundledBinId() { return sp.getString("bundledBinId", ""); }
    void setBundledBinId(String v) { sp.edit().putString("bundledBinId", v).apply(); }

    File binFile() {
        String p = sp.getString("binPath", "");
        if (p == null || p.isEmpty()) {
            p = new File(app.getFilesDir(), "pumpkin").getAbsolutePath();
        }
        return new File(p);
    }

    void put(String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    void putBool(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    File serverDirFile() {
        return new File(serverDir());
    }

    File tomlFile() {
        return new File(serverDirFile(), "pumpkin.toml");
    }

    File dataFile(String name) {
        return new File(new File(serverDirFile(), "data"), name);
    }

    File logsFile() {
        return new File(new File(serverDirFile(), "logs"), "latest.log");
    }

    File backupDirFile() {
        return new File(backupDir());
    }
}
