# Catatan proyek — Pumpkin Panel (Android)

Dokumen ini merangkum hal-hal yang tidak selalu terlihat dari kode: cara build, fitur yang
tersembunyi di menu native, model keamanan, dan checklist sebelum repo dipublikasikan.

Versi aplikasi: **1.0.0** (`versionCode` 5). Jangan naikkan versi tanpa mengubah
`build.sh` dan `app/build.gradle.kts` bersamaan.

## Arsitektur singkat

```
MainActivity (WebView + menu native)
        │  HTTP + SSE ke 127.0.0.1:8087
        ▼
ApiServer (Java, pengganti server.js)
        │
        ├─ Pumpkin.java   proses, log, world, backup, config, settings
        ├─ PanelService   foreground service + wakelock
        └─ Store          SharedPreferences
```

- Panel web: `panel/` (HTML/CSS/JS modular) → dibundel menjadi
  `app/src/main/assets/index.html` — satu file, tanpa framework/CDN, **selalu dari aset APK**.
  Aset hasil bundling tidak di-commit; `build.sh` membuatnya ulang.
- Data server: `/sdcard/server` (world, `pumpkin.toml`, logs, plugins).
- Binary: `/data/data/com.pumpkin.panel/files/pumpkin`.
- Backup: `/sdcard/server/backups`.
- Log diagnostik panel: `/sdcard/server/panel.log`.

## Build

### Termux (dipakai untuk APK rilis ini)

```bash
cd pumpkin-server-android

# APK polosan
./build.sh
# hasil: build/apk/pumpkin-panel-plain.apk

# APK full (binary resmi ikut dibundel)
./build.sh --with-binary /sdcard/pumpkin-aarch64-android
# hasil: build/apk/pumpkin-panel-full.apk
```

`build.sh` memakai `aapt2`, `javac`, `d8`, `zip`, dan `apksigner`; tidak butuh Gradle.
Keystore debug dibuat otomatis dan **tidak** ikut ke repo (`.gitignore`). Panel dan binary
ditulis ke folder staging `build/assets/`; binari tidak pernah masuk ke folder sumber/git.

### Notifikasi service

Notifikasi foreground hanya tampil selama server Pumpkin berjalan. Saat server berhenti
(dari panel maupun aksi **Hentikan server** di notifikasi), service keluar dari foreground
dan notifikasi dibatalkan, tetapi service panel tetap hidup agar API lokal masih bisa
dipakai. Saat server dimulai lagi, notifikasi muncul kembali.

### Panel modular

Panel tidak lagi berupa satu berkas sumber. Semua HTML/CSS/JS ada di `panel/`; `build.sh`
menjalankan `tools/bundle-panel.sh` untuk menggabungkannya menjadi satu aset sebelum
`aapt2`. Untuk melihat hasil bundling saja:

```bash
bash tools/bundle-panel.sh /tmp/preview.html
```

Detail struktur dan aturan urutan ada di [`docs/PANEL.md`](PANEL.md).

### GitHub Actions

Workflow `.github/workflows/build.yml` memakai `build.sh` yang sama di runner Ubuntu
(JDK 17 + Android SDK 34 + build-tools 34.0.0). APK hasil CI ditandatangani dengan debug
key milik runner, jadi berbeda dari APK lokal. Uninstal dulu jika berpindah sumber APK.

### Setelah mengubah panel

Edit berkas di `panel/` (bukan aset hasil bundling), lalu build ulang. `build.sh`
otomatis membundel ulang; panel tidak pernah dibaca dari `/sdcard` saat runtime.

```bash
./build.sh
```

## Keamanan panel

Panel bersifat **privat**:

- **Token akses dibuat otomatis** saat pertama kali aplikasi berjalan (16 byte acak, hex).
- **Semua** akses panel — HTML maupun `/api/*`, dari localhost atau LAN — wajib menyertakan token:
  `?token=...` untuk halaman atau header `x-token` untuk API.
- WebView aplikasi membuka panel dengan token di URL, jadi pengguna tidak perlu mengetik apa pun.
- Tanpa token yang cocok, server **tidak mengirim HTML panel sama sekali**; yang tampil hanya
  halaman kunci sederhana berisi form token.
- Token bisa dilihat/di-salip dari **Pengaturan → Token akses panel → Salin token**.
- Bind ke `0.0.0.0`/IP LAN tanpa token tetap dipaksa ke `127.0.0.1` sebagai pengaman.
- WebView memblokir semua request `http`/`https` non-lokal. Panel tidak memuat aset eksternal.

Token disimpan di SharedPreferences perangkat. **Jangan** commit token, keystore, atau file
`/sdcard/gh-token.txt` ke repo.

## Fitur yang tidak menonjol di UI

Beberapa fitur sengaja tidak diberi tombol besar, tetapi tersedia:

1. **Menu native aplikasi** — ikon ponsel di topbar panel, kartu *Aplikasi Android* di
   Pengaturan, atau tombol **Back** Android saat panel terbuka.
2. **Tukar binary Pumpkin** — impor dari `/sdcard`, `/sdcard/Download`, `/sdcard/Download/Pumpkin`,
   atau `/sdcard/server`. Binary lama disimpan sebagai `pumpkin.bak`; tombol *Tukar dengan
   cadangan* membaliknya dua arah.
3. **Deteksi binary glibc** — impor/start menolak ELF Linux (`ld-linux-aarch64`) dan hanya
   menerima build Android (`/system/bin/linker64`).
4. **Unduh log** dan **salin alamat** — tombol kecil di console dan kartu alamat koneksi.
5. **Seed world** — simpan/acak seed dari panel Konfigurasi.
6. **Tema & aksen** — mode gelap/terang dan dropdown warna aksen ringkas di Pengaturan.
7. **Suara tombol Minecraft** — bisa dimatikan lewat toggle di Pengaturan.
8. **API JSON lokal** — seluruh aksi panel memakai `/api/*`; berguna untuk otomasi di perangkat
   yang sama. Lihat daftar route di README.
9. **Diagnostik `panel.log`** — mencatat `START`, `EXIT`, dan `EXEC GAGAL` beserta interpreter ELF.
10. **Folder app cadangan** — tombol *Pakai folder app* memindahkan data server ke
    `/data/data/com.pumpkin.panel/files/server` bila `/sdcard` bermasalah.
11. **Menu backup rahasia** — kartu *Backup world* dan tombol Backup di Ringkasan disembunyikan
    sejak awal. Tekan lama kartu **Tentang & catatan** (±900 ms) untuk membukanya. Status
    disimpan di localStorage `pp.menu.backup`; hapus key itu untuk menyembunyikan lagi.
12. **Panel web online (eksperimental)** — kartu di bagian **Eksperimental** berisi nama server,
    alamat panel, port, dan token. Sakelar **Panel web online** default **mati**; saat mati
    seluruh pengaturan dibuat abu-abu dan read-only, dan server panel dipaksa bind ke
    `127.0.0.1`. Saat dinyalakan, alamat/port/token baru dipakai.
13. **Alamat server Pumpkin terpisah (eksperimental)** — pilihan tiga mode (localhost / IP saat
    ini / custom) untuk bind **server Pumpkin**, terpisah dari alamat panel web. Nilainya ditulis
    ke `networking.java.address` dan `networking.bedrock.nethernet.address` di `pumpkin.toml`
    dengan port lama dipertahankan. Restart server agar berlaku.
14. **Warna aksen gradasi hijau** — dropdown ringkas berisi enam warna limau → hijau → teal,
    default `#22c55e`. Nilai lama bawaan (`#ffaa00`/`#ff8a3d`) otomatis dimigrasikan ke hijau.
    Warna aksen juga menggerakkan permukaan "Minecraft green": tombol primer, tombol power,
    strip/glow hero saat online, toggle, avatar, ikon cepat, chip IP, dan badge merek.
15. **Tentang & catatan** — sekarang berupa tombol yang membuka popup panduan pengguna
    (pemakaian, token, akses perangkat lain, alamat server, backup). Catatan build tidak lagi
    ditampilkan di panel; dokumen ini tetap untuk pengembang.
16. **Aktivitas terakhir** — kartu di bawah Beranda menampilkan tiga baris log terbaru dan
    tombol pintas ke Console, mengisi ruang kosong di ujung Beranda.

## Bagian Umum / Eksperimental

Menu **Lainnya** dibagi menjadi dua bagian lewat segmented control. Setiap kali menu dibuka,
bagian **Umum** selalu aktif kembali. Pengaturan jaringan yang masih dalam pengujian berada di
**Eksperimental**, sehingga tidak menambah panjang halaman utama pengaturan.

## Panel web online vs server Pumpkin

Dua alamat ini sengaja dipisah:

| Kebutuhan | Penyimpanan | Default |
|---|---|---|
| Bind panel web (HP lain membuka panel) | SharedPreferences `host` + `panelOnline` | `127.0.0.1`, online **mati** |
| Bind server Pumpkin (pemain masuk) | `pumpkin.toml` → `networking.java.address`, `networking.bedrock.nethernet.address` | `0.0.0.0` |

- `Store.panelOnline()` default `false`; `ApiServer.openListener()` memaksa loopback selama mati.
- `PUT /api/settings` menerima `panelOnline` dan memicu `relisten()` bila nilainya berubah.
- `MainActivity.panelHost()` memakai `127.0.0.1` selama panel offline, jadi WebView tetap aman.
- Alamat di Beranda dihitung dari bind **server Pumpkin** (TOML), bukan dari alamat panel.
- Alamat bind `0.0.0.0`/`::` ditampilkan sebagai IP perangkat di kartu alamat dan subtitle Beranda,
  karena alamat wildcard tidak bisa dipakai pemain untuk menyambung.
- Mode bind server selalu diturunkan dari isi `pumpkin.toml` saat data dimuat; pilihan yang belum
  disimpan tetap dipertahankan saat berpindah bagian Umum/Eksperimental.
- Saat panel online dinyalakan dengan host custom yang kosong atau sudah tidak aktif di perangkat,
  panel otomatis memakai IP saat ini dan memberi tahu pengguna, supaya sakelar tidak terkunci.
- Rebase hanya dijadwalkan bila alamat panel benar-benar berubah (mis. loopback → LAN atau
  sebaliknya); mengaktifkan panel di alamat yang sama tidak memuat ulang halaman.

## UX console

Panel console di HP memakai posisi **fixed** yang diukur dari bawah topbar sampai atas tabbar
(lewat variabel `--console-top`/`--console-bottom` yang diperbarui saat resize/orientasi).
Log berada di dalam panel bergaris dengan scroll internal, input perintah di atas log, dan baris
chip perintah tetap terlihat di atas navigasi bawah — log tidak lagi menembus tabbar.

## UX koneksi panel

Banner merah **"Panel belum terhubung"** tidak muncul seketika. Alasannya: saat WebView baru
membuka panel, halaman + token masih dimuat dan SSE bisa putus-nyambung sebentar.

- Saat pertama membuka panel, banner diberi tenggang **3 detik**; setelah pernah terhubung,
  tenggangnya **1,5 detik**.
- Selama tenggang, status pill menampilkan **Menyambung** (kuning), bukan merah.
- Kalau penyebabnya token, pesannya spesifik: *Token panel tidak cocok*.
- Alamat API selalu memakai `location.origin` saat halaman dilayani server panel, jadi alamat
  lama di `localStorage` (IP/port basi) tidak lagi membuat banner muncul palsu.
- Kalau tetap 401, panel otomatis mencoba token yang ditanam server (`window.__PP_TOKEN`) lalu
  origin halaman sebelum menyerah.
- **Status koneksi ditentukan oleh API, bukan SSE.** Satu request `/api/*` yang sukses langsung
  menandai panel terhubung (dan menyembunyikan banner). SSE yang putus — misalnya saat layar mati
  atau WebView menunda stream — tidak lagi membuat banner merah; panel mengecek `/api/ping`
  dulu sebelum menyatakan offline.
- **Penyebab SSE putus (diperbaiki):** `PanelService` mengirim `status` dari main thread, sehingga
  penulisan socket SSE melempar `NetworkOnMainThreadException`. Server lalu menutup koneksi dan
  panel mengira dirinya offline. Sekarang semua tulisan SSE dijalankan di executor `sse-tx`
  (single thread, daemon), jadi stream bertahan dan banner tidak muncul palsu.
- Watchdog 4 detik memanggil `/api/ping` saat panel terlihat dan status masih offline, dan
  `visibilitychange` memeriksa ulang saat aplikasi dibuka kembali.

Menu/opsi yang sedang dipilih memakai **latar putih + teks hitam** dengan garis dan tanda centang,
agar jelas berbeda dari field yang harus diisi manual. Aturan ini berlaku untuk dropdown,
segmented control, sidebar, dan tab bar di semua tema.

## Perilaku yang disengaja

- `targetSdk 28` supaya binary ELF boleh dieksekusi dari folder data aplikasi (kebijakan W^X
  Android 10+). Ini juga memberi akses legacy ke `/sdcard`.
- Panel hanya menampilkan **IPv4**, unik (tanpa duplikat), maksimal **2 alamat per edisi**
  (Java dan Bedrock).
- Traffic real-time memakai sampel **100 ms tetap** dan hanya membaca `/proc/net/dev`.
- Console hanya merender ±400 baris terakhir; log penuh tetap tersedia lewat unduhan.
- Semua data panel berjalan lokal; tidak ada telemetri.

## Checklist sebelum publikasi GitHub

- [ ] `versionName` tetap `1.0.0`, `versionCode` 5 (atau ubah keduanya sekaligus).
- [ ] `app/src/main/assets/index.html` dihasilkan ulang dari `panel/` (`bash tools/bundle-panel.sh`).
- [ ] Tidak ada token/keystore/path pribadi di repo (`git status`, `git grep -i token`).
- [ ] README menjelaskan dua varian (polosan dan full) serta kebutuhan internet untuk unduhan.
- [ ] Repo publik; `LICENSE` (Apache-2.0) dan `NOTICE` (atribusi Pumpkin/Mojang) sudah sesuai.
- [ ] APK `pumpkin-panel-plain.apk` dan `pumpkin-panel-full.apk` diuji pasang di perangkat bersih.
