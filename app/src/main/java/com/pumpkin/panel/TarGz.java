package com.pumpkin.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Simple .tar.gz archive writer so backups do not depend on Android's
 * tar binary.
 */
final class TarGz {
    private TarGz() {}

    static void create(File baseDir, String entry, File target) throws IOException {
        List<String> one = new ArrayList<>();
        one.add(entry);
        createMany(baseDir, one, target);
    }

    /** Archives several top-level folders (all worlds) into one .tar.gz. */
    static void createMany(File baseDir, List<String> entries, File target) throws IOException {
        if (entries == null || entries.isEmpty()) throw new IOException("Tidak ada folder untuk dibackup");
        /* Write next to the target first so a failure never leaves a half file. */
        File part = new File(target.getAbsolutePath() + ".part");
        try {
            try (FileOutputStream fos = new FileOutputStream(part);
                 GZIPOutputStream gz = new GZIPOutputStream(fos, 64 * 1024)) {
                for (String entry : entries) {
                    File root = new File(baseDir, entry);
                    if (!root.exists()) throw new IOException("Folder tidak ditemukan: " + root);
                    add(gz, root, entry, true);
                }
                gz.write(new byte[1024]); // akhir arsip: dua blok nol
                gz.finish();
            }
            //noinspection ResultOfMethodCallIgnored
            if (!part.renameTo(target)) throw new IOException("Gagal menyelesaikan arsip backup");
        } finally {
            if (part.exists()) {
                //noinspection ResultOfMethodCallIgnored
                part.delete();
            }
        }
    }

    private static void add(OutputStream out, File f, String name, boolean top) throws IOException {
        String entryName = name.replace(File.separatorChar, '/');
        if (f.isDirectory()) {
            writeHeader(out, entryName + "/", 0, f.lastModified(), (byte) '5');
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) {
                    add(out, k, entryName + "/" + k.getName(), false);
                }
            }
        } else {
            writeHeader(out, entryName, f.length(), f.lastModified(), (byte) '0');
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                long left = f.length();
                while (left > 0) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) break;
                    out.write(buf, 0, n);
                    left -= n;
                }
            }
            int pad = (int) ((512 - (f.length() % 512)) % 512);
            if (pad > 0) out.write(new byte[pad]);
        }
        if (top) {
            // no extra action
        }
    }

    private static void writeHeader(OutputStream out, String name, long size, long mtime, byte type)
            throws IOException {
        byte[] h = new byte[512];
        String prefix = "";
        if (name.getBytes("UTF-8").length > 100) {
            int cut = name.lastIndexOf('/', 150);
            if (cut > 0 && cut < name.length() - 1) {
                prefix = name.substring(0, cut);
                name = name.substring(cut + 1);
            }
        }
        put(h, 0, 100, name);
        put(h, 100, 8, type == '5' ? "0000755" : "0000644");
        put(h, 108, 8, "0000000");
        put(h, 116, 8, "0000000");
        put(h, 124, 12, String.format("%011o", size));
        put(h, 136, 12, String.format("%011o", mtime / 1000));
        put(h, 148, 8, "        ");
        h[156] = type;
        put(h, 157, 100, "");
        put(h, 257, 8, "ustar");
        put(h, 263, 2, "00");
        put(h, 265, 32, "root");
        put(h, 297, 32, "root");
        put(h, 329, 8, "0000000");
        put(h, 337, 8, "0000000");
        put(h, 345, 155, prefix);

        int sum = 0;
        for (byte b : h) sum += (b & 0xff);
        put(h, 148, 8, String.format("%06o", sum) + "\u0000 ");
        out.write(h);
    }

    private static void put(byte[] buf, int off, int len, String value) {
        byte[] b;
        try {
            b = value.getBytes("UTF-8");
        } catch (Exception e) {
            b = value.getBytes();
        }
        int n = Math.min(len, b.length);
        System.arraycopy(b, 0, buf, off, n);
    }

    /* ------------------------------------------------------------- restore */

    /** Extracts a .tar.gz made by this class (or a normal world tar.gz) into destDir. */
    static void extract(File archive, File destDir) throws IOException {
        File dest = destDir.getCanonicalFile();
        if (!dest.exists() && !dest.mkdirs()) throw new IOException("Folder tujuan tidak bisa dibuat");
        try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(archive), 64 * 1024)) {
            byte[] h = new byte[512];
            String longName = null;
            while (true) {
                int n = readFully(gz, h, 512);
                if (n == 0) break;
                if (n < 512) throw new IOException("Arsip rusak");
                boolean zero = true;
                for (byte b : h) {
                    if (b != 0) { zero = false; break; }
                }
                if (zero) break;

                String name = str(h, 0, 100);
                String prefix = str(h, 345, 155);
                if (!prefix.isEmpty()) name = prefix + "/" + name;
                long size = oct(h, 124, 12);
                byte type = h[156];

                /* GNU/pax metadata entries carry the real name of the next entry. */
                if (type == 'L' || type == 'x') {
                    String meta = readText(gz, size);
                    skip(gz, (512 - (size % 512)) % 512);
                    if (type == 'L') {
                        longName = meta.trim();
                    } else {
                        longName = paxPath(meta);
                    }
                    continue;
                }
                if (type == 'K' || type == 'g' || type == 'V') {
                    skip(gz, size + (512 - (size % 512)) % 512);
                    continue;
                }
                if (longName != null) {
                    name = longName;
                    longName = null;
                }

                String clean = name.replace('\\', '/').replaceAll("^/+", "");
                while (clean.startsWith("./")) clean = clean.substring(2);

                if (clean.isEmpty() || clean.contains("..")) {
                    skip(gz, size + (512 - (size % 512)) % 512);
                    continue;
                }
                File out = new File(dest, clean);
                String canon = out.getCanonicalPath();
                if (!canon.equals(dest.getPath()) && !canon.startsWith(dest.getPath() + File.separator)) {
                    throw new IOException("Entri arsip tidak aman: " + clean);
                }
                if (type == '5' || clean.endsWith("/")) {
                    if (!out.exists() && !out.mkdirs()) throw new IOException("Gagal membuat folder " + clean);
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Gagal membuat folder " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        copy(gz, fos, size);
                    }
                }
                skip(gz, (512 - (size % 512)) % 512);
            }
        }
    }

    /** Reads a metadata header body (long name / pax records) as UTF-8 text. */
    private static String readText(GZIPInputStream in, long size) throws IOException {
        if (size <= 0) return "";
        if (size > 1048576) throw new IOException("Header arsip terlalu besar");
        byte[] buf = new byte[(int) size];
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new IOException("Arsip terpotong");
            off += n;
        }
        return new String(buf, "UTF-8");
    }

    /** Pulls the path= record out of a pax extended header. */
    private static String paxPath(String meta) {
        String found = null;
        for (String line : meta.split("\n")) {
            int sp = line.indexOf(' ');
            if (sp <= 0) continue;
            String rec = line.substring(sp + 1);
            if (!rec.startsWith("path=")) continue;
            String value = rec.substring(5);
            if (!value.isEmpty()) found = value;
        }
        return found;
    }

    private static int readFully(GZIPInputStream in, byte[] buf, int len) throws IOException {
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) break;
            off += n;
        }
        return off;
    }

    private static void copy(GZIPInputStream in, OutputStream out, long size) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long left = size;
        while (left > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (n < 0) throw new IOException("Arsip terpotong");
            out.write(buf, 0, n);
            left -= n;
        }
    }

    private static void skip(GZIPInputStream in, long size) throws IOException {
        byte[] buf = new byte[8192];
        long left = Math.max(0, size);
        while (left > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (n < 0) return;
            left -= n;
        }
    }

    private static String str(byte[] h, int off, int len) {
        int end = off + len;
        StringBuilder sb = new StringBuilder();
        for (int i = off; i < end && h[i] != 0; i++) sb.append((char) (h[i] & 0xff));
        return sb.toString().trim();
    }

    private static long oct(byte[] h, int off, int len) {
        String s = str(h, off, len);
        if (s.isEmpty()) return 0;
        try {
            return Long.parseLong(s, 8);
        } catch (Exception e) {
            return 0;
        }
    }
}
