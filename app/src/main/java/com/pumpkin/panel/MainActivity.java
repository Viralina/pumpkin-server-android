package com.pumpkin.panel;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String BIN_URL =
            "https://github.com/Pumpkin-MC/Pumpkin/releases/download/0.2.2%2B26.3-26.60/pumpkin-aarch64-android";
    /** Ada hanya di APK "full"; APK polosan tidak punya aset ini. */
    private static final String BUNDLED_BIN = "pumpkin-aarch64-android";
    private static final String BUNDLED_ID = "bundled-bin-id";
    private static final int REQ_STORAGE = 1;
    private static final int REQ_IMPORT = 2;

    private Store st;
    private WebView web;
    private LinearLayout setup;
    private LinearLayout setupBody;
    private TextView setupTitle;
    private TextView setupDesc;
    private TextView setupStatus;
    private TextView setupPath;
    private ProgressBar progress;
    private Button btnDownload;
    private Button btnImport;
    private Button btnInternal;
    private Button btnSwap;
    private Button btnClose;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private int retries;
    private boolean panelStarted;
    private boolean menuOpen;
    private boolean bundledBusy;
    private String lang = "id";

    private static final Map<String, String> EN = new HashMap<>();

    static {
        EN.put("Binary Pumpkin belum terpasang.\nUnduh sekali (120 MB), atau impor dari file yang sudah kamu punya.",
                "Pumpkin binary is not installed.\nDownload it once (120 MB), or import a file you already have.");
        EN.put("Unduh Pumpkin 0.2.2 (120 MB)", "Download Pumpkin 0.2.2 (120 MB)");
        EN.put("Impor dari file", "Import from file");
        EN.put("Impor / tukar binary dari /sdcard", "Import / swap binary from /sdcard");
        EN.put("Tukar dengan cadangan", "Swap with backup");
        EN.put("Tutup menu", "Close menu");
        EN.put("Pakai folder app (jika /sdcard bermasalah)", "Use app folder (if /sdcard fails)");
        EN.put("Menu aplikasi", "App menu");
        EN.put("Kelola binary Pumpkin dan data server. Server tetap berjalan di belakang layar.",
                "Manage the Pumpkin binary and server data. The server keeps running in the background.");
        EN.put("Pilih binary untuk ditukar, atau tutup menu untuk kembali ke panel.",
                "Choose a binary to swap, or close this menu to return to the panel.");
        EN.put("Impor binary Pumpkin untuk mulai.", "Import a Pumpkin binary to start.");
        EN.put("Folder data default: /sdcard/server\nBinary: /data/data/com.pumpkin.panel/files/pumpkin\n\n"
                        + "Menukar binary: impor file baru kapan saja. Binary lama otomatis disimpan sebagai "
                        + "pumpkin.bak, jadi bisa ditukar balik lewat tombol \"Tukar dengan cadangan\".\n\n"
                        + "Menu ini juga terbuka dari panel: tombol menu di kanan atas atau tombol Back Android.\n\n"
                        + "Server berjalan sebagai foreground service. Izinkan aplikasi berjalan di latar belakang "
                        + "dan matikan optimasi baterai agar tidak dimatikan sistem.",
                "Default data folder: /sdcard/server\nBinary: /data/data/com.pumpkin.panel/files/pumpkin\n\n"
                        + "Swapping binaries: import a new file at any time. The old binary is saved as "
                        + "pumpkin.bak, so you can swap back with the \"Swap with backup\" button.\n\n"
                        + "This menu also opens from the panel: the menu button at the top right or the Android Back button.\n\n"
                        + "The server runs as a foreground service. Allow background activity and disable battery "
                        + "optimization so the system does not stop it.");
        EN.put("Menunggu izin penyimpanan...", "Waiting for storage permission...");
        EN.put("Izin penyimpanan ditolak. Tanpa izin ini /sdcard/server tidak bisa dipakai.",
                "Storage permission denied. Without it /sdcard/server cannot be used.");
        EN.put("Izin diperlukan", "Permission required");
        EN.put("Panel butuh akses penyimpanan untuk folder server di /sdcard.",
                "The panel needs storage access for the server folder on /sdcard.");
        EN.put("Buka pengaturan", "Open settings");
        EN.put("Pakai folder app", "Use app folder");
        EN.put("Binary terpasang tidak bisa dipakai.\nImpor binary Android lewat menu di bawah.",
                "The installed binary cannot be used.\nImport an Android binary using the menu below.");
        EN.put("Siap mengunduh.", "Ready to download.");
        EN.put("Binary belum ada.", "Binary not installed.");
        EN.put("Tidak bisa memberi izin eksekusi ke binary. Impor ulang.",
                "Could not make the binary executable. Import it again.");
        EN.put("Menyiapkan unduhan...", "Preparing download...");
        EN.put("Binary Pumpkin ikut dibundel di APK ini.\nDisiapkan sekali saat pertama dibuka.",
                "The Pumpkin binary is bundled in this APK.\nIt is prepared once on first launch.");
        EN.put("Menyiapkan binary bawaan...", "Preparing built-in binary...");
        EN.put("Binary bawaan siap.", "Built-in binary ready.");
        EN.put("Gagal menyiapkan binary bawaan. Impor manual lewat menu di bawah.",
                "Failed to prepare the built-in binary. Import it manually using the menu below.");
        EN.put("Binary siap.", "Binary ready.");
        EN.put("Menyalin file...", "Copying file...");
        EN.put("Menukar binary...", "Swapping binary...");
        EN.put("Binary ditukar. Hentikan lalu mulai server supaya versi baru dipakai.",
                "Binary swapped. Stop and start the server so the new version is used.");
        EN.put("Binary ditukar", "Binary swapped");
        EN.put("Belum ada cadangan. Impor binary baru dulu.", "No backup yet. Import a new binary first.");
        EN.put("Tidak ada file manager", "No file manager found");
        EN.put("Tunggu proses binary selesai", "Wait for the binary process to finish");
        EN.put("Folder app dipakai", "App folder in use");
        EN.put("Mencari binary di /sdcard...", "Searching for binaries in /sdcard...");
        EN.put("Tidak ada file binary Pumpkin di /sdcard.", "No Pumpkin binary found in /sdcard.");
        EN.put("File tidak ditemukan", "File not found");
        EN.put("Pilih file lain", "Choose another file");
        EN.put("Pilih binary Pumpkin", "Select Pumpkin binary");
        EN.put("Batal", "Cancel");
        EN.put("Binary tidak bisa dipakai", "Binary cannot be used");
        EN.put("Menyalin file Pumpkin...", "Copying Pumpkin file...");
        EN.put("Menyimpan world...", "Saving world...");
        EN.put("Unduh", "Download");
        EN.put("Impor", "Import");
        EN.put("Simpan", "Save");
        EN.put("Bahasa / Language", "Language");
        EN.put("Binary", "Binary");
        EN.put("Ukuran", "Size");
        EN.put("Status", "Status");
        EN.put("Cadangan", "Backup");
        EN.put("Data", "Data");
        EN.put("siap", "ready");
        EN.put("belum ada", "not installed");
        EN.put("siap ditukar", "ready to swap");
        EN.put("TIDAK BISA DIPAKAI", "NOT USABLE");
        EN.put("Pumpkin terpasang - restart server untuk memakai binary baru",
                "Pumpkin installed - restart the server to use the new binary");
    }

    /** Terjemahan teks native (ID -> EN). */
    private String tr(String s) {
        if (!"en".equals(lang)) return s;
        String v = EN.get(s);
        return v == null ? s : v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        st = new Store(this);
        lang = st.lang();
        getWindow().setStatusBarColor(0xff07090d);
        getWindow().setNavigationBarColor(0xff07090d);
        buildUi();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
            setStatus(tr("Menunggu izin penyimpanan..."));
        } else {
            afterPermission();
        }
    }

    /* ------------------------------------------------------------------- UI */

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff07090d);

        web = new WebView(this);
        web.setBackgroundColor(0xff07090d);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) ws.setSafeBrowsingEnabled(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost() == null ? "" : uri.getHost();
                if ("127.0.0.1".equals(host) || "localhost".equals(host)) return false;
                if (host.equals(panelHost())) return false;
                // Sepenuhnya offline: jangan buka tautan keluar dari panel.
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if (!scheme.startsWith("http")) return null;
                String host = uri.getHost() == null ? "" : uri.getHost();
                if ("127.0.0.1".equals(host) || "localhost".equals(host) || host.equals(panelHost())) return null;
                // Hanya panel lokal yang boleh dimuat; aset eksternal diblokir.
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (panelStarted && !menuOpen) setup.setVisibility(View.GONE);
                if (panelStarted && isPanelUrl(url)) {
                    String tok = st.token();
                    if (tok != null && !tok.isEmpty()) {
                        view.evaluateJavascript(
                                "window.ppSetToken && window.ppSetToken(" + org.json.JSONObject.quote(tok) + ")", null);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                if (request == null || !request.isForMainFrame() || !panelStarted) return;
                if (retries++ < 5) ui.postDelayed(() -> probe(0), 800);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new NativeBridge(), "PanelNative");

        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setup = buildSetup();
        root.addView(setup, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private LinearLayout buildSetup() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xff07090d);
        scroll.setFillViewport(true);

        setupBody = new LinearLayout(this);
        setupBody.setOrientation(LinearLayout.VERTICAL);
        setupBody.setPadding(dp(24), dp(40), dp(24), dp(32));
        scroll.addView(setupBody, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Pumpkin Panel");
        title.setTextColor(0xffeef2f8);
        title.setTextSize(26);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        setupBody.addView(title);
        setupTitle = title;

        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lrlp.topMargin = dp(10);
        setupBody.addView(langRow, lrlp);

        TextView langLbl = new TextView(this);
        langLbl.setText(tr("Bahasa / Language"));
        langLbl.setTextColor(0xff66748a);
        langLbl.setTextSize(12);
        langRow.addView(langLbl);

        View spacer = new View(this);
        langRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        langRow.addView(flagButton(R.drawable.flag_id, "id", "Bahasa Indonesia"),
                new LinearLayout.LayoutParams(dp(46), dp(32)));
        LinearLayout.LayoutParams flagLp = new LinearLayout.LayoutParams(dp(46), dp(32));
        flagLp.leftMargin = dp(8);
        langRow.addView(flagButton(R.drawable.flag_en, "en", "English"), flagLp);

        TextView desc = new TextView(this);
        desc.setText(tr("Binary Pumpkin belum terpasang.\nUnduh sekali (120 MB), atau impor dari file yang sudah kamu punya."));
        desc.setTextColor(0xff98a6b8);
        desc.setTextSize(14);
        desc.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(10);
        setupBody.addView(desc, dlp);
        setupDesc = desc;

        setupPath = new TextView(this);
        setupPath.setTextColor(0xff66748a);
        setupPath.setTextSize(11);
        setupPath.setTypeface(android.graphics.Typeface.MONOSPACE);
        setupPath.setPadding(0, dp(12), 0, dp(12));
        setupBody.addView(setupPath);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        plp.topMargin = dp(4);
        plp.bottomMargin = dp(10);
        setupBody.addView(progress, plp);

        setupStatus = new TextView(this);
        setupStatus.setTextColor(0xffff8a3d);
        setupStatus.setTextSize(13);
        setupStatus.setMinHeight(dp(40));
        setupBody.addView(setupStatus);

        btnDownload = new Button(this);
        btnDownload.setText(tr("Unduh Pumpkin 0.2.2 (120 MB)"));
        btnDownload.setAllCaps(false);
        btnDownload.setOnClickListener(v -> startDownload());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(14);
        setupBody.addView(btnDownload, blp);

        btnImport = new Button(this);
        btnImport.setText(tr("Impor dari file"));
        btnImport.setAllCaps(false);
        btnImport.setOnClickListener(v -> pickFile());
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp2.topMargin = dp(8);
        setupBody.addView(btnImport, blp2);

        Button btnSd = new Button(this);
        btnSd.setText(tr("Impor / tukar binary dari /sdcard"));
        btnSd.setAllCaps(false);
        btnSd.setOnClickListener(v -> chooseSdcardBinary());
        LinearLayout.LayoutParams blp2b = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp2b.topMargin = dp(8);
        setupBody.addView(btnSd, blp2b);

        btnSwap = new Button(this);
        btnSwap.setText(tr("Tukar dengan cadangan"));
        btnSwap.setAllCaps(false);
        btnSwap.setVisibility(View.GONE);
        btnSwap.setOnClickListener(v -> swapBinary());
        LinearLayout.LayoutParams blpSwap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blpSwap.topMargin = dp(8);
        setupBody.addView(btnSwap, blpSwap);

        btnClose = new Button(this);
        btnClose.setText(tr("Tutup menu"));
        btnClose.setAllCaps(false);
        btnClose.setVisibility(View.GONE);
        btnClose.setOnClickListener(v -> hideMenu());
        LinearLayout.LayoutParams blpClose = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blpClose.topMargin = dp(18);
        setupBody.addView(btnClose, blpClose);

        btnInternal = new Button(this);
        btnInternal.setText(tr("Pakai folder app (jika /sdcard bermasalah)"));
        btnInternal.setAllCaps(false);
        btnInternal.setOnClickListener(v -> useInternalFolder());
        LinearLayout.LayoutParams blp3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp3.topMargin = dp(8);
        setupBody.addView(btnInternal, blp3);

        TextView note = new TextView(this);
        note.setText(tr("Folder data default: /sdcard/server\nBinary: /data/data/com.pumpkin.panel/files/pumpkin\n\n"
                + "Menukar binary: impor file baru kapan saja. Binary lama otomatis disimpan sebagai "
                + "pumpkin.bak, jadi bisa ditukar balik lewat tombol \"Tukar dengan cadangan\".\n\n"
                + "Menu ini juga terbuka dari panel: tombol menu di kanan atas atau tombol Back Android.\n\n"
                + "Server berjalan sebagai foreground service. Izinkan aplikasi berjalan di latar belakang "
                + "dan matikan optimasi baterai agar tidak dimatikan sistem."));
        note.setTextColor(0xff66748a);
        note.setTextSize(11);
        note.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(22);
        setupBody.addView(note, nlp);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return wrap;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private ImageButton flagButton(int res, final String code, String label) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(res);
        b.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        b.setPadding(dp(3), dp(3), dp(3), dp(3));
        b.setContentDescription(label);
        b.setBackground(flagBg(code.equals(lang)));
        b.setOnClickListener(v -> setLanguage(code));
        return b;
    }

    private GradientDrawable flagBg(boolean active) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(7));
        g.setColor(active ? 0x33ffffff : 0x0dffffff);
        g.setStroke(dp(1), active ? 0x99ffaa00 : 0x22ffffff);
        return g;
    }

    /** Simpan bahasa, bangun ulang layar native, dan samakan bahasa panel web. */
    private void setLanguage(String code) {
        st.setLang(code);
        lang = st.lang();
        rebuildSetup();
        if (menuOpen) showMenu();
        else if (panelStarted) setup.setVisibility(View.GONE);
        else updateBinaryInfo();
        if (web != null) {
            try {
                web.evaluateJavascript("window.ppApplyLang && window.ppApplyLang('" + lang + "')", null);
            } catch (Exception ignored) {
            }
        }
    }

    private void rebuildSetup() {
        if (setup == null || !(setup.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) setup.getParent();
        int idx = parent.indexOfChild(setup);
        ViewGroup.LayoutParams lp = setup.getLayoutParams();
        boolean visible = setup.getVisibility() == View.VISIBLE;
        parent.removeView(setup);
        setup = buildSetup();
        parent.addView(setup, idx, lp);
        setup.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /* --------------------------------------------------------------- alur */

    @Override
    protected void onResume() {
        super.onResume();
        if (!panelStarted
                && Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && st.binFile().isFile()) {
            afterPermission();
        }
    }

    /** ID binary bawaan (md5), atau kosong untuk APK polosan. */
    private String bundledId() {
        try (InputStream in = getAssets().open(BUNDLED_ID)) {
            byte[] b = new byte[128];
            int n = in.read(b);
            return n <= 0 ? "" : new String(b, 0, n,
                    java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * APK "full" membawa binary Pumpkin di assets. Binary diekstrak sekali per
     * ID binary (md5 yang ditulis saat build), jadi update full bisa mengganti
     * binary lama, tetapi binary yang diimpor pengguna setelahnya tidak
     * tertimpa. APK "polosan" tidak punya aset ini dan langsung dilewati.
     */
    private boolean ensureBundledBinary() {
        if (bundledBusy) return true;
        final String id = bundledId();
        if (id.isEmpty()) return false;                 // APK polosan
        if (id.equals(st.bundledBinId())) return false; // sudah pernah diekstrak
        final File dst = st.binFile();

        bundledBusy = true;
        File parent = dst.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        setupDesc.setText(tr("Binary Pumpkin ikut dibundel di APK ini.\nDisiapkan sekali saat pertama dibuka."));
        setStatus(tr("Menyiapkan binary bawaan..."));
        progress.setIndeterminate(true);
        progress.setVisibility(View.VISIBLE);
        btnDownload.setEnabled(false);
        btnImport.setEnabled(false);
        btnInternal.setEnabled(false);

        io.execute(() -> {
            final File tmp = new File(dst.getAbsolutePath() + ".tmp");
            boolean ok = false;
            try (InputStream in = getAssets().open(BUNDLED_BIN);
                 OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[128 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.flush();
                ok = tmp.isFile() && tmp.length() > 0;
            } catch (IOException ignored) {
            }
            if (ok) {
                //noinspection ResultOfMethodCallIgnored
                dst.delete();
                ok = tmp.renameTo(dst);
            }
            if (ok) {
                //noinspection ResultOfMethodCallIgnored
                dst.setExecutable(true, false);
                ok = dst.canExecute();
            }
            if (!ok) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
            final boolean done = ok;
            ui.post(() -> {
                bundledBusy = false;
                progress.setIndeterminate(false);
                progress.setVisibility(View.GONE);
                btnDownload.setEnabled(true);
                btnImport.setEnabled(true);
                btnInternal.setEnabled(true);
                if (done) {
                    st.setBundledBinId(id);
                    setStatus(tr("Binary bawaan siap."));
                    afterPermission();
                } else {
                    setStatus(tr("Gagal menyiapkan binary bawaan. Impor manual lewat menu di bawah."));
                    showSetup();
                }
            });
        });
        return true;
    }

    private void afterPermission() {
        if (ensureBundledBinary()) return;
        setupPath.setText("Binary : " + st.binFile().getAbsolutePath()
                + "\nData   : " + st.serverDir());
        File dir = st.serverDirFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            setStatus(( "en".equals(lang) ? "Folder " : "Folder " ) + dir.getAbsolutePath() + ("en".equals(lang) ? " cannot be created. Use the app folder below." : " tidak bisa dibuat. Pakai folder app di bawah."));
        }
        if (st.binFile().isFile()) {
            String problem = Fs.binaryProblem(st.binFile());
            if (!problem.isEmpty()) {
                setupDesc.setText("Binary terpasang tidak bisa dipakai.\nImpor binary Android lewat menu di bawah.");
                setStatus("Binary terpasang tidak bisa dipakai: " + problem
                        + ".\nImpor ulang file pumpkin-aarch64-android (bukan build Linux).");
            }
            updateBinaryInfo();
            onBinaryReady();
        } else {
            updateBinaryInfo();
            setStatus(tr("Siap mengunduh."));
        }
    }

    private void startPanel() {
        if (panelStarted) return;
        panelStarted = true;
        Intent svc = new Intent(this, PanelService.class).setAction(PanelService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
        else startService(svc);
        retries = 0;
        probe(0);
    }

    /** Host yang benar-benar dipakai panel (127.0.0.1 bila bind ke semua antarmuka). */
    private String panelHost() {
        if (!st.panelOnline()) return "127.0.0.1";
        String h = st.host();
        if (h == null || h.isEmpty() || "0.0.0.0".equals(h) || "::".equals(h)) return "127.0.0.1";
        return h;
    }

    private String panelBase() {
        String h = panelHost();
        if (h.contains(":")) h = "[" + h + "]";
        return "http://" + h + ":" + st.port();
    }

    /** URL panel; token disertakan agar akses lewat IP LAN tetap lolos halaman kunci. */
    private String panelHomeUrl() {
        String tok = st.token();
        String base = panelBase() + "/";
        if (tok != null && !tok.isEmpty()) base += "?token=" + Uri.encode(tok);
        return base;
    }

    /** Header token untuk request halaman utama (cadangan bila query hilang). */
    private Map<String, String> panelHeaders() {
        Map<String, String> h = new HashMap<>();
        String tok = st.token();
        if (tok != null && !tok.isEmpty()) h.put("x-token", tok);
        return h;
    }

    /** Tunggu API lokal siap sebelum menampilkan WebView. */
    private void probe(int attempt) {
        if (!panelStarted) return;
        net.execute(() -> {
            boolean up = false;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(panelBase() + "/api/ping").openConnection();
                conn.setConnectTimeout(1200);
                conn.setReadTimeout(1500);
                conn.setUseCaches(false);
                String tok = st.token();
                if (tok != null && !tok.isEmpty()) conn.setRequestProperty("x-token", tok);
                // Hanya terima panel milik proses ini; jangan tertipu panel lain di port yang sama.
                if (conn.getResponseCode() == 200) {
                    java.io.InputStream in = conn.getInputStream();
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0 && bo.size() < 16384) bo.write(buf, 0, n);
                    in.close();
                    org.json.JSONObject o = new org.json.JSONObject(bo.toString("UTF-8"));
                    up = o.optInt("panel", 0) == 1
                            && o.optInt("pid", -1) == android.os.Process.myPid();
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
            final boolean ready = up;
            ui.post(() -> {
                if (!panelStarted) return;
                if (ready) {
                    retries = 0;
                    if (!menuOpen) setup.setVisibility(View.GONE);
                    web.loadUrl(panelHomeUrl(), panelHeaders());
                    return;
                }
                if (attempt < 15) {
                    ui.postDelayed(() -> probe(attempt + 1), 700);
                } else {
                    panelDown();
                }
            });
        });
    }

    private void panelDown() {
        panelStarted = false;
        showSetup();
        setStatus(("en".equals(lang) ? "Panel API is not responding at " : "Panel API tidak merespons di ") + panelHost() + ":" + st.port() + ("en".equals(lang) ? ". The port may be used by another app, or the service failed to start. Close and reopen this app." : ". Port mungkin dipakai aplikasi lain (mis. panel Termux), atau service gagal start. Tutup lalu buka lagi aplikasi ini."));
    }

    private void onBinaryReady() {
        File bin = st.binFile();
        if (!bin.isFile()) {
            setStatus(tr("Binary belum ada."));
            return;
        }
        if (!Fs.isAndroidArm64Elf(bin)) {
            // Binary lama/rusak tidak bisa dipakai; tampilkan lagi layar pemasangan.
            panelStarted = false;
            showSetup();
            btnDownload.setEnabled(true);
            btnImport.setEnabled(true);
            setStatus(("en".equals(lang) ? "Binary at " : "Binary di ") + bin.getAbsolutePath() + ("en".equals(lang) ? " cannot be used: " : " tidak bisa dipakai: ") + Fs.binaryProblem(bin) + ("en".equals(lang) ? ".\nImport pumpkin-aarch64-android again." : ".\nImpor ulang file pumpkin-aarch64-android."));
            return;
        }
        if (!bin.canExecute()) //noinspection ResultOfMethodCallIgnored
            bin.setExecutable(true, true);
        if (bin.canExecute()) {
            startPanel();
        } else {
            setup.setVisibility(View.VISIBLE);
            panelStarted = false;
            setStatus(tr("Tidak bisa memberi izin eksekusi ke binary. Impor ulang."));
        }
    }

    private void setStatus(String text) {
        if (setupStatus != null) setupStatus.setText(text);
    }

    /* ------------------------------------------------------------ menu app */

    /** Jembatan untuk tombol "Menu aplikasi" di HTML panel. */
    public class NativeBridge {
        @JavascriptInterface
        public void openMenu() {
            ui.post(() -> {
                if (web == null) return;
                String url = web.getUrl();
                if (!isPanelUrl(url)) return;
                showMenu();
            });
        }

        @JavascriptInterface
        public boolean hasMenu() {
            return true;
        }

        @JavascriptInterface
        public String getLang() {
            return lang;
        }

        @JavascriptInterface
        public void setLang(String code) {
            ui.post(() -> setLanguage(code));
        }
    }

    private boolean isPanelUrl(String url) {
        if (url == null) return false;
        try {
            String host = Uri.parse(url).getHost();
            if (host == null) return false;
            return "127.0.0.1".equals(host) || "localhost".equals(host) || host.equals(panelHost());
        } catch (Exception e) {
            return false;
        }
    }

    private File backupFile() {
        return new File(st.binFile().getAbsolutePath() + ".bak");
    }

    private void showMenu() {
        menuOpen = true;
        setupTitle.setText(tr("Menu aplikasi"));
        setupDesc.setText(tr("Kelola binary Pumpkin dan data server. Server tetap berjalan di belakang layar."));
        btnClose.setVisibility(panelStarted ? View.VISIBLE : View.GONE);
        progress.setVisibility(View.GONE);
        btnDownload.setEnabled(true);
        btnImport.setEnabled(true);
        updateBinaryInfo();
        setStatus(tr(panelStarted
                ? "Pilih binary untuk ditukar, atau tutup menu untuk kembali ke panel."
                : "Impor binary Pumpkin untuk mulai."));
        setup.setVisibility(View.VISIBLE);
    }

    private void hideMenu() {
        menuOpen = false;
        if (panelStarted) setup.setVisibility(View.GONE);
    }

    private void showSetup() {
        menuOpen = false;
        setupTitle.setText("Pumpkin Panel");
        setupDesc.setText(tr("Binary Pumpkin belum terpasang.\nUnduh sekali (120 MB), atau impor dari file yang sudah kamu punya."));
        btnClose.setVisibility(View.GONE);
        updateBinaryInfo();
        setup.setVisibility(View.VISIBLE);
    }

    private static String sizeMb(File f) {
        return String.format(Locale.US, "%.1f MB", f.length() / 1048576.0);
    }

    private void updateBinaryInfo() {
        File bin = st.binFile();
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Binary")).append(" : ").append(bin.getAbsolutePath()).append('\n');
        if (bin.isFile()) {
            sb.append(tr("Ukuran")).append(" : ").append(sizeMb(bin)).append('\n');
            String problem = Fs.binaryProblem(bin);
            if (problem.isEmpty()) {
                String interp = Fs.elfInterpreter(bin);
                sb.append(tr("Status")).append(" : ").append(tr("siap"));
                if (!interp.isEmpty()) sb.append(" (").append(interp).append(')');
            } else {
                sb.append(tr("Status")).append(" : ").append(tr("TIDAK BISA DIPAKAI")).append(" - ").append(problem);
            }
        } else {
            sb.append(tr("Status")).append(" : ").append(tr("belum ada"));
        }
        sb.append("\n").append(tr("Data")).append("   : ").append(st.serverDir());
        File bak = backupFile();
        if (bak.isFile()) {
            sb.append("\n").append(tr("Cadangan")).append(": ").append(sizeMb(bak));
            String p = Fs.binaryProblem(bak);
            sb.append(" (").append(p.isEmpty() ? tr("siap ditukar") : p).append(")");
        }
        setupPath.setText(sb.toString());
        updateSwapButton();
    }

    private void updateSwapButton() {
        File bak = backupFile();
        boolean valid = bak.isFile() && Fs.binaryProblem(bak).isEmpty();
        btnSwap.setVisibility(bak.isFile() ? View.VISIBLE : View.GONE);
        btnSwap.setEnabled(valid);
        if (bak.isFile()) btnSwap.setText(tr("Tukar dengan cadangan") + " (" + sizeMb(bak) + ")");
    }

    /** Tukar binary aktif dengan cadangan (pumpkin.bak), dua arah. */
    private void swapBinary() {
        final File bin = st.binFile();
        final File bak = backupFile();
        if (!bak.isFile()) {
            setStatus(tr("Belum ada cadangan. Impor binary baru dulu."));
            return;
        }
        String problem = Fs.binaryProblem(bak);
        if (!problem.isEmpty()) {
            setStatus("Cadangan tidak bisa dipakai: " + problem);
            return;
        }
        btnSwap.setEnabled(false);
        setStatus(tr("Menukar binary..."));
        io.execute(() -> {
            final File tmp = new File(bin.getAbsolutePath() + ".swap");
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            boolean ok = !bin.exists() || bin.renameTo(tmp);
            if (ok && !bak.renameTo(bin)) {
                if (tmp.exists()) //noinspection ResultOfMethodCallIgnored
                    tmp.renameTo(bin);
                ok = false;
            }
            if (ok) //noinspection ResultOfMethodCallIgnored
                tmp.renameTo(bak);
            boolean exec = ok && bin.setExecutable(true, true);
            final boolean okFinal = ok;
            final boolean execFinal = exec;
            final String after = ok ? Fs.binaryProblem(bin) : "rename gagal";
            ui.post(() -> {
                btnSwap.setEnabled(true);
                updateBinaryInfo();
                if (!okFinal || !execFinal || !after.isEmpty()) {
                    setStatus("Gagal menukar binary: "
                            + (!okFinal ? "rename gagal" : (!execFinal ? "izin eksekusi" : after)));
                    return;
                }
                setStatus("Binary ditukar. Hentikan lalu mulai server supaya versi baru dipakai.");
                Toast.makeText(this, tr("Binary ditukar"), Toast.LENGTH_SHORT).show();
            });
        });
    }

    /* ------------------------------------------------------------- unduhan */

    private void startDownload() {
        btnDownload.setEnabled(false);
        btnImport.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        setStatus(tr("Menyiapkan unduhan..."));
        final File part = new File(st.binFile().getAbsolutePath() + ".part");

        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BIN_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "PumpkinPanel/1.0");

                long have = part.isFile() ? part.length() : 0;
                if (have > 0) conn.setRequestProperty("Range", "bytes=" + have + "-");
                int code = conn.getResponseCode();
                if (code != 200 && code != 206) throw new IOException("HTTP " + code);

                long total = conn.getContentLengthLong();
                boolean resume = code == 206 && have > 0;
                if (resume && total > 0) total += have;
                if (!resume) {
                    //noinspection ResultOfMethodCallIgnored
                    part.delete();
                }
                final long totalFinal = total;
                ui.post(() -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(0);
                });

                long done = resume ? have : 0;
                int lastPct = -1;
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(part, resume)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (totalFinal > 0) {
                            int pct = (int) (done * 1000 / totalFinal);
                            if (pct != lastPct) {
                                lastPct = pct;
                                final long d = done;
                                ui.post(() -> {
                                    progress.setProgress(pct);
                                    setStatus(String.format(Locale.US, "Mengunduh %d / %d MB",
                                            d / 1048576, totalFinal / 1048576));
                                });
                            }
                        }
                    }
                }

                installPart(part);
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus(tr("Binary siap."));
                    Toast.makeText(this, tr("Pumpkin terpasang - restart server untuk memakai binary baru"), Toast.LENGTH_LONG).show();
                    hideMenu();
                    updateBinaryInfo();
                    onBinaryReady();
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus("Gagal mengunduh: " + msg);
                    btnDownload.setEnabled(true);
                    btnImport.setEnabled(true);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void installPart(File part) throws IOException {
        try {
            verifyAndInstall(part);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            part.delete(); // jangan biarkan unduhan rusak dipakai untuk resume
            throw e;
        }
    }

    private void verifyAndInstall(File part) throws IOException {
        String problem = Fs.binaryProblem(part);
        if (!problem.isEmpty()) throw new IOException(problem);
        File bin = st.binFile();
        File parent = bin.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        File bak = backupFile();
        if (bin.exists()) {
            long free = parent == null ? 0 : parent.getUsableSpace();
            boolean keep = Fs.binaryProblem(bin).isEmpty();
            bak.delete();
            if (keep && free > bin.length() + 8L * 1024 * 1024) {
                // Simpan versi lama supaya bisa ditukar balik dari menu.
                if (!bin.renameTo(bak)) {
                    try {
                        copyFile(bin, bak);
                    } catch (IOException ignored) {
                        //noinspection ResultOfMethodCallIgnored
                        bak.delete();
                    }
                    //noinspection ResultOfMethodCallIgnored
                    bin.delete();
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                bin.delete();
            }
        }
        if (!part.renameTo(bin)) {
            try {
                copyFile(part, bin);
                //noinspection ResultOfMethodCallIgnored
                part.delete();
            } catch (IOException e) {
                if (bak.isFile() && !bin.isFile()) //noinspection ResultOfMethodCallIgnored
                    bak.renameTo(bin);
                throw e;
            }
        }
        if (!bin.setExecutable(true, true)) {
            throw new IOException("Gagal memberi izin eksekusi");
        }
    }

    private void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    /** Cari semua file binary Pumpkin di /sdcard lalu biarkan pengguna memilih. */
    private void chooseSdcardBinary() {
        setStatus(tr("Mencari binary di /sdcard..."));
        io.execute(() -> {
            final List<File> found = findSdcardBinaries();
            ui.post(() -> {
                updateBinaryInfo();
                if (found.isEmpty()) {
                    setStatus(tr("Tidak ada file binary Pumpkin di /sdcard."));
                    new AlertDialog.Builder(this)
                            .setTitle(tr("File tidak ditemukan"))
                            .setMessage("Taruh file pumpkin-aarch64-android di /sdcard atau /sdcard/Download,"
                                    + " lalu coba lagi.\n\nBuild Linux/glibc tidak bisa dipakai di Android.")
                            .setPositiveButton("OK", null)
                            .setNegativeButton(tr("Pilih file lain"), (d, w) -> pickFile())
                            .show();
                    return;
                }
                String[] labels = new String[found.size()];
                for (int i = 0; i < found.size(); i++) {
                    File f = found.get(i);
                    String p = Fs.binaryProblem(f);
                    labels[i] = f.getName() + "  (" + sizeMb(f) + ")"
                            + (p.isEmpty() ? "  siap" : "  - " + p);
                }
                new AlertDialog.Builder(this)
                        .setTitle(tr("Pilih binary Pumpkin"))
                        .setItems(labels, (d, which) -> importBinary(found.get(which)))
                        .setNeutralButton(tr("Pilih file lain"), (d, w) -> pickFile())
                        .setNegativeButton(tr("Batal"), null)
                        .show();
            });
        });
    }

    private List<File> findSdcardBinaries() {
        Map<String, File> map = new LinkedHashMap<>();
        String[] roots = {
                "/sdcard/pumpkin",
                "/sdcard",
                "/sdcard/Download",
                "/sdcard/Download/Pumpkin",
                "/sdcard/server"
        };
        for (String root : roots) {
            File dir = new File(root);
            if (dir.isFile()) {
                addCandidate(map, dir);
                continue;
            }
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) addCandidate(map, f);
        }
        List<File> list = new ArrayList<>(map.values());
        list.sort((a, b) -> {
            boolean va = Fs.binaryProblem(a).isEmpty();
            boolean vb = Fs.binaryProblem(b).isEmpty();
            if (va != vb) return va ? -1 : 1;
            return Long.compare(b.lastModified(), a.lastModified());
        });
        return list;
    }

    private void addCandidate(Map<String, File> map, File f) {
        if (!f.isFile() || f.length() < 1024 * 1024) return;
        String n = f.getName().toLowerCase(Locale.US);
        if (!n.contains("pumpkin")) return;
        if (n.endsWith(".apk") || n.endsWith(".zip") || n.endsWith(".log")
                || n.endsWith(".toml") || n.endsWith(".txt") || n.endsWith(".part")) return;
        map.put(f.getAbsolutePath(), f);
    }

    private void importBinary(final File from) {
        String problem = Fs.binaryProblem(from);
        if (!problem.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(tr("Binary tidak bisa dipakai"))
                    .setMessage(from.getAbsolutePath() + "\n\n" + problem
                            + ".\n\nUnduh pumpkin-aarch64-android lewat tombol Unduh.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        btnDownload.setEnabled(false);
        btnImport.setEnabled(false);
        btnSwap.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        setStatus("Menyalin " + from.getAbsolutePath() + " ...");

        final File part = new File(st.binFile().getAbsolutePath() + ".part");
        io.execute(() -> {
            try {
                //noinspection ResultOfMethodCallIgnored
                part.delete();
                copyFile(from, part);
                installPart(part);
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus(tr("Binary siap."));
                    Toast.makeText(this, tr("Pumpkin terpasang - restart server untuk memakai binary baru"), Toast.LENGTH_LONG).show();
                    hideMenu();
                    updateBinaryInfo();
                    onBinaryReady();
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus("Gagal menyalin: " + msg);
                    btnDownload.setEnabled(true);
                    btnImport.setEnabled(true);
                    updateBinaryInfo();
                });
            }
        });
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            Toast.makeText(this, tr("Tidak ada file manager"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null) return;
        final Uri uri = data.getData();
        if (uri == null) return;

        btnDownload.setEnabled(false);
        btnImport.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        setStatus(tr("Menyalin file..."));

        final File part = new File(st.binFile().getAbsolutePath() + ".part");
        io.execute(() -> {
            try {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(part)) {
                    if (in == null) throw new IOException("File tidak bisa dibaca");
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    long done = 0;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        final long d = done;
                        ui.post(() -> setStatus("Menyalin " + (d / 1048576) + " MB..."));
                    }
                }
                installPart(part);
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus(tr("Binary siap."));
                    hideMenu();
                    updateBinaryInfo();
                    onBinaryReady();
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                ui.post(() -> {
                    progress.setVisibility(View.GONE);
                    setStatus("Gagal menyalin: " + msg);
                    btnDownload.setEnabled(true);
                    btnImport.setEnabled(true);
                    updateBinaryInfo();
                });
            }
        });
    }

    /* ------------------------------------------------------- folder internal */

    private void useInternalFolder() {
        File dir = new File(getFilesDir(), "server");
        st.put("serverDir", dir.getAbsolutePath());
        st.put("backupDir", new File(getFilesDir(), "backups").getAbsolutePath());
        try {
            if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            Toast.makeText(this, tr("Folder app dipakai"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        btnDownload.setEnabled(true);
        btnImport.setEnabled(true);
        afterPermission();
    }

    /* ---------------------------------------------------------- permissions */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_STORAGE) return;
        boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (ok) {
            afterPermission();
        } else {
            setStatus(tr("Izin penyimpanan ditolak. Tanpa izin ini /sdcard/server tidak bisa dipakai."));
            btnDownload.setEnabled(false);
            btnImport.setEnabled(false);
            new AlertDialog.Builder(this)
                    .setTitle(tr("Izin diperlukan"))
                    .setMessage(tr("Panel butuh akses penyimpanan untuk folder server di /sdcard."))
                    .setPositiveButton(tr("Buka pengaturan"), (d, w) -> {
                        try {
                            startActivity(new Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton(tr("Pakai folder app"), (d, w) -> useInternalFolder())
                    .show();
        }
    }

    /* ----------------------------------------------------------------- back */

    @Override
    public void onBackPressed() {
        if (progress != null && progress.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, tr("Tunggu proses binary selesai"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (menuOpen) {
            if (panelStarted) {
                hideMenu();
                return;
            }
            super.onBackPressed();
            return;
        }
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        if (panelStarted) {
            // Back dari panel kembali ke menu native, bukan keluar aplikasi.
            showMenu();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        net.shutdownNow();
        super.onDestroy();
    }
}
