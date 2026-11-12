package com.pumpkin.panel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/** Utilitas file, jaringan, dan teks. */
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

    /** Isi PT_INTERP ("" untuk binary statis atau file yang tidak terbaca). */
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

    /** ELF 64-bit little-endian, arm64, dan bukan build Linux/glibc. */
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

    /** Penjelasan singkat kenapa sebuah file bukan binary Android yang valid. */
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

    /* ---------------------------------------------------------------- teks */

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

    /* ------------------------------------------------------------ jaringan */

    private static volatile JSONArray ipCache;
    private static volatile long ipCacheAt;
    private static final long IP_TTL_MS = 5000;

    /**
     * Total byte jaringan (rx, tx) dari /proc/net/dev. Loopback dilewati supaya
     * angka mencerminkan trafik pemain, bukan polling panel itu sendiri.
     */
    static long[] netBytes() {
        long rx = 0, tx = 0;
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
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp()) continue;
                String name = ni.getName();
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    // Hanya IPv4: panel menampilkan alamat yang bisa dipakai pemain.
                    if (!(ia instanceof Inet4Address)) continue;
                    String addr = ia.getHostAddress();
                    if (addr == null || addr.isEmpty()) continue;
                    int pct = addr.indexOf('%');
                    if (pct > 0) addr = addr.substring(0, pct);
                    JSONObject o = new JSONObject();
                    J.p(o, "name", name);
                    J.p(o, "address", addr);
                    J.p(o, "family", "IPv4");
                    J.p(o, "internal", ia.isLoopbackAddress());
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
        if (!"IPv4".equals(family)) return 100 + (internal ? 9 : 5);
        if (internal) return 50;
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
