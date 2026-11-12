package com.pumpkin.panel;

import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pumpkin process controller + all panel data.
 * This is a port of server.js without Node.js.
 */
final class Pumpkin {

    static final class ProcInfo {
        int pid;
        String cmd;
    }

    private static final Pattern TIME_HEAD = Pattern.compile("^\\[?(\\d{1,2}:\\d{2}:\\d{2})\\]?\\s+");
    private static final Pattern BRACKET_LEVEL = Pattern.compile(
            "^\\[(INFO|WARN|WARNING|ERROR|DEBUG|TRACE|CRITICAL|FATAL)\\]\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE_LEVEL = Pattern.compile(
            "^(INFO|WARN|WARNING|ERROR|DEBUG|TRACE|CRITICAL|FATAL)\\b\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN = Pattern.compile("^(\\S+) joined the game", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEFT = Pattern.compile("^(\\S+) left the game", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLINE = Pattern.compile(
            "There are \\d+ of a max of \\d+ players online:\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION = Pattern.compile("Starting Pumpkin\\s+([\\w.+-]+)");

    private static final long APP_START = System.currentTimeMillis();

    private final Context app;
    private final Store st;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private volatile ApiServer api;
    private volatile Process proc;
    private volatile long startedAt;
    private volatile boolean stopping;
    private volatile int stopToken;
    private volatile boolean restartQueued;
    private volatile int autoRestartCount;

    final List<JSONObject> buffer = new ArrayList<>();
    int seq;
    final LinkedHashSet<String> players = new LinkedHashSet<>();
    private volatile String version = "";
    final List<JSONObject> metrics = new ArrayList<>();

    private long lastSampleT;
    private long lastJiffies;
    private volatile int worldMb;
    private volatile long worldMbAt;
    private final AtomicBoolean worldBusy = new AtomicBoolean(false);

    private volatile JSONObject backupJob;

    Pumpkin(Context ctx, Store store) {
        app = ctx.getApplicationContext();
        st = store;
    }

    void setApi(ApiServer a) {
        api = a;
    }

    /* ------------------------------------------------------------ utilities */

    private JSONObject res(boolean ok, String msg) {
        JSONObject o = new JSONObject();
        J.p(o, "ok", ok);
        if (msg != null && !msg.isEmpty()) J.p(o, "msg", msg);
        return o;
    }

    private void broadcast(String event, JSONObject data) {
        ApiServer a = api;
        if (a != null) a.broadcast(event, data);
    }

    private String toml() {
        return Fs.readTextQuiet(st.tomlFile());
    }

    String levelName() {
        String v = Toml.grabRoot(toml(), "default_level_name");
        String name = Toml.unquote(v);
        return name.isEmpty() ? "world" : name;
    }

    private String seed() {
        return Toml.unquote(Toml.grabRoot(toml(), "seed"));
    }

    /* ------------------------------------------------------------------ log */

    /**
     * Split one Pumpkin log line into [time, level, message].
     * Handled formats: "[INFO] message", "12:00:00 INFO message",
     * "[12:00:00] [INFO] message", "INFO message".
     */
    static String[] splitLogLine(String clean) {
        String rest = clean.trim();
        String time = "";
        String level = "";
        Matcher tm = TIME_HEAD.matcher(rest);
        if (tm.find()) {
            time = tm.group(1);
            rest = rest.substring(tm.end()).trim();
        }
        Matcher bl = BRACKET_LEVEL.matcher(rest);
        if (bl.find()) {
            level = normLevel(bl.group(1));
            rest = rest.substring(bl.end()).trim();
        } else {
            Matcher bm = BARE_LEVEL.matcher(rest);
            if (bm.find()) {
                level = normLevel(bm.group(1));
                rest = rest.substring(bm.end()).trim();
            }
        }
        return new String[]{time, level, rest};
    }

    private static String normLevel(String lv) {
        String u = lv.toUpperCase(Locale.US);
        if ("WARNING".equals(u)) return "WARN";
        if ("CRITICAL".equals(u) || "FATAL".equals(u)) return "ERROR";
        return u;
    }

    private static String nowTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
    }

    /** Diagnostic note to &lt;serverDir&gt;/panel.log so it can be inspected outside the app. */
    void diag(String text) {
        try {
            File f = new File(st.serverDirFile(), "panel.log");
            if (f.length() > 256 * 1024) //noinspection ResultOfMethodCallIgnored
                f.delete();
            Fs.appendText(f, nowTime() + " " + text + "\n");
        } catch (Exception ignored) {
        }
    }

    void pushLog(String text, String src) {
        String clean = Fs.stripAnsi(text);
        clean = clean.replaceAll("\\s+$", "");
        if (clean.isEmpty()) return;

        String[] parts = splitLogLine(clean);
        String time = parts[0].isEmpty() ? nowTime() : parts[0];
        String level = parts[1];
        String msg = parts[2];

        if (version.isEmpty()) {
            Matcher v = VERSION.matcher(clean);
            if (v.find()) version = v.group(1);
        }

        Matcher j = JOIN.matcher(msg);
        if (j.find()) {
            synchronized (players) {
                players.add(j.group(1));
            }
        }
        Matcher l = LEFT.matcher(msg);
        if (l.find()) {
            synchronized (players) {
                players.remove(l.group(1));
            }
        }
        Matcher on = ONLINE.matcher(msg);
        if (on.find()) {
            synchronized (players) {
                players.clear();
                String rest = on.group(1) == null ? "" : on.group(1);
                for (String n : rest.split("[, ]+")) {
                    if (!n.isEmpty()) players.add(n);
                }
            }
        }

        JSONObject e = new JSONObject();
        J.p(e, "id", ++seq);
        J.p(e, "time", time);
        J.p(e, "level", level);
        J.p(e, "msg", msg);
        J.p(e, "src", src);
        J.p(e, "ts", System.currentTimeMillis());

        synchronized (buffer) {
            buffer.add(e);
            int max = Math.max(50, st.maxLogLines());
            while (buffer.size() > max) buffer.remove(0);
        }
        broadcast("log", e);
    }

    void primeLogs() {
        try {
            String raw = Fs.readText(st.logsFile());
            String[] lines = raw.split("\n");
            int from = Math.max(0, lines.length - 120);
            for (int i = from; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) pushLog(lines[i], "file");
            }
        } catch (Exception ignored) {
        }
    }

    /* --------------------------------------------------------------- process */

    private Process child() {
        return proc;
    }

    boolean isRunning() {
        return proc != null || !procList().isEmpty();
    }

    List<ProcInfo> procList() {
        List<ProcInfo> out = new ArrayList<>();
        File[] entries = new File("/proc").listFiles();
        if (entries == null) return out;
        String selfBin = st.binFile().getAbsolutePath();
        for (File d : entries) {
            String name = d.getName();
            if (!isNumber(name)) continue;
            String raw = Fs.readTextQuiet(new File(d, "cmdline"));
            if (raw.isEmpty()) continue;
            String[] argv = raw.split("\u0000");
            boolean hit = false;
            StringBuilder cmd = new StringBuilder();
            for (String a : argv) {
                if (a == null || a.isEmpty()) continue;
                if (cmd.length() > 0) cmd.append(' ');
                cmd.append(a);
                if (isSelfBinary(a, selfBin)) hit = true;
            }
            if (hit) {
                ProcInfo p = new ProcInfo();
                p.pid = Integer.parseInt(name);
                p.cmd = cmd.toString();
                out.add(p);
            }
        }
        return out;
    }

    private static boolean isNumber(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    /** Match only this panel's binary, not other pumpkin processes (e.g. Termux). */
    private static boolean isSelfBinary(String arg, String selfBin) {
        if (arg == null || arg.isEmpty()) return false;
        if (arg.equals(selfBin)) return true;
        if (!arg.contains("pumpkin")) return false;
        try {
            return new File(arg).getCanonicalPath().equals(new File(selfBin).getCanonicalPath());
        } catch (Exception e) {
            return false;
        }
    }

    private long procRss(int pid) {
        try {
            String raw = Fs.readText(new File("/proc/" + pid + "/status"));
            Matcher m = Pattern.compile("VmRSS:\\s+(\\d+) kB").matcher(raw);
            return m.find() ? Long.parseLong(m.group(1)) * 1024L : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** @return [utime+stime, starttime] or null */
    private long[] procStat(int pid) {
        try {
            String raw = Fs.readText(new File("/proc/" + pid + "/stat"));
            int end = raw.lastIndexOf(')');
            if (end < 0) return null;
            String[] f = raw.substring(end + 2).split(" ");
            return new long[]{Long.parseLong(f[11]) + Long.parseLong(f[12]), Long.parseLong(f[21])};
        } catch (Exception e) {
            return null;
        }
    }

    private void signal(int pid, String sig) {
        try {
            new ProcessBuilder("/system/bin/kill", "-" + sig, String.valueOf(pid))
                    .redirectErrorStream(true).start();
        } catch (Exception ignored) {
        }
    }

    JSONObject start() {
        synchronized (this) {
            if (proc != null || !procList().isEmpty()) return res(false, "Server sudah berjalan");
            File bin = st.binFile();
            if (!bin.isFile()) return res(false, "Binary pumpkin belum ada: " + bin.getAbsolutePath());
            String problem = Fs.binaryProblem(bin);
            if (!problem.isEmpty()) {
                pushLog("[panel] binary ditolak: " + problem, "panel");
                return res(false, "Binary tidak bisa dipakai: " + problem
                        + ". Impor ulang file pumpkin-aarch64-android.");
            }
            if (!bin.canExecute()) {
                //noinspection ResultOfMethodCallIgnored
                bin.setExecutable(true, true);
            }
            File dir = st.serverDirFile();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return res(false, "Folder server tidak bisa dibuat: " + dir.getAbsolutePath());
            }

            stopping = false;
            stopToken++;
            startedAt = System.currentTimeMillis();
            synchronized (players) {
                players.clear();
            }
            pushLog("[panel] menjalankan: " + bin.getAbsolutePath(), "panel");

            ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath());
            pb.directory(dir);
            pb.redirectErrorStream(true);
            try {
                java.util.Map<String, String> env = pb.environment();
                env.remove("LD_PRELOAD");
                env.remove("LD_LIBRARY_PATH");
                env.put("HOME", dir.getAbsolutePath());
                env.put("TMPDIR", app.getCacheDir().getAbsolutePath());
                env.put("XDG_CACHE_HOME", app.getCacheDir().getAbsolutePath());
            } catch (Exception ignored) {
            }

            Process p;
            try {
                p = pb.start();
            } catch (Exception e) {
                startedAt = 0;
                stopping = false;
                String msg = "Gagal menjalankan Pumpkin: " + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : ": " + e.getMessage());
                pushLog("[panel] " + msg, "panel");
                diag("EXEC GAGAL bin=" + bin.getAbsolutePath()
                        + " size=" + bin.length()
                        + " exec=" + bin.canExecute()
                        + " interp=" + Fs.elfInterpreter(bin)
                        + " dir=" + dir.getAbsolutePath()
                        + " dirWrite=" + dir.canWrite()
                        + " err=" + msg);
                return res(false, msg);
            }
            proc = p;
            diag("START bin=" + bin.getAbsolutePath() + " size=" + bin.length()
                    + " dir=" + dir.getAbsolutePath());

            pool.execute(() -> watch(p));
            pool.execute(() -> {
                sleep(600);
                broadcast("status", status());
            });

            JSONObject o = res(true, "");
            J.p(o, "pid", procList().isEmpty() ? 0 : procList().get(0).pid);
            return o;
        }
    }

    private void watch(Process p) {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) pushLog(line, "server");
            }
        } catch (Exception ignored) {
        }

        int code;
        try {
            code = p.waitFor();
        } catch (Exception e) {
            code = -1;
        }

        long up = startedAt > 0 ? Math.round((System.currentTimeMillis() - startedAt) / 1000.0) : 0;
        pushLog("[panel] server berhenti (kode=" + code + ", uptime=" + up + "s)", "panel");
        diag("EXIT kode=" + code + " uptime=" + up + "s");

        synchronized (this) {
            if (proc == p) proc = null;
            startedAt = 0;
            synchronized (players) {
                players.clear();
            }
            lastSampleT = 0;
        }

        broadcast("status", status());

        if (restartQueued) {
            restartQueued = false;
            pool.execute(() -> {
                sleep(400);
                for (int i = 0; i < 24 && !procList().isEmpty(); i++) sleep(500);
                start();
            });
        } else if (!stopping && st.autoRestart() && autoRestartCount < 3) {
            autoRestartCount++;
            pushLog("[panel] auto-restart (" + autoRestartCount + "/3) dalam 5 detik...", "panel");
            pool.execute(() -> {
                sleep(5000);
                start();
            });
        }
        stopping = false;
    }

    JSONObject stop(boolean force) {
        List<ProcInfo> procs = procList();
        final Process p = proc;
        if (p == null && procs.isEmpty()) return res(true, "Server sudah berhenti");

        stopping = true;
        final int token = ++stopToken;
        restartQueued = false;
        autoRestartCount = 0;
        pushLog(force ? "[panel] mematikan paksa server..." : "[panel] menghentikan server...", "panel");

        if (p != null && !force) {
            try {
                p.getOutputStream().write("stop\n".getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().flush();
            } catch (Exception ignored) {
            }
        }

        if (force) {
            if (p != null) p.destroyForcibly();
            for (ProcInfo pi : procList()) signal(pi.pid, "KILL");
            return res(true, "");
        }

        if (p == null) {
            pushLog("[panel] server eksternal — kirim SIGINT", "panel");
            for (ProcInfo pi : procs) signal(pi.pid, "INT");
        }

        pool.execute(() -> {
            sleep(9000);
            if (token != stopToken || !stopping) return;
            if (!procList().isEmpty()) {
                pushLog("[panel] server belum berhenti, kirim SIGINT", "panel");
                for (ProcInfo pi : procList()) signal(pi.pid, "INT");
            }
        });

        pool.execute(() -> {
            sleep(22000);
            if (token != stopToken || !stopping) return;
            if (!procList().isEmpty()) {
                pushLog("[panel] server masih hidup, SIGKILL", "panel");
                for (ProcInfo pi : procList()) signal(pi.pid, "KILL");
            }
        });

        return res(true, "");
    }

    JSONObject restart() {
        if (proc == null) {
            if (procList().isEmpty()) return start();
            // Server runs outside the panel: there is no watcher, so wait manually.
            stop(false);
            restartQueued = false;
            pool.execute(() -> {
                for (int i = 0; i < 40 && !procList().isEmpty(); i++) sleep(500);
                if (procList().isEmpty()) {
                    sleep(400);
                    start();
                } else {
                    pushLog("[panel] restart dibatalkan: server eksternal masih hidup", "panel");
                }
            });
            return res(true, "Restart dijadwalkan");
        }
        stop(false);
        restartQueued = true;
        return res(true, "Restart dijadwalkan");
    }

    JSONObject command(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return res(false, "Perintah kosong");
        Process p = proc;
        if (p == null) return res(false, "Server tidak berjalan dari panel ini");
        pushLog(cmd, "cmd");
        try {
            p.getOutputStream().write((cmd.replace("\n", "") + "\n").getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().flush();
            return res(true, "");
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
    }

    private boolean rawCommand(String cmd) {
        Process p = proc;
        if (p == null) return false;
        try {
            p.getOutputStream().write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
            p.getOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    /* -------------------------------------------------------------- metrics */

    private int[] sample(List<ProcInfo> procs) {
        long rss = 0;
        long jiffies = 0;
        for (ProcInfo p : procs) {
            rss += procRss(p.pid);
            long[] s = procStat(p.pid);
            if (s != null) jiffies += s[0];
        }

        long now = System.currentTimeMillis();
        double cpu = 0;
        synchronized (this) {
            if (lastSampleT > 0 && now > lastSampleT) {
                double dt = (now - lastSampleT) / 1000.0;
                long dj = jiffies - lastJiffies;
                if (dt > 0) cpu = Math.max(0, ((double) dj / 100.0) / dt * 100.0);
            }
            if (!procs.isEmpty()) {
                lastSampleT = now;
                lastJiffies = jiffies;
            }
        }

        int memMB = (int) Math.round(rss / 1048576.0);
        JSONObject m = new JSONObject();
        J.p(m, "t", now);
        J.p(m, "cpu", Math.round(cpu));
        J.p(m, "mem", memMB);
        synchronized (metrics) {
            metrics.add(m);
            while (metrics.size() > 90) metrics.remove(0);
        }
        return new int[]{(int) Math.round(cpu), memMB};
    }

    void refreshWorldSizeAsync() {
        if (!worldBusy.compareAndSet(false, true)) return;
        pool.execute(() -> {
            try {
                long bytes = Fs.sizeOf(new File(st.serverDirFile(), levelName()));
                worldMb = (int) Math.round(bytes / 1048576.0);
                worldMbAt = System.currentTimeMillis();
            } finally {
                worldBusy.set(false);
            }
        });
    }

    /* --------------------------------------------------------------- status */

    JSONObject status() {
        List<ProcInfo> procs = procList();
        int[] sm = sample(procs);
        String t = toml();

        String javaAddr = Toml.grabSection(t, "networking.java", "address");
        if (javaAddr == null) javaAddr = "\"0.0.0.0:25565\"";
        String bedrockEnabled = Toml.grabSection(t, "networking.bedrock", "enabled");
        boolean bedrockOn = !"false".equals(bedrockEnabled);
        String bedrockAddr = Toml.grabSection(t, "networking.bedrock.nethernet", "address");
        if (bedrockAddr == null) bedrockAddr = "\"0.0.0.0:19132\"";

        JSONObject o = new JSONObject();
        J.p(o, "ok", true);
        J.p(o, "name", st.name());
        J.p(o, "running", !procs.isEmpty());
        J.p(o, "managed", proc != null);
        JSONArray pids = new JSONArray();
        for (ProcInfo p : procs) J.a(pids, p.pid);
        J.p(o, "pids", pids);
        J.p(o, "startedAt", startedAt);
        J.p(o, "uptime", startedAt > 0 ? Math.round((System.currentTimeMillis() - startedAt) / 1000.0) : 0);
        J.p(o, "cpu", sm[0]);
        J.p(o, "mem", sm[1]);

        JSONObject pl = new JSONObject();
        JSONArray plist = new JSONArray();
        synchronized (players) {
            J.p(pl, "count", players.size());
            for (String n : players) J.a(plist, n);
        }
        J.p(pl, "list", plist);
        J.p(o, "players", pl);

        J.p(o, "version", version);
        J.p(o, "worldMB", worldMb);
        JSONObject w = new JSONObject();
        J.p(w, "name", levelName());
        J.p(w, "seed", seed());
        J.p(o, "world", w);
        J.p(o, "ip", Fs.lanIP());
        J.p(o, "ips", Fs.ips());

        JSONObject ports = new JSONObject();
        J.p(ports, "java", portOf(javaAddr, 25565));
        J.p(ports, "bedrock", portOf(bedrockAddr, 19132));
        J.p(ports, "bedrockEnabled", bedrockOn);
        J.p(o, "ports", ports);
        J.p(o, "motd", Toml.unquote(Toml.grabSection(t, "networking.java", "motd")));
        J.p(o, "maxPlayers", Toml.parseIntSafe(Toml.grabSection(t, "networking.java", "max_players"), 0));

        JSONObject info = new JSONObject();
        J.p(info, "motdJava", Toml.unquote(Toml.grabSection(t, "networking.java", "motd")));
        J.p(info, "motdBedrock", Toml.unquote(Toml.grabSection(t, "networking.bedrock", "motd")));
        J.p(info, "seed", seed());
        J.p(info, "worldName", levelName());
        J.p(info, "difficulty", Toml.unquote(Toml.grabRoot(t, "default_difficulty")));
        J.p(info, "gamemode", Toml.unquote(Toml.grabRoot(t, "default_gamemode")));
        J.p(info, "hardcore", "true".equals(Toml.grabRoot(t, "hardcore")));
        J.p(info, "allowNether", "true".equals(Toml.grabRoot(t, "allow_nether")));
        J.p(info, "allowEnd", "true".equals(Toml.grabRoot(t, "allow_end")));
        J.p(info, "whiteList", "true".equals(Toml.grabRoot(t, "white_list")));
        J.p(info, "spawnProtection", Toml.parseIntSafe(Toml.grabRoot(t, "spawn_protection"), 0));
        J.p(info, "maxPlayersJava", Toml.parseIntSafe(Toml.grabSection(t, "networking.java", "max_players"), 0));
        J.p(info, "maxPlayersBedrock", Toml.parseIntSafe(Toml.grabSection(t, "networking.bedrock", "max_players"), 0));
        J.p(info, "viewDistanceJava", Toml.parseIntSafe(Toml.grabSection(t, "networking.java", "view_distance"), 0));
        J.p(info, "simDistanceJava", Toml.parseIntSafe(Toml.grabSection(t, "networking.java", "simulation_distance"), 0));
        J.p(info, "viewDistanceBedrock", Toml.parseIntSafe(Toml.grabSection(t, "networking.bedrock", "view_distance"), 0));
        J.p(info, "simDistanceBedrock", Toml.parseIntSafe(Toml.grabSection(t, "networking.bedrock", "simulation_distance"), 0));
        J.p(info, "onlineModeJava", "true".equals(Toml.grabSection(t, "networking.java", "online_mode")));
        J.p(info, "onlineModeBedrock", "true".equals(Toml.grabSection(t, "networking.bedrock", "online_mode")));
        J.p(info, "pvp", "true".equals(Toml.grabSection(t, "pvp", "enabled")));
        J.p(info, "bedrockEnabled", bedrockOn);
        String javaEnabled = Toml.grabSection(t, "networking.java", "enabled");
        J.p(info, "javaEnabled", !"false".equals(javaEnabled));
        J.p(o, "info", info);

        J.p(o, "configPath", st.tomlFile().getAbsolutePath());

        JSONArray ms = new JSONArray();
        synchronized (metrics) {
            int from = Math.max(0, metrics.size() - 60);
            for (int i = from; i < metrics.size(); i++) J.a(ms, metrics.get(i));
        }
        J.p(o, "metrics", ms);

        JSONArray last = new JSONArray();
        synchronized (buffer) {
            int from = Math.max(0, buffer.size() - 7);
            for (int i = from; i < buffer.size(); i++) J.a(last, buffer.get(i));
        }
        J.p(o, "logs", last);

        JSONObject panel = new JSONObject();
        J.p(panel, "port", st.port());
        J.p(panel, "host", st.host());
        J.p(panel, "version", 1);
        J.p(o, "panel", panel);

        if (System.currentTimeMillis() - worldMbAt > 120000) refreshWorldSizeAsync();
        return o;
    }

    private static int portOf(String addr, int def) {
        if (addr == null) return def;
        Matcher m = Pattern.compile(":(\\d+)\\s*$").matcher(addr.replace("\"", "").replace("'", ""));
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    /* ----------------------------------------------------------------- logs */

    JSONObject logs(int limit) {
        int max = Math.max(1, Math.min(limit, Math.max(50, st.maxLogLines())));
        JSONArray lines = new JSONArray();
        synchronized (buffer) {
            int from = Math.max(0, buffer.size() - max);
            for (int i = from; i < buffer.size(); i++) J.a(lines, buffer.get(i));
        }
        if (lines.length() == 0) {
            try {
                String raw = Fs.readText(st.logsFile());
                String[] all = raw.split("\n");
                int from = Math.max(0, all.length - max);
                for (int i = from; i < all.length; i++) {
                    String line = Fs.stripAnsi(all[i]).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = splitLogLine(line);
                    JSONObject e = new JSONObject();
                    J.p(e, "id", 0);
                    J.p(e, "time", parts[0]);
                    J.p(e, "level", parts[1]);
                    J.p(e, "msg", parts[2]);
                    J.p(e, "src", "file");
                    J.p(e, "ts", 0);
                    J.a(lines, e);
                }
            } catch (Exception ignored) {
            }
        }
        JSONObject o = new JSONObject();
        J.p(o, "ok", true);
        J.p(o, "lines", lines);
        J.p(o, "total", seq);
        return o;
    }

    /* -------------------------------------------------------------- player */

    JSONObject players() {
        JSONArray ops = Fs.jsonArray(st.dataFile("ops.json"));
        JSONArray white = Fs.jsonArray(st.dataFile("whitelist.json"));
        JSONArray cache = Fs.jsonArray(st.dataFile("usercache.json"));
        JSONArray bans = Fs.jsonArray(st.dataFile("banned-players.json"));

        JSONArray online = new JSONArray();
        synchronized (players) {
            for (String n : players) J.a(online, n);
        }
        JSONArray known = new JSONArray();
        for (int i = 0; i < cache.length(); i++) {
            JSONObject c = cache.optJSONObject(i);
            if (c == null) continue;
            JSONObject k = new JSONObject();
            J.p(k, "name", J.str(c, "name", ""));
            J.p(k, "uuid", J.str(c, "uuid", ""));
            J.a(known, k);
        }

        String t = toml();
        boolean wl = "true".equals(Toml.grabRoot(t, "white_list"));
        boolean enf = "true".equals(Toml.grabRoot(t, "enforce_whitelist"));
        JSONObject flags = new JSONObject();
        J.p(flags, "whiteList", wl);
        J.p(flags, "enforceWhitelist", enf);
        J.p(flags, "whitelistActive", wl || enf);

        JSONObject o = new JSONObject();
        J.p(o, "ok", true);
        J.p(o, "online", online);
        J.p(o, "ops", ops);
        J.p(o, "whitelist", white);
        J.p(o, "usercache", cache);
        J.p(o, "bans", bans);
        J.p(o, "known", known);
        J.p(o, "flags", flags);
        J.p(o, "ips", Fs.ips());
        return o;
    }

    private String lookupUUID(String name) {
        JSONArray cache = Fs.jsonArray(st.dataFile("usercache.json"));
        for (int i = 0; i < cache.length(); i++) {
            JSONObject c = cache.optJSONObject(i);
            if (c == null) continue;
            if (J.str(c, "name", "").equalsIgnoreCase(name)) return J.str(c, "uuid", Fs.offlineUUID(name));
        }
        return Fs.offlineUUID(name);
    }

    JSONObject op(String name, int level) {
        File f = st.dataFile("ops.json");
        JSONArray ops = Fs.jsonArray(f);
        for (int i = 0; i < ops.length(); i++) {
            JSONObject o = ops.optJSONObject(i);
            if (o != null && J.str(o, "name", "").equalsIgnoreCase(name)) {
                return res(true, "Sudah OP");
            }
        }
        JSONObject entry = new JSONObject();
        J.p(entry, "uuid", lookupUUID(name));
        J.p(entry, "name", name);
        J.p(entry, "level", level <= 0 ? 4 : level);
        J.p(entry, "bypassesPlayerLimit", false);
        J.a(ops, entry);
        try {
            Fs.writeJson(f, ops);
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
        boolean live = rawCommand("op " + name);
        JSONObject o = res(true, live ? "" : "Disimpan; berlaku setelah server jalan");
        J.p(o, "live", live);
        return o;
    }

    JSONObject deop(String name) {
        File f = st.dataFile("ops.json");
        JSONArray ops = Fs.jsonArray(f);
        JSONArray next = new JSONArray();
        for (int i = 0; i < ops.length(); i++) {
            JSONObject o = ops.optJSONObject(i);
            if (o == null) continue;
            if (J.str(o, "name", "").equalsIgnoreCase(name)) continue;
            J.a(next, o);
        }
        try {
            Fs.writeJson(f, next);
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
        boolean live = rawCommand("deop " + name);
        JSONObject o = res(true, "");
        J.p(o, "live", live);
        return o;
    }

    JSONObject whitelistAdd(String name) {
        File f = st.dataFile("whitelist.json");
        JSONArray wl = Fs.jsonArray(f);
        boolean found = false;
        for (int i = 0; i < wl.length(); i++) {
            JSONObject o = wl.optJSONObject(i);
            if (o != null && J.str(o, "name", "").equalsIgnoreCase(name)) found = true;
        }
        if (!found) {
            JSONObject entry = new JSONObject();
            J.p(entry, "uuid", lookupUUID(name));
            J.p(entry, "name", name);
            J.a(wl, entry);
            try {
                Fs.writeJson(f, wl);
            } catch (Exception e) {
                return res(false, e.getMessage());
            }
        }
        return res(true, "");
    }

    JSONObject whitelistRemove(String name) {
        File f = st.dataFile("whitelist.json");
        JSONArray wl = Fs.jsonArray(f);
        JSONArray next = new JSONArray();
        for (int i = 0; i < wl.length(); i++) {
            JSONObject o = wl.optJSONObject(i);
            if (o == null) continue;
            if (J.str(o, "name", "").equalsIgnoreCase(name)) continue;
            J.a(next, o);
        }
        try {
            Fs.writeJson(f, next);
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
        return res(true, "");
    }

    JSONObject ban(String name, String reason) {
        File f = st.dataFile("banned-players.json");
        JSONArray bans = Fs.jsonArray(f);
        boolean found = false;
        for (int i = 0; i < bans.length(); i++) {
            JSONObject o = bans.optJSONObject(i);
            if (o != null && J.str(o, "name", "").equalsIgnoreCase(name)) found = true;
        }
        if (!found) {
            JSONObject b = new JSONObject();
            J.p(b, "uuid", lookupUUID(name));
            J.p(b, "name", name);
            J.p(b, "created", Fs.isoNow());
            J.p(b, "source", "Panel");
            J.p(b, "expires", "forever");
            J.p(b, "reason", reason);
            J.a(bans, b);
            try {
                Fs.writeJson(f, bans);
            } catch (Exception e) {
                return res(false, e.getMessage());
            }
        }
        boolean live = rawCommand("ban " + name + " " + reason);
        JSONObject o = res(true, live ? "" : "Masuk daftar ban; berlaku setelah server jalan");
        J.p(o, "live", live);
        return o;
    }

    JSONObject unban(String name) {
        File f = st.dataFile("banned-players.json");
        JSONArray bans = Fs.jsonArray(f);
        JSONArray next = new JSONArray();
        for (int i = 0; i < bans.length(); i++) {
            JSONObject o = bans.optJSONObject(i);
            if (o == null) continue;
            if (J.str(o, "name", "").equalsIgnoreCase(name)) continue;
            J.a(next, o);
        }
        try {
            Fs.writeJson(f, next);
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
        boolean live = rawCommand("pardon " + name);
        JSONObject o = res(true, live ? "" : "Dihapus; berlaku setelah server jalan");
        J.p(o, "live", live);
        return o;
    }

    JSONObject whitelistMode(boolean on) {
        String cur = toml();
        if (!cur.isEmpty()) {
            String next = Toml.set(cur, "white_list", on);
            next = Toml.set(next, "enforce_whitelist", on);
            try {
                Fs.writeText(new File(st.tomlFile().getAbsolutePath() + ".bak"), cur);
                Fs.writeText(st.tomlFile(), next);
            } catch (Exception ignored) {
            }
        }
        boolean live = rawCommand("whitelist " + (on ? "on" : "off"));
        JSONObject o = res(true, live ? "" : "Disimpan di pumpkin.toml; aktif setelah server dijalankan");
        J.p(o, "enabled", on);
        J.p(o, "live", live);
        return o;
    }

    /* ---------------------------------------------------------------- world */

    JSONObject worlds() {
        String active = levelName();
        File base = st.serverDirFile();
        List<String> names = new ArrayList<>();
        File[] kids = base.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (k.isDirectory() && new File(k, "level.dat").isFile()) names.add(k.getName());
            }
        }
        if (!names.contains(active) && new File(new File(base, active), "level.dat").isFile()) {
            names.add(0, active);
        }
        Collections.sort(names, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                boolean aa = a.equals(active);
                boolean bb = b.equals(active);
                if (aa != bb) return aa ? -1 : 1;
                return a.compareToIgnoreCase(b);
            }
        });

        JSONArray arr = new JSONArray();
        for (String n : names) {
            JSONObject o = new JSONObject();
            J.p(o, "name", n);
            J.p(o, "at", new File(base, n).lastModified());
            J.p(o, "mb", n.equals(active) ? worldMb : JSONObject.NULL);
            J.p(o, "active", n.equals(active));
            J.a(arr, o);
        }
        JSONObject out = new JSONObject();
        J.p(out, "ok", true);
        J.p(out, "active", active);
        J.p(out, "seed", seed());
        J.p(out, "worlds", arr);
        return out;
    }

    private boolean writeSeed(String value) {
        String cur = toml();
        if (cur.isEmpty()) return false;
        String next = Toml.set(cur, "seed", value);
        try {
            Fs.writeText(new File(st.tomlFile().getAbsolutePath() + ".bak"), cur);
            Fs.writeText(st.tomlFile(), next);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean worldExists(String name) {
        return Fs.validWorldName(name) && new File(new File(st.serverDirFile(), name), "level.dat").isFile();
    }

    JSONObject seedPost(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.length() > 64) return res(false, "Seed tidak valid");
        if (!writeSeed(v)) return res(false, "pumpkin.toml tidak ditemukan");
        pushLog("[panel] seed diubah: " + v + " (berlaku untuk world baru)", "panel");
        JSONObject o = res(true, "Seed disimpan; berlaku saat world dibuat ulang");
        J.p(o, "seed", v);
        return o;
    }

    JSONObject worldDelete(String name) {
        if (!procList().isEmpty()) return res(false, "Hentikan server dulu sebelum menghapus world");
        if (!worldExists(name)) return res(false, "World tidak ditemukan");
        Fs.deleteRecursive(new File(st.serverDirFile(), name));
        pushLog("[panel] world \"" + name + "\" dihapus", "panel");
        refreshWorldSizeAsync();
        return res(true, "World " + name + " dihapus");
    }

    JSONObject worldRegenerate(String name, String seedValue) {
        if (!procList().isEmpty()) return res(false, "Hentikan server dulu sebelum membuat ulang world");
        if (!worldExists(name)) return res(false, "World tidak ditemukan");
        String v = seedValue == null ? "" : seedValue.trim();
        if (v.length() > 64) return res(false, "Seed tidak valid");
        Fs.deleteRecursive(new File(st.serverDirFile(), name));
        if (!v.isEmpty()) writeSeed(v);
        pushLog("[panel] world \"" + name + "\" dibuat ulang" + (v.isEmpty() ? "" : " (seed " + v + ")"), "panel");
        refreshWorldSizeAsync();
        return res(true, "World dihapus; akan dibuat baru saat server dijalankan");
    }

    JSONObject worldRename(String from, String to) {
        if (!procList().isEmpty()) return res(false, "Hentikan server dulu sebelum mengganti nama world");
        if (!worldExists(from)) return res(false, "World asal tidak ditemukan");
        if (!Fs.validWorldName(to)) return res(false, "Nama world tidak valid");
        if (from.equals(to)) return res(true, "");
        File dst = new File(st.serverDirFile(), to);
        if (dst.exists()) return res(false, "Nama world sudah dipakai");
        File src = new File(st.serverDirFile(), from);
        if (!src.renameTo(dst)) return res(false, "Gagal mengganti nama folder world");
        if (levelName().equals(from)) {
            String cur = toml();
            if (!cur.isEmpty()) {
                try {
                    Fs.writeText(new File(st.tomlFile().getAbsolutePath() + ".bak"), cur);
                    Fs.writeText(st.tomlFile(), Toml.set(cur, "default_level_name", to));
                } catch (Exception ignored) {
                }
            }
        }
        pushLog("[panel] world \"" + from + "\" diganti nama menjadi \"" + to + "\"", "panel");
        refreshWorldSizeAsync();
        return res(true, "Nama world diubah");
    }

    /* --------------------------------------------------------------- backup */

    JSONObject backups() {
        JSONArray arr = new JSONArray();
        File[] files = st.backupDirFile().listFiles();
        List<File> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".tar.gz")) list.add(f);
            }
        }
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        for (File f : list) {
            JSONObject o = new JSONObject();
            J.p(o, "name", f.getName());
            J.p(o, "sizeMB", Math.round(f.length() / 1048576.0));
            J.p(o, "at", f.lastModified());
            J.a(arr, o);
        }
        JSONObject out = new JSONObject();
        J.p(out, "ok", true);
        J.p(out, "backups", arr);
        J.p(out, "running", backupJob == null ? JSONObject.NULL : backupJob);
        return out;
    }

    JSONObject startBackup() {
        if (backupJob != null) return res(false, "Backup sedang berjalan");
        File dir = st.backupDirFile();
        if (!dir.exists() && !dir.mkdirs()) return res(false, "Folder backup tidak bisa dibuat");

        final String name = "world-" + Fs.backupStamp() + ".tar.gz";
        final File target = new File(dir, name);
        final String level = levelName();

        rawCommand("save-all");

        JSONObject job = new JSONObject();
        J.p(job, "name", name);
        J.p(job, "startedAt", System.currentTimeMillis());
        backupJob = job;

        JSONObject ev = new JSONObject();
        J.p(ev, "running", true);
        J.p(ev, "name", name);
        broadcast("backup", ev);
        pushLog("[panel] membuat backup " + name + " ...", "panel");

        pool.execute(() -> {
            String error = null;
            try {
                TarGz.create(st.serverDirFile(), level, target);
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            boolean ok = error == null;
            if (ok) {
                pushLog("[panel] backup selesai: " + name, "panel");
            } else {
                pushLog("[panel] backup gagal: " + error, "panel");
                //noinspection ResultOfMethodCallIgnored
                target.delete();
            }
            backupJob = null;
            JSONObject done = new JSONObject();
            J.p(done, "running", false);
            J.p(done, "ok", ok);
            J.p(done, "name", name);
            if (!ok) J.p(done, "error", error);
            broadcast("backup", done);
        });

        JSONObject o = res(true, "");
        J.p(o, "name", name);
        return o;
    }

    JSONObject deleteBackup(String name) {
        String base = Fs.safeBasename(name);
        if (!base.endsWith(".tar.gz")) return res(false, "Nama tidak valid");
        File f = new File(st.backupDirFile(), base);
        if (!f.isFile()) return res(false, "Backup tidak ditemukan");
        return f.delete() ? res(true, "") : res(false, "Gagal menghapus backup");
    }

    /* --------------------------------------------------------------- config */

    JSONObject configGet() {
        JSONObject o = new JSONObject();
        J.p(o, "ok", true);
        J.p(o, "toml", toml());
        J.p(o, "path", st.tomlFile().getAbsolutePath());
        J.p(o, "exists", st.tomlFile().isFile());
        return o;
    }

    JSONObject configPut(JSONObject data) {
        String current = toml();
        if (current.isEmpty() && (data == null || !data.has("toml"))) {
            return res(false, "pumpkin.toml belum ada. Jalankan server sekali agar file konfigurasi dibuat.");
        }
        String next = current;
        if (data != null && data.has("toml") && !data.isNull("toml")) {
            next = data.optString("toml", current);
        } else if (data != null && data.optJSONArray("patch") != null) {
            JSONArray patch = data.optJSONArray("patch");
            for (int i = 0; patch != null && i < patch.length(); i++) {
                JSONObject item = patch.optJSONObject(i);
                if (item == null) continue;
                String key = item.optString("key", "");
                if (key.isEmpty()) continue;
                next = Toml.set(next, key, item.opt("value"));
            }
        } else {
            return res(false, "Format tidak dikenal");
        }

        try {
            Fs.writeText(new File(st.tomlFile().getAbsolutePath() + ".bak"), current);
            Fs.writeText(st.tomlFile(), next);
            pushLog("[panel] pumpkin.toml disimpan (restart untuk menerapkan)", "panel");
            JSONObject o = res(true, "");
            J.p(o, "toml", next);
            J.p(o, "needRestart", true);
            return o;
        } catch (Exception e) {
            return res(false, e.getMessage());
        }
    }

    /* ------------------------------------------------------------- settings */

    JSONObject settingsGet() {
        JSONObject s = new JSONObject();
        J.p(s, "name", st.name());
        J.p(s, "port", st.port());
        J.p(s, "host", st.host());
        J.p(s, "serverDir", st.serverDir());
        J.p(s, "backupDir", st.backupDir());
        J.p(s, "maxLogLines", st.maxLogLines());
        J.p(s, "autoStart", st.autoStart());
        J.p(s, "autoRestart", st.autoRestart());
        J.p(s, "token", st.token());
        J.p(s, "panelOnline", st.panelOnline());
        J.p(s, "launcher", st.binFile().getAbsolutePath());
        J.p(s, "guestCmd", st.guestCmd());
        J.p(s, "binPath", st.binFile().getAbsolutePath());

        JSONObject paths = new JSONObject();
        J.p(paths, "config", new File(app.getFilesDir(), "panel.prefs").getAbsolutePath());
        J.p(paths, "backups", st.backupDir());
        J.p(paths, "home", "/sdcard");

        JSONObject rt = new JSONObject();
        J.p(rt, "node", "android-native");
        J.p(rt, "platform", "android");
        String[] abis = Build.SUPPORTED_ABIS;
        J.p(rt, "arch", abis != null && abis.length > 0 ? abis[0] : "arm64");
        J.p(rt, "uptime", Math.round((System.currentTimeMillis() - APP_START) / 1000.0));

        JSONObject o = new JSONObject();
        J.p(o, "ok", true);
        J.p(o, "settings", s);
        J.p(o, "paths", paths);
        J.p(o, "runtime", rt);
        return o;
    }

    /** Allow only hosts that actually belong to this device. */
    private static boolean isLocalBindHost(String h) {
        if ("0.0.0.0".equals(h) || "::".equals(h) || "localhost".equals(h)) return true;
        try {
            return NetworkInterface.getByInetAddress(InetAddress.getByName(h)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    JSONObject settingsPut(JSONObject d) {
        String oldHost = st.host();
        int oldPort = st.port();
        boolean oldOnline = st.panelOnline();

        int newPort = oldPort;
        boolean portGiven = d.has("port") && !d.isNull("port");
        if (portGiven) {
            newPort = d.optInt("port", 0);
            if (newPort < 1 || newPort > 65535) return res(false, "Port tidak valid (1-65535)");
        }

        String newHost = oldHost;
        boolean hostGiven = d.has("host") && !d.isNull("host");
        if (hostGiven) {
            newHost = d.optString("host", "").trim();
            if (newHost.isEmpty()) return res(false, "Host tidak boleh kosong");
            if (!isLocalBindHost(newHost)) {
                return res(false, "Host " + newHost + " bukan alamat perangkat ini");
            }
        }

        if (newPort != oldPort) {
            try (ServerSocket probe = new ServerSocket()) {
                probe.setReuseAddress(true);
                String bindHost = newHost.isEmpty() ? "0.0.0.0" : newHost;
                probe.bind(new InetSocketAddress(InetAddress.getByName(bindHost), newPort), 1);
            } catch (Exception e) {
                return res(false, "Port " + newPort + " tidak bisa dipakai: " + e.getMessage());
            }
        }

        // Private panel: a LAN host (including 0.0.0.0) must use a token.
        String tokenAfter = st.token();
        if (d.has("token")) {
            String tv = d.optString("token", "");
            if (!"***".equals(tv)) tokenAfter = tv;
        }
        boolean lanHost = !(newHost == null || newHost.isEmpty()
                || "127.0.0.1".equals(newHost) || "localhost".equals(newHost) || "::1".equals(newHost));
        if (lanHost && tokenAfter.trim().isEmpty()) {
            return res(false, "Isi token akses LAN dulu sebelum membuka panel ke jaringan.");
        }

        String[] strings = {"serverDir", "backupDir", "name", "guestCmd"};
        for (String k : strings) {
            if (!d.has(k) || d.isNull(k)) continue;
            String v = d.optString(k, "").trim();
            if (k.equals("serverDir") || k.equals("backupDir")) {
                if (v.startsWith("/")) st.put(k, v);
            } else {
                st.put(k, v);
            }
        }
        if (d.has("launcher") && !d.isNull("launcher")) {
            String v = d.optString("launcher", "").trim();
            if (v.startsWith("/")) st.put("binPath", v);
        }
        if (d.has("binPath") && !d.isNull("binPath")) {
            String v = d.optString("binPath", "").trim();
            if (v.startsWith("/")) st.put("binPath", v);
        }
        if (d.has("token")) {
            String v = d.optString("token", "").trim();
            // The UI must not clear the token; an empty token is regenerated automatically.
            if (!v.isEmpty() && !"***".equals(v)) st.put("token", v);
        }
        if (d.has("autoStart")) st.putBool("autoStart", d.optBoolean("autoStart", false));
        if (d.has("autoRestart")) st.putBool("autoRestart", d.optBoolean("autoRestart", false));
        if (d.has("panelOnline")) st.putBool("panelOnline", d.optBoolean("panelOnline", false));
        if (d.has("maxLogLines")) {
            int v = d.optInt("maxLogLines", 2500);
            if (v >= 50 && v <= 20000) st.putInt("maxLogLines", v);
        }
        if (portGiven) st.putInt("port", newPort);
        if (hostGiven) st.put("host", newHost);

        File dir = st.serverDirFile();
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        File bdir = st.backupDirFile();
        if (!bdir.exists()) //noinspection ResultOfMethodCallIgnored
            bdir.mkdirs();
        refreshWorldSizeAsync();

        boolean needRelisten = !oldHost.equals(st.host()) || oldPort != st.port() || oldOnline != st.panelOnline();
        if (needRelisten) {
            ApiServer a = api;
            if (a != null) a.relisten();
        }

        JSONArray urls = new JSONArray();
        JSONArray ips = Fs.ips();
        for (int i = 0; i < ips.length(); i++) {
            JSONObject ip = ips.optJSONObject(i);
            if (ip == null) continue;
            if (!"IPv4".equals(J.str(ip, "family", ""))) continue;
            JSONObject u = new JSONObject();
            J.p(u, "label", J.str(ip, "name", ""));
            J.p(u, "url", "http://" + J.str(ip, "address", "") + ":" + st.port());
            J.a(urls, u);
        }

        JSONObject out = settingsGet();
        J.p(out, "needRelisten", needRelisten);
        J.p(out, "urls", urls);
        return out;
    }
}
