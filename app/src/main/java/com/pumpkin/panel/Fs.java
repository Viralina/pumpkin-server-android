package com.pumpkin.panel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

/** File, network, and text utilities. */
final class Fs {
    private Fs() {}

    private static final Pattern ANSI = Pattern.compile(
            "\u001B\\[[0-9;?]*[ -/]*[@-~]|\u001B\\][^\u0007]*\u0007|\u001B[@-_]");

    private static final Pattern WORLD_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

    /* ------------------------------------------------------------- file io */

    static byte[] readAll(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    static String readText(File f) throws IOException {
        return new String(readAll(f), "UTF-8");
    }

    static String readTextQuiet(File f) {
        try {
            return readText(f);
        } catch (Exception e) {
            return "";
        }
    }

    static void writeText(File f, String text) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(text.getBytes("UTF-8"));
            out.flush();
        }
    }

    static void writeBytes(File f, byte[] data) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
            out.flush();
        }
    }

    static void appendText(File f, String text) {
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                out.write(text.getBytes("UTF-8"));
                out.flush();
            }
        } catch (Exception ignored) {
        }
    }

    /* ---------------------------------------------------------- ELF binary */

    /** PT_INTERP contents ("" for static binaries or unreadable files). */
    static String elfInterpreter(File f) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            if (raf.length() < 64) return "";
            byte[] ident = new byte[6];
            raf.readFully(ident);
            if (ident[0] != 0x7f || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F') return "";
            raf.seek(0x20);
            long phoff = readLongLE(raf);
            raf.seek(0x36);
            int phentsize = readShortLE(raf);
            int phnum = readShortLE(raf);
            if (phentsize < 56 || phnum <= 0 || phnum > 128) return "";
            for (int i = 0; i < phnum; i++) {
                long off = phoff + (long) i * phentsize;
                if (off < 0 || off + 40 > raf.length()) break;
                raf.seek(off);
                int type = readIntLE(raf);
                if (type != 3) continue; // PT_INTERP
                raf.seek(off + 8);
                long pOffset = readLongLE(raf);
                raf.seek(off + 32);
                long pFilesz = readLongLE(raf);
                if (pFilesz <= 0 || pFilesz > 4096 || pOffset < 0 || pOffset + pFilesz > raf.length()) break;
                byte[] b = new byte[(int) pFilesz];
                raf.seek(pOffset);
                raf.readFully(b);
                String s = new String(b, "UTF-8");
                int end = s.indexOf('\0');
                return (end >= 0 ? s.substring(0, end) : s).trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** 64-bit little-endian arm64 ELF that is not a Linux/glibc build. */
    static boolean isAndroidArm64Elf(File f) {
        byte[] head = new byte[20];
        try (FileInputStream in = new FileInputStream(f)) {
            if (in.read(head) < 20) return false;
        } catch (Exception e) {
            return false;
        }
        if (head[0] != 0x7f || head[1] != 'E' || head[2] != 'L' || head[3] != 'F') return false;
        if (head[4] != 2 || head[5] != 1) return false;
        int machine = (head[18] & 0xff) | ((head[19] & 0xff) << 8);
        if (machine != 0xB7) return false;
        String interp = elfInterpreter(f);
        return interp.isEmpty() || interp.contains("linker64");
    }

    /** Short explanation of why a file is not a valid Android binary. */
    static String binaryProblem(File f) {
        if (!f.isFile()) return "file tidak ada";
        if (f.length() < 1024 * 1024) return "ukuran file terlalu kecil (" + f.length() + " byte)";
        byte[] head = new byte[20];
        try (FileInputStream in = new FileInputStream(f)) {
            if (in.read(head) < 20) return "file terlalu kecil";
        } catch (Exception e) {
            return "file tidak bisa dibaca: " + e.getMessage();
        }
        if (head[0] != 0x7f || head[1] != 'E' || head[2] != 'L' || head[3] != 'F') return "bukan file ELF";
        if (head[4] != 2 || head[5] != 1) return "bukan ELF 64-bit little-endian";
        int machine = (head[18] & 0xff) | ((head[19] & 0xff) << 8);
        if (machine != 0xB7) return "bukan arm64 (e_machine=" + machine + ")";
        String interp = elfInterpreter(f);
        if (!interp.isEmpty() && !interp.contains("linker64")) {
            return "build Linux/glibc (" + interp + "), bukan build Android";
        }
        return "";
    }

    private static long readLongLE(java.io.RandomAccessFile raf) throws IOException {
        long v = 0;
        for (int i = 0; i < 8; i++) v |= ((long) raf.readUnsignedByte()) << (8 * i);
        return v;
    }

    private static int readIntLE(java.io.RandomAccessFile raf) throws IOException {
        int v = 0;
        for (int i = 0; i < 4; i++) v |= raf.readUnsignedByte() << (8 * i);
        return v;
    }

    private static int readShortLE(java.io.RandomAccessFile raf) throws IOException {
        int v = 0;
        for (int i = 0; i < 2; i++) v |= raf.readUnsignedByte() << (8 * i);
        return v;
    }

    static JSONArray jsonArray(File f) {
        try {
            return new JSONArray(readText(f));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    static void writeJson(File f, JSONArray arr) throws IOException {
        try {
            writeText(f, arr.toString(2));
        } catch (Exception e) {
            writeText(f, "[]");
        }
    }

    static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) deleteRecursive(k);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    static long sizeOf(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long total = 0;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) total += sizeOf(k);
        }
        return total;
    }

    /* ----------------------------------------------------------------- text */

    static String stripAnsi(String s) {
        if (s == null) return "";
        return ANSI.matcher(s).replaceAll("").replace("\r", "");
    }

    static boolean validWorldName(String name) {
        return name != null && WORLD_NAME.matcher(name).matches();
    }

    static String safeBasename(String name) {
        if (name == null) return "";
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    static String isoNow() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    static String backupStamp() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US);
        return f.format(new Date());
    }

    static String offlineUUID(String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] h = md.digest(("OfflinePlayer:" + name).getBytes("UTF-8"));
            h[6] = (byte) ((h[6] & 0x0f) | 0x30);
            h[8] = (byte) ((h[8] & 0x3f) | 0x80);
            StringBuilder sb = new StringBuilder(36);
            for (int i = 0; i < 16; i++) {
                if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-');
                sb.append(String.format(Locale.US, "%02x", h[i] & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return name;
        }
    }

    /* ------------------------------------------------------------- network */

    private static volatile JSONArray ipCache;
    private static volatile long ipCacheAt;
    private static final long IP_TTL_MS = 5000;

    private static Context appCtx;

    /** Needs the app context for carrier names and the default internet network. */
    static void init(Context ctx) {
        if (ctx != null) appCtx = ctx.getApplicationContext();
    }

    /** Wi-Fi / carrier name for a label; raw interface names are never shown. */
    private static String labelFor(String name, String carrier) {
        String n = name == null ? "" : name.toLowerCase(Locale.US);
        if (n.startsWith("ap") || n.startsWith("wlan") || n.startsWith("wifi") || n.startsWith("swlan"))
            return "Wi-Fi";
        if (n.startsWith("ccmni") || n.startsWith("rmnet") || n.startsWith("wwan")
                || n.startsWith("pdp") || n.startsWith("data") || n.startsWith("cell"))
            return carrier == null || carrier.isEmpty() ? "Data seluler" : carrier;
        if (n.startsWith("eth")) return "Ethernet";
        if (n.startsWith("tun") || n.startsWith("ppp") || n.startsWith("wg")) return "VPN";
        return "Jaringan";
    }

    private static TelephonyManager telephony() {
        Context c = appCtx;
        if (c == null) return null;
        try {
            return (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Trim a name reported by the system; empty when unknown. */
    private static String cleanName(String s) {
        if (s == null) return "";
        s = s.replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > 32 ? s.substring(0, 32).trim() : s;
    }

    /** SIM / network operator name as reported by Android (works in any country). */
    private static String carrierName() {
        TelephonyManager tm = telephony();
        if (tm == null) return "";
        try {
            String s = cleanName(tm.getSimOperatorName());
            if (s.isEmpty()) s = cleanName(tm.getNetworkOperatorName());
            return s;
        } catch (Throwable t) {
            return "";
        }
    }

    /** Carrier for one SIM slot (dual-SIM data connections). */
    private static String subCarrier(int subId) {
        TelephonyManager tm = telephony();
        if (tm == null || subId < 0) return "";
        try {
            TelephonyManager s = tm.createForSubscriptionId(subId);
            String n = cleanName(s.getSimOperatorName());
            if (n.isEmpty()) n = cleanName(s.getNetworkOperatorName());
            return n;
        } catch (Throwable t) {
            return "";
        }
    }

    /** SIM slot behind a cellular network (API 30+, reflection so older devices stay safe). */
    private static String subCarrierOf(NetworkCapabilities nc) {
        if (android.os.Build.VERSION.SDK_INT < 29) return "";
        try {
            Object info = nc.getTransportInfo();
            if (info == null) return "";
            Class<?> cls = Class.forName("android.telephony.TelephonyNetworkSpecifier");
            if (!cls.isInstance(info)) return "";
            Object id = cls.getMethod("getSubscriptionId").invoke(info);
            return id instanceof Integer ? subCarrier((Integer) id) : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /** Interface name -> label ("Wi-Fi", SIM/operator name, "Ethernet"). */
    private static Map<String, String> ifaceLabels() {
        Map<String, String> out = new HashMap<>();
        Context c = appCtx;
        if (c == null) return out;
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return out;
            String carrier = carrierName();
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                LinkProperties lp = cm.getLinkProperties(n);
                if (nc == null || lp == null) continue;
                String ifn = lp.getInterfaceName();
                if (ifn == null || ifn.isEmpty()) continue;
                String car = carrier;
                String sub = subCarrierOf(nc);
                if (!sub.isEmpty()) car = sub;
                String lbl = null;
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) lbl = "Wi-Fi";
                else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    lbl = car.isEmpty() ? "Data seluler" : car;
                else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) lbl = "Ethernet";
                else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) lbl = "VPN";
                if (lbl != null) out.put(ifn, lbl);
            }
            /* Two SIMs with the same operator name: keep them distinguishable. */
            Map<String, Integer> seen = new HashMap<>();
            for (Map.Entry<String, String> e : out.entrySet()) {
                String lbl = e.getValue();
                Integer n = seen.get(lbl);
                if (n == null) seen.put(lbl, 1);
                else {
                    seen.put(lbl, n + 1);
                    e.setValue(lbl + " " + (n + 1));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** Addresses of the validated default network (the one that actually reaches the internet). */
    private static Set<String> internetAddrs() {
        Set<String> out = new HashSet<>();
        Context c = appCtx;
        if (c == null) return out;
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return out;
            Network n = cm.getActiveNetwork();
            if (n == null) return out;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if (nc == null || !nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return out;
            LinkProperties lp = cm.getLinkProperties(n);
            if (lp == null) return out;
            for (LinkAddress la : lp.getLinkAddresses()) {
                String a = la.getAddress().getHostAddress();
                int pct = a.indexOf('%');
                if (pct > 0) a = a.substring(0, pct);
                out.add(a);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /**
     * Traffic of this app and its Pumpkin child process (same UID): rx, tx since boot.
     * Per-UID counters are monotonic, so they do not jump when an interface appears
     * (a device-wide sum did, which looked like a traffic spike). Other apps' traffic
     * is not counted. Falls back to per-interface totals only when unsupported.
     */
    static long[] netBytes() {
        int uid = android.os.Process.myUid();
        try {
            long ur = android.net.TrafficStats.getUidRxBytes(uid);
            long ut = android.net.TrafficStats.getUidTxBytes(uid);
            if (ur != android.net.TrafficStats.UNSUPPORTED && ut != android.net.TrafficStats.UNSUPPORTED) {
                return new long[]{Math.max(0, ur), Math.max(0, ut)};
            }
        } catch (Throwable ignored) {
        }

        long rx = 0, tx = 0;
        boolean any = false;
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                String name = ni.getName();
                long r = android.net.TrafficStats.getRxBytes(name);
                long t = android.net.TrafficStats.getTxBytes(name);
                if (r != android.net.TrafficStats.UNSUPPORTED && t != android.net.TrafficStats.UNSUPPORTED) {
                    rx += Math.max(0, r);
                    tx += Math.max(0, t);
                    any = true;
                }
            }
        } catch (Throwable ignored) {
        }
        if (any && (rx > 0 || tx > 0)) return new long[]{rx, tx};

        /* Fallback: system-wide counters (includes loopback, but still better than zero). */
        try {
            long r = android.net.TrafficStats.getTotalRxBytes();
            long t = android.net.TrafficStats.getTotalTxBytes();
            if (r != android.net.TrafficStats.UNSUPPORTED && t != android.net.TrafficStats.UNSUPPORTED
                    && (r > 0 || t > 0)) {
                return new long[]{r, t};
            }
        } catch (Throwable ignored) {
        }

        /* Last resort for old/rooted devices where /proc/net/dev is readable. */
        rx = 0;
        tx = 0;
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("/proc/net/dev"))) {
            String line;
            while ((line = r.readLine()) != null) {
                int c = line.indexOf(':');
                if (c < 0) continue;
                String name = line.substring(0, c).trim();
                if (name.isEmpty() || "lo".equals(name)) continue;
                String[] p = line.substring(c + 1).trim().split("\\s+");
                if (p.length < 9) continue;
                try {
                    rx += Long.parseLong(p[0]);
                    tx += Long.parseLong(p[8]);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return new long[]{rx, tx};
    }

    static JSONArray ips() {
        JSONArray cached = ipCache;
        if (cached != null && System.currentTimeMillis() - ipCacheAt < IP_TTL_MS) return cached;
        List<JSONObject> list = new ArrayList<>();
        Map<String, String> labels = ifaceLabels();
        Set<String> inet = internetAddrs();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp()) continue;
                String name = ni.getName();
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    boolean v6 = ia instanceof Inet6Address;
                    // Show usable addresses: IPv4/IPv6, without link-local or multicast.
                    if (!(ia instanceof Inet4Address) && !v6) continue;
                    if (ia.isMulticastAddress()) continue;
                    if (v6 && ia.isLinkLocalAddress()) continue;
                    String addr = ia.getHostAddress();
                    if (addr == null || addr.isEmpty()) continue;
                    int pct = addr.indexOf('%');
                    if (pct > 0) addr = addr.substring(0, pct);
                    JSONObject o = new JSONObject();
                    J.p(o, "name", name);
                    J.p(o, "label", labels.containsKey(name) ? labels.get(name) : labelFor(name, ""));
                    J.p(o, "address", addr);
                    J.p(o, "family", v6 ? "IPv6" : "IPv4");
                    J.p(o, "internal", ia.isLoopbackAddress());
                    J.p(o, "net", inet.contains(addr));
                    list.add(o);
                }
            }
        } catch (Exception ignored) {
        }

        Collections.sort(list, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                int ra = rank(a);
                int rb = rank(b);
                if (ra != rb) return ra - rb;
                return J.str(a, "name", "").compareTo(J.str(b, "name", ""));
            }
        });

        JSONArray out = new JSONArray();
        for (JSONObject o : list) J.a(out, o);
        ipCache = out;
        ipCacheAt = System.currentTimeMillis();
        return out;
    }

    private static int rank(JSONObject o) {
        String family = J.str(o, "family", "IPv6");
        boolean internal = J.bool(o, "internal", false);
        String addr = J.str(o, "address", "");
        /* The address on the validated internet network always comes first. */
        if (J.bool(o, "net", false)) return -2;
        if (internal) return "IPv6".equals(family) ? 51 : 50;
        if (!"IPv4".equals(family)) {
            String a = addr.toLowerCase(Locale.US);
            return (a.startsWith("fc") || a.startsWith("fd")) ? 4 : 5;
        }
        if (addr.startsWith("192.168.")) return 0;
        if (addr.startsWith("10.")) return 1;
        if (addr.startsWith("172.16.") || addr.startsWith("172.17.") || addr.startsWith("172.18.")
                || addr.startsWith("172.19.") || addr.startsWith("172.2") || addr.startsWith("172.30.")
                || addr.startsWith("172.31.")) return 2;
        return 3;
    }

    static String lanIP() {
        JSONArray all = ips();
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            if ("IPv4".equals(J.str(o, "family", "")) && !J.bool(o, "internal", false)) {
                return J.str(o, "address", "127.0.0.1");
            }
        }
        return "127.0.0.1";
    }
}
