package com.pumpkin.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Simple .tar.gz archive writer so backups do not depend on Android's
 * tar binary.
 */
final class TarGz {
    private TarGz() {}

    static void create(File baseDir, String entry, File target) throws IOException {
        File root = new File(baseDir, entry);
        if (!root.exists()) throw new IOException("Folder tidak ditemukan: " + root);
        try (FileOutputStream fos = new FileOutputStream(target);
             GZIPOutputStream gz = new GZIPOutputStream(fos, 64 * 1024)) {
            add(gz, root, entry, true);
            gz.write(new byte[1024]); // akhir arsip: dua blok nol
            gz.finish();
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
}
