# Panel web — struktur modular

Panel adalah HTML + CSS + JavaScript vanilla, tanpa framework, tanpa CDN, dan tanpa
proses build Node. Saat APK dibangun, semua berkas digabung menjadi **satu** `index.html`
supaya APK tetap self-contained dan runtime tidak pernah membaca `/sdcard`.

```
panel/
├── index.html          template (memakai <link> dan <script src>)
├── css/
│   ├── base.css        variabel tema, reset, shell, sidebar, app bar, tabbar
│   ├── components.css  kartu, tombol, chip, form, console, toast, modal
│   ├── responsive.css  penyesuaian layar kecil / landscape
│   ├── minecraft.css   permukaan, bayangan, dan tekstur gaya Minecraft
│   └── minecraft-extra.css  aksen, animasi, dan detail tema
├── js/
│   ├── core.js         helper `$`, `esc`, localStorage, state awal
│   ├── api.js          fetch API, token, SSE, deteksi koneksi
│   ├── ui.js           toast, modal, helper tampilan
│   ├── status.js       status server, IP, metrik
│   ├── network.js      panel online + bind server Pumpkin
│   ├── home.js         kartu beranda dan ringkasan
│   ├── commands.js     console, perintah, start/stop/restart
│   ├── toml.js         parser TOML sisi klien
│   ├── config.js       editor pumpkin.toml
│   ├── players.js      daftar pemain, OP, whitelist
│   ├── worlds.js       world dan backup
│   ├── settings.js     pengaturan panel dan path
│   ├── events.js       semua handler klik/input
│   ├── init.js         boot, polling, menu native APK
│   ├── interactions.js suara klik, getar, animasi tekan
│   └── i18n.js         kamus ID/EN dan dropdown bahasa
└── ../tools/bundle-panel.sh
```

## Bundling

```bash
bash tools/bundle-panel.sh                        # -> app/src/main/assets/index.html
bash tools/bundle-panel.sh /tmp/preview.html      # -> file lain, untuk diperiksa
```

Skrip hanya memakai `bash` + `cat`: setiap baris `<link rel="stylesheet" href="css/...">`
diganti isi berkas di dalam `<style>`, dan setiap `<script src="js/..."></script>` diganti
isi berkas di dalam `<script>`. Baris lain disalin apa adanya.

`build.sh` otomatis menjalankan bundler sebelum `aapt2`. Aset hasil bundling
(`app/src/main/assets/index.html`) **tidak di-commit** — selalu dibuat ulang supaya tidak ada
dua sumber kebenaran.

## Menambah atau mengubah berkas

1. Tambahkan berkas di `panel/css/` atau `panel/js/`.
2. Daftarkan di `panel/index.html` pada urutan yang benar.
3. Jalankan `bash tools/bundle-panel.sh` lalu buka hasilnya (atau build APK).

Urutan penting:

- CSS: `base` → `components` → `responsive` → `minecraft` → `minecraft-extra`.
  Berkas berikutnya boleh menimpa aturan sebelumnya.
- JS: `core` harus pertama karena menyediakan `$`, `esc`, `LS`, dan state.
  `i18n` harus terakhir karena memindai DOM dan teks yang sudah dirender.
  Modul lain boleh berbagi nama fungsi global (tidak memakai ES module).

Sebagian besar modul memakai IIFE atau fungsi global biasa. Kalau menambah `const`/`let`
di top-level, pastikan namanya unik di seluruh bundel — semuanya berjalan di satu scope.

## Pratinjau di browser

`panel/index.html` bisa dibuka langsung dari browser desktop. Tampilan akan muncul, tetapi
API tidak tersedia sehingga status tetap "belum terhubung". Cara paling akurat tetap build
APK lalu buka panel dari WebView aplikasi.

## Menjaga satu berkas

Jangan menambahkan aset eksternal (CDN, font, gambar dari internet). Panel harus tetap
berjalan offline dan seluruh isinya masuk ke satu `index.html`. Suara klik saat ini
disematkan sebagai data URI di `js/interactions.js`.
