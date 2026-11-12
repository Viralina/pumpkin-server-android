package com.pumpkin.panel;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local HTTP API, a replacement for server.js. */
final class ApiServer {

    private final Context ctx;
    private final Store st;
    private final Pumpkin pk;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    /** SSE writes must not run on the main thread (NetworkOnMainThreadException). */
    private final ExecutorService tx = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sse-tx");
        t.setDaemon(true);
        return t;
    });
    private final List<Sse> clients = new CopyOnWriteArrayList<>();

    private volatile ServerSocket ss;
    private volatile boolean running;
    private volatile byte[] uiBytes;

    private static final class Sse {
        final OutputStream out;
        final Socket socket;

        Sse(OutputStream out, Socket socket) {
            this.out = out;
            this.socket = socket;
        }

        synchronized void send(String text) throws IOException {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    ApiServer(Context ctx, Store store, Pumpkin pumpkin) {
        this.ctx = ctx.getApplicationContext();
        this.st = store;
        this.pk = pumpkin;
    }

    /* ------------------------------------------------------------ lifecycle */

    synchronized void start() throws IOException {
        if (ss != null) closeListener();
        openListener();
        running = true;
        Thread t = new Thread(this::acceptLoop, "api-accept");
        t.setDaemon(true);
        t.start();
    }

    private void openListener() throws IOException {
        String host = st.host();
        int port = st.port();
        // Web panel online defaults to OFF: force loopback so it is not exposed to the LAN.
        if (!st.panelOnline()) host = "127.0.0.1";
        // Private panel: without a LAN token, force loopback even if an old setting used the LAN.
        String token = st.token();
        boolean hasToken = token != null && !token.trim().isEmpty();
        if (!hasToken && host != null && !host.isEmpty()
                && !"127.0.0.1".equals(host) && !"localhost".equals(host) && !"::1".equals(host)) {
            host = "127.0.0.1";
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        InetSocketAddress addr;
        if (host.isEmpty() || "0.0.0.0".equals(host)) {
            addr = new InetSocketAddress(port);
        } else {
            addr = new InetSocketAddress(InetAddress.getByName(host), port);
        }
        socket.bind(addr, 50);
        ss = socket;
    }

    private void closeListener() {
        running = false;
        ServerSocket s = ss;
        ss = null;
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    void relisten() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            synchronized (ApiServer.this) {
                closeListener();
                try {
                    openListener();
                    running = true;
                    Thread accept = new Thread(this::acceptLoop, "api-accept");
                    accept.setDaemon(true);
                    accept.start();
                    pk.pushLog("[panel] pindah listen ke " + st.host() + ":" + st.port(), "panel");
                } catch (Exception e) {
                    pk.pushLog("[panel] gagal listen: " + e.getMessage(), "panel");
                }
            }
        }, "api-relisten");
        t.setDaemon(true);
        t.start();
    }

    void stop() {
        closeListener();
        for (Sse c : clients) {
            try {
                c.socket.close();
            } catch (Exception ignored) {
            }
        }
        clients.clear();
        pool.shutdownNow();
        tx.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket s = ss.accept();
                pool.execute(() -> handle(s));
            } catch (Exception e) {
                if (!running) return;
            }
        }
    }

    /* ------------------------------------------------------------- broadcast */

    void broadcast(String event, JSONObject data) {
        if (clients.isEmpty()) return;
        final String payload = "event: " + event + "\ndata: " + data.toString() + "\n\n";
        try {
            tx.execute(() -> {
                for (Sse c : clients) {
                    try {
                        c.send(payload);
                    } catch (Exception e) {
                        pk.diag("SSE send err " + e);
                        clients.remove(c);
                        try {
                            c.socket.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        } catch (Exception ignored) {
            /* executor has already shut down when the service is destroyed */
        }
    }

    /* ---------------------------------------------------------------- request */

    private void handle(Socket s) {
        try {
            s.setSoTimeout(20000);
            InputStream in = s.getInputStream();
            String reqLine = readLine(in);
            if (reqLine == null || reqLine.isEmpty()) {
                s.close();
                return;
            }
            String[] parts = reqLine.split(" ");
            if (parts.length < 2) {
                s.close();
                return;
            }
            String method = parts[0];
            String target = parts[1];

            Map<String, String> headers = new HashMap<>();
            String line;
            int guard = 0;
            while ((line = readLine(in)) != null && !line.isEmpty() && guard++ < 120) {
                int c = line.indexOf(':');
                if (c > 0) {
                    headers.put(line.substring(0, c).trim().toLowerCase(Locale.US), line.substring(c + 1).trim());
                }
            }

            int len = 0;
            String cl = headers.get("content-length");
            if (cl != null) {
                try {
                    len = Integer.parseInt(cl.trim());
                } catch (Exception ignored) {
                }
            }
            byte[] body = new byte[0];
            if (len > 0) body = readN(in, Math.min(len, 4 * 1024 * 1024));

            String path = target;
            String query = "";
            int q = target.indexOf('?');
            if (q >= 0) {
                path = target.substring(0, q);
                query = target.substring(q + 1);
            }
            Map<String, String> params = parseQuery(query);

            route(s, method, path, params, headers, new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void route(Socket s, String method, String path, Map<String, String> params,
                       Map<String, String> headers, String body) throws IOException {
        if ("OPTIONS".equals(method)) {
            String head = "HTTP/1.1 204 No Content\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS\r\n"
                    + "Access-Control-Allow-Headers: content-type,x-token\r\n"
                    + "Access-Control-Max-Age: 86400\r\n"
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n\r\n";
            OutputStream out = s.getOutputStream();
            out.write(head.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            s.close();
            return;
        }

        if ("/".equals(path) || "/index.html".equals(path)) {
            // Private panel: only send the HTML to callers that know the token.
            if (!tokenOK(headers, params)) {
                serveLocked(s);
                return;
            }
            serveHtml(s);
            return;
        }
        if ("/favicon.ico".equals(path)) {
            send(s, 204, "text/plain", new byte[0]);
            return;
        }
        if (!path.startsWith("/api/")) {
            json(s, 404, J.obj("ok", false, "msg", "Tidak ditemukan"));
            return;
        }
        if (!tokenOK(headers, params)) {
            json(s, 401, J.obj("ok", false, "msg", "Token panel salah"));
            return;
        }

        JSONObject data = new JSONObject();
        if (body != null && !body.trim().isEmpty()) {
            try {
                data = new JSONObject(body);
            } catch (Exception e) {
                json(s, 400, J.obj("ok", false, "msg", "Body JSON tidak valid"));
                return;
            }
        }

        try {
            if ("/api/ping".equals(path)) {
                JSONObject o = J.obj("ok", true, "name", st.name());
                J.p(o, "panel", 1);
                J.p(o, "pid", android.os.Process.myPid());
                json(s, 200, o);
                return;
            }

            if ("/api/status".equals(path)) {
                json(s, 200, pk.status());
                return;
            }

            if ("/api/net".equals(path)) {
                long[] nb = Fs.netBytes();
                JSONObject o = new JSONObject();
                J.p(o, "ok", true);
                J.p(o, "rx", nb[0]);
                J.p(o, "tx", nb[1]);
                J.p(o, "ts", System.currentTimeMillis());
                json(s, 200, o);
                return;
            }

            if ("/api/logs".equals(path)) {
                int limit = 300;
                String l = params.get("limit");
                if (l != null) {
                    try {
                        limit = Integer.parseInt(l);
                    } catch (Exception ignored) {
                    }
                }
                json(s, 200, pk.logs(limit));
                return;
            }

            if ("/api/stream".equals(path)) {
                serveSse(s);
                return;
            }

            if ("/api/start".equals(path)) {
                json(s, 200, pk.start());
                return;
            }
            if ("/api/stop".equals(path)) {
                json(s, 200, pk.stop(false));
                return;
            }
            if ("/api/kill".equals(path)) {
                json(s, 200, pk.stop(true));
                return;
            }
            if ("/api/restart".equals(path)) {
                json(s, 200, pk.restart());
                return;
            }
            if ("/api/command".equals(path)) {
                json(s, 200, pk.command(data.optString("cmd", "")));
                return;
            }

            /* ---- config ---- */
            if ("/api/config".equals(path) && "GET".equals(method)) {
                json(s, 200, pk.configGet());
                return;
            }
            if ("/api/config".equals(path) && "PUT".equals(method)) {
                json(s, 200, pk.configPut(data));
                return;
            }

            /* ---- players ---- */
            if ("/api/players".equals(path) && "GET".equals(method)) {
                json(s, 200, pk.players());
                return;
            }
            if ("/api/players/op".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                json(s, 200, pk.op(name, data.optInt("level", 4)));
                return;
            }
            if ("/api/players/deop".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                json(s, 200, pk.deop(name));
                return;
            }
            if ("/api/players/whitelist-add".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                json(s, 200, pk.whitelistAdd(name));
                return;
            }
            if ("/api/players/whitelist-remove".equals(path)) {
                json(s, 200, pk.whitelistRemove(cleanName(data.optString("name", ""))));
                return;
            }
            if ("/api/players/kick".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                String reason = cleanReason(data.optString("reason", ""), "Dikeluarkan oleh admin");
                json(s, 200, pk.command("kick " + name + " " + reason));
                return;
            }
            if ("/api/players/ban".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                String reason = cleanReason(data.optString("reason", ""), "Diban oleh admin");
                json(s, 200, pk.ban(name, reason));
                return;
            }
            if ("/api/players/unban".equals(path)) {
                json(s, 200, pk.unban(cleanName(data.optString("name", ""))));
                return;
            }
            if ("/api/players/gamemode".equals(path)) {
                String name = cleanName(data.optString("name", ""));
                String mode = data.optString("mode", "").toLowerCase(Locale.US);
                if (name.isEmpty()) {
                    json(s, 400, J.obj("ok", false, "msg", "Nama kosong"));
                    return;
                }
                if (!"survival".equals(mode) && !"creative".equals(mode)
                        && !"adventure".equals(mode) && !"spectator".equals(mode)) {
                    json(s, 400, J.obj("ok", false, "msg", "Gamemode tidak valid"));
                    return;
                }
                json(s, 200, pk.command("gamemode " + mode + " " + name));
                return;
            }
            if ("/api/players/whitelist-mode".equals(path)) {
                boolean on = data.optBoolean("enabled", false) || "true".equals(data.optString("enabled", ""));
                json(s, 200, pk.whitelistMode(on));
                return;
            }

            /* ---- backup ---- */
            if ("/api/backups".equals(path) && "GET".equals(method)) {
                json(s, 200, pk.backups());
                return;
            }
            if ("/api/backup".equals(path) && "POST".equals(method)) {
                json(s, 200, pk.startBackup());
                return;
            }
            if ("/api/backup".equals(path) && "DELETE".equals(method)) {
                json(s, 200, pk.deleteBackup(data.optString("name", "")));
                return;
            }

            /* ---- world ---- */
            if ("/api/worlds".equals(path) && "GET".equals(method)) {
                json(s, 200, pk.worlds());
                return;
            }
            if ("/api/worlds/seed".equals(path)) {
                json(s, 200, pk.seedPost(data.optString("seed", "")));
                return;
            }
            if ("/api/worlds/delete".equals(path)) {
                json(s, 200, pk.worldDelete(data.optString("name", "")));
                return;
            }
            if ("/api/worlds/regenerate".equals(path)) {
                json(s, 200, pk.worldRegenerate(data.optString("name", ""), data.optString("seed", "")));
                return;
            }
            if ("/api/worlds/rename".equals(path)) {
                json(s, 200, pk.worldRename(cleanName(data.optString("from", "")), cleanName(data.optString("to", ""))));
                return;
            }

            /* ---- settings ---- */
            if ("/api/settings".equals(path) && "GET".equals(method)) {
                json(s, 200, pk.settingsGet());
                return;
            }
            if ("/api/settings".equals(path) && "PUT".equals(method)) {
                json(s, 200, pk.settingsPut(data));
                return;
            }

            json(s, 404, J.obj("ok", false, "msg", "Endpoint tidak dikenal"));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            json(s, 500, J.obj("ok", false, "msg", msg));
        }
    }

    private static String cleanReason(String v, String def) {
        String s = v == null ? "" : v.replace("\r", " ").replace("\n", " ").trim();
        return s.isEmpty() ? def : s;
    }

    private static String cleanName(String v) {
        if (v == null) return "";
        return v.replace("\r", "").replace("\n", "").replace("\t", "").trim();
    }

    private boolean authOK(Map<String, String> headers, Map<String, String> params) {
        String token = st.token();
        if (token == null || token.isEmpty()) return true;
        String h = headers.get("x-token");
        if (h == null) h = params.get("token");
        return token.equals(h);
    }

    private boolean isLocal(Socket s) {
        try {
            InetAddress a = s.getInetAddress();
            return a == null || a.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /** Token required for every panel request (including localhost). */
    private boolean tokenOK(Map<String, String> headers, Map<String, String> params) {
        String token = st.token();
        if (token == null || token.isEmpty()) return false;
        String h = headers.get("x-token");
        if (h == null) h = params.get("token");
        return token.equals(h);
    }

    /** Lock page: does not load the panel HTML at all. */
    private void serveLocked(Socket s) throws IOException {
        String html = "<!doctype html><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>Pumpkin Panel</title>"
                + "<body style=\"background:#07090d;color:#eef2f8;font-family:system-ui;padding:28px;line-height:1.55\">"
                + "<h2 style=\"margin:0 0 10px\">Panel privat</h2>"
                + "<p style=\"color:#98a6b8\">Token akses dibuat otomatis dan bisa dilihat di aplikasi: "
                + "<b>Pengaturan &rarr; Token akses panel &rarr; Salin token</b>. "
                + "Masukkan token untuk membuka panel.</p>"
                + "<form method=\"get\" style=\"margin-top:18px;display:flex;gap:8px\">"
                + "<input name=\"token\" type=\"password\" placeholder=\"Token\" "
                + "style=\"flex:1;padding:11px;border-radius:10px;border:1px solid #26303d;background:#0e131b;color:#eef2f8\">"
                + "<button style=\"padding:11px 16px;border-radius:10px;border:0;background:#ff8a3d;color:#111;font-weight:700\">Buka</button>"
                + "</form>"
                + "<p style=\"color:#66748a;font-size:12px;margin-top:14px\">Private panel. The access token is generated automatically; "
                + "copy it from the app under <b>Settings &rarr; Panel access token &rarr; Copy token</b>.</p>"
                + "</body>";
        send(s, 401, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
    }

    /* --------------------------------------------------------------- response */

    private void json(Socket s, int code, JSONObject obj) throws IOException {
        send(s, code, "application/json; charset=utf-8",
                obj.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void send(Socket s, int code, String type, byte[] body) throws IOException {
        OutputStream out = s.getOutputStream();
        String head = "HTTP/1.1 " + code + " " + reason(code) + "\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
        try {
            s.close();
        } catch (Exception ignored) {
        }
    }

    private static String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "OK";
        }
    }

    private void serveHtml(Socket s) throws IOException {
        byte[] body = uiBytes;
        if (body == null) {
            try (InputStream in = ctx.getAssets().open("index.html")) {
                body = readAll(in);
                uiBytes = body;
            } catch (Exception ignored) {
            }
        }
        if (body != null) {
            // The correct token is embedded in the page, so JS does not depend on ?token= in the URL.
            String tok = st.token();
            if (tok != null && !tok.isEmpty()) {
                String html = new String(body, StandardCharsets.UTF_8);
                String inject = "<script>window.__PP_TOKEN=" + JSONObject.quote(tok) + ";</script>";
                int at = html.indexOf("<head>");
                html = at >= 0 ? html.substring(0, at + 6) + inject + html.substring(at + 6)
                        : inject + html;
                body = html.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (body == null) {
            String msg = "<!doctype html><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<body style=\"background:#07090d;color:#eef2f8;font-family:system-ui;padding:24px\">"
                    + "<h2>UI not found</h2><p>The panel is bundled inside the APK; reinstall the app.</p></body>";
            body = msg.getBytes(StandardCharsets.UTF_8);
        }
        send(s, 200, "text/html; charset=utf-8", body);
    }

    private void serveSse(Socket s) {
        Sse client = null;
        try {
            s.setSoTimeout(0);
            OutputStream out = s.getOutputStream();
            String head = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/event-stream; charset=utf-8\r\n"
                    + "Cache-Control: no-cache\r\n"
                    + "Connection: keep-alive\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "X-Accel-Buffering: no\r\n\r\n";
            out.write(head.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            client = new Sse(out, s);
            JSONObject hello = new JSONObject();
            J.p(hello, "at", System.currentTimeMillis());
            client.send("event: hello\ndata: " + hello + "\n\n");
            client.send("event: status\ndata: " + pk.status() + "\n\n");
            clients.add(client);
            pk.diag("SSE open clients=" + clients.size());

            while (!s.isClosed()) {
                Thread.sleep(25000);
                client.send(": ping\n\n");
            }
        } catch (Exception e) {
            pk.diag("SSE err " + e);
        } finally {
            if (client != null) clients.remove(client);
            pk.diag("SSE close clients=" + clients.size() + (s.isClosed() ? " socket=closed" : " socket=open"));
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    /* -------------------------------------------------------------- plumbing */

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int c;
        boolean any = false;
        while ((c = in.read()) >= 0) {
            any = true;
            if (c == '\n') break;
            if (c != '\r') buf.write(c);
            if (buf.size() > 8192) break;
        }
        if (!any && buf.size() == 0) return null;
        return new String(buf.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static byte[] readN(InputStream in, int len) throws IOException {
        byte[] out = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(out, off, len - off);
            if (n < 0) break;
            off += n;
        }
        if (off == len) return out;
        byte[] cut = new byte[off];
        System.arraycopy(out, 0, cut, 0, off);
        return cut;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> out = new HashMap<>();
        if (query == null || query.isEmpty()) return out;
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            try {
                String k = eq >= 0 ? pair.substring(0, eq) : pair;
                String v = eq >= 0 ? pair.substring(eq + 1) : "";
                out.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /** Used for internal tests. */
    String baseUrl() {
        return "http://" + st.host() + ":" + st.port();
    }
}
