/* ==================================================================
   Language (ID/EN) + custom dropdown.
   All original text is stored so it can be toggled back and forth without losing text.
   ================================================================== */
(function () {
  'use strict';

  var EN = {
    /* navigation & shell */
    'Ringkasan': 'Overview',
    'Konfigurasi': 'Configuration',
    'Pemain': 'Players',
    'Pengaturan': 'Settings',
    'Menghubungkan…': 'Connecting…',
    'Memuat status server…': 'Loading server status…',
    'Panel belum terhubung.': 'Panel is not connected.',
    'Token panel tidak cocok. Buka ulang aplikasi atau salin token dari Pengaturan.': 'The panel token does not match. Reopen the app or copy the token from Settings.',
    'Menyambung': 'Connecting',
    'Panel lokal tidak merespons.': 'Local panel is not responding.',
    'Coba lagi': 'Retry',
    'Memeriksa…': 'Checking…',
    'Menunggu data dari server': 'Waiting for server data',
    'Uptime': 'Uptime',
    'Mulai server': 'Start server',
    'Hentikan server': 'Stop server',
    'Alamat koneksi': 'Connection addresses',
    'IPv4 / IPv6 · maksimal 2 alamat per edisi · ketuk untuk menyalin': 'IPv4 / IPv6 · max 2 addresses per edition · tap to copy',
    'Ukuran world': 'World size',
    'penyimpanan': 'storage',
    'Traffic real-time': 'Real-time traffic',
    'sampel 100 ms · tetap': '100 ms sample · fixed',
    'Unduh': 'Download',
    'Unggah': 'Upload',
    'Total sesi': 'Session total',
    'Saran': 'Suggestions',
    'Berdasarkan kondisi server': 'Based on server condition',
    'Simpan': 'Save',
    'Semua': 'All',
    'Info': 'Info',
    'Config': 'Config',
    /* dynamic home */
    'Server online': 'Server online',
    'Server offline': 'Server offline',
    'Dikelola panel · siap menerima pemain': 'Managed by panel · ready for players',
    'Berjalan di luar panel · kontrol terbatas': 'Running outside panel · limited control',
    'Tekan mulai untuk menjalankan server': 'Press start to run the server',
    'Belum dihitung': 'Not calculated yet',
    'Tanpa MOTD': 'No MOTD',
    'Server sedang mati. Tekan Mulai untuk menjalankan Pumpkin.': 'Server is stopped. Press Start to run Pumpkin.',
    'Ukuran world belum terbaca. Jalankan server sebentar lalu cek lagi.': 'World size is unknown. Run the server briefly and check again.',
    'Bedrock nonaktif. Aktifkan di Konfigurasi bila pemain HP ingin ikut.': 'Bedrock is off. Enable it in Configuration if mobile players want to join.',
    'Semua terlihat normal. Server siap menerima pemain.': 'Everything looks normal. The server is ready for players.',
    'Bedrock nonaktif': 'Bedrock disabled',
    /* config */
    'Restart server agar perubahan berlaku.': 'Restart the server for changes to take effect.',
    'Umum': 'General',
    'Full': 'Full',
    'Identitas dan aturan dasar': 'Identity and basic rules',
    'Max pemain': 'Max players',
    'Gamemode default': 'Default gamemode',
    'Nama world': 'World name',
    'Spawn protection (blok)': 'Spawn protection (blocks)',
    'Nether': 'Nether',
    'The End': 'The End',
    'Hardcore': 'Hardcore',
    'Wajib whitelist': 'Require whitelist',
    'Port dan jarak render': 'Port and render distance',
    'Alamat / port': 'Address / port',
    'View distance': 'View distance',
    'Simulation distance': 'Simulation distance',
    'Aktif': 'On',
    'Online mode': 'Online mode',
    'Kompresi': 'Compression',
    'Mobile & console': 'Mobile & console',
    'MOTD Bedrock': 'Bedrock MOTD',
    'Kelola world & seed': 'Manage worlds & seed',
    'Simpan seed': 'Save seed',
    'Acak': 'Random',
    'Seed baru berlaku saat world dibuat ulang atau world baru dibuat.': 'A new seed applies when a world is recreated or a new world is created.',
    'Hapus & regenerate hanya bisa saat server berhenti.': 'Delete & regenerate only while the server is stopped.',
    'Tidak ada perubahan': 'No changes',
    'Online sekarang': 'Online now',
    '0 pemain': '0 players',
    'Muat': 'Load',
    'Operator (OP)': 'Operator (OP)',
    'Beri akses perintah admin': 'Grant admin command access',
    'Lv 4': 'Lv 4', 'Lv 3': 'Lv 3', 'Lv 2': 'Lv 2', 'Lv 1': 'Lv 1',
    'Nonaktif': 'Off',
    'Tambah': 'Add',
    'Daftar ban': 'Ban list',
    'Pemain dikenal': 'Known players',
    'Dari usercache — ketuk untuk mengisi nama': 'From usercache — tap to fill the name',
    'Dari usercache': 'From usercache',
    'Ketuk nama untuk mengisi kolom OP atau whitelist.': 'Tap a name to fill the OP or whitelist field.',
    'ketuk untuk mengisi nama': 'tap to fill the name',
    /* folder picker + path editor */
    'Direktori': 'Directory',
    'Pilih folder': 'Choose folder',
    'Pilih file': 'Choose file',
    'Pilih folder ini': 'Use this folder',
    'Pakai folder ini': 'Use this folder',
    'folder di atas': 'parent folder',
    'folder': 'folder',
    'file · ketuk untuk memakai': 'file · tap to use',
    'Folder ini kosong': 'This folder is empty',
    'Ketik manual, atau pilih lewat tombol Dir.': 'Type it manually, or pick it with the Dir button.',
    /* world + plugin */
    'Plugin': 'Plugins',
    'Pasang & kelola plugin server': 'Install & manage server plugins',
    'Buka Plugin': 'Open Plugins',
    'World baru': 'New world',
    'siap dibuat': 'ready',
    'terkunci': 'locked',
    'Daftar world': 'World list',
    'Salin world': 'Copy world',
    'Pindahkan world ke /sdcard': 'Move world to /sdcard',
    'Pindahkan': 'Move',
    'Memindahkan': 'Moving',
    'Salin ke /sdcard': 'Copy to /sdcard',
    'Duplikat jadi world baru': 'Duplicate as a new world',
    'Duplikat': 'Duplicate',
    'Impor .tar.gz': 'Import .tar.gz',
    'Pulihkan backup': 'Restore backup',
    'Pulihkan': 'Restore',
    'Ganti world': 'Switch world',
    'Sedang aktif': 'Currently active',
    'Salin': 'Copy',
    'Default Minecraft (anvil)': 'Minecraft default (anvil)',
    'Pumpkin (pump)': 'Pumpkin (pump)',
    'Seed dan tipe chunk dipakai saat world dibuat.': 'Seed and chunk type are used when a world is created.',
    'Seed dan tipe chunk dipakai saat world dibuat, lalu keduanya terkunci.': 'Seed and chunk type are used when a world is created, then both are locked.',
    'Terkunci: world sudah ada. Hapus semua world dulu untuk memakai seed/tipe chunk baru.': 'Locked: a world already exists. Delete all worlds first to use a new seed/chunk type.',
    'Tipe chunk': 'Chunk type',
    /* settings */
    'Jaringan panel': 'Panel network',
    'Nama server, alamat panel, port, dan token': 'Server name, panel address, port, and token',
    'Eksperimental': 'Experimental',
    'Menu backup dibuka': 'Backup menu revealed',
    'Nama server': 'Server name',
    'Alamat panel': 'Panel address',
    'IP saat ini': 'Current IP',
    'Pilih IP': 'Select IP',
    'HP ini': 'This phone',
    'Data seluler': 'Mobile data',
    'Jaringan': 'Network',
    'Wi-Fi': 'Wi-Fi',
    'Ethernet': 'Ethernet',
    'VPN': 'VPN',
    'Host custom': 'Custom host',
    'Port panel': 'Panel port',
    'Token akses panel': 'Panel access token',
    'Salin token': 'Copy token',
    'Token disalin': 'Token copied',
    'Token belum tersedia': 'Token not available yet',
    'Token dibuat otomatis. Untuk LAN buka http://IP:PORT/?token=TOKEN': 'The token is generated automatically. For LAN access open http://IP:PORT/?token=TOKEN',
    'Simpan & terapkan': 'Save & apply',
    'Saat ini hanya bisa dibuka dari HP ini.': 'Currently only accessible from this phone.',
    'Panel hanya bisa dibuka dari HP ini.': 'The panel can only be opened from this phone.',
    'Panel terbuka di jaringan. Gunakan token untuk mengakses dari perangkat lain.': 'The panel is available on the network. Use the token to access it from another device.',
    'Tampilan': 'Appearance',
    'Tema dan warna aksen': 'Theme and accent color',
    'Mode gelap': 'Dark mode',
    'Gelap': 'Dark',
    'Suara tombol Minecraft': 'Minecraft button sound',
    'Warna aksen': 'Accent color',
    'Aplikasi Android': 'Android app',
    'Tentang & catatan': 'About & notes',
    'Panduan singkat untuk pengguna': 'A quick guide for users',
    'Cara memakai panel, keamanan token, akses dari HP lain, dan tips merawat world.': 'How to use the panel, token safety, access from another device, and world care tips.',
    'Baca tentang & catatan': 'Read about & notes',
    'Menu native: ganti binary Pumpkin & data server': 'Native menu: swap Pumpkin binary & server data',
    'Buka menu aplikasi': 'Open app menu',
    'Tukar binary Pumpkin': 'Swap Pumpkin binary',
    'Backup world': 'World backup',
    'Belum ada backup': 'No backups yet',
    'Buat backup': 'Create backup',
    'berjalan…': 'running…',
    'Path & runtime': 'Path & runtime',
    'Ketuk ikon pensil untuk mengubah path': 'Tap the pencil icon to change paths',
    'Folder server (host)': 'Server folder (host)',
    'Perintah start': 'Start command',
    'Launcher': 'Launcher',
    'File HTML': 'HTML file',
    'Zona bahaya': 'Danger zone',
    'Aksi ini langsung menghentikan proses server': 'This immediately stops the server process',
    'Matikan paksa (SIGKILL)': 'Force kill (SIGKILL)',
    'Muat ulang semua data': 'Reload all data',
    /* settings + experimental network section */
    'Bagian pengaturan': 'Settings sections',
    'Hijau limau': 'Lime green',
    'Hijau muda': 'Light green',
    'Hijau': 'Green',
    'Hijau teal': 'Emerald',
    'Teal': 'Teal',
    'Sian': 'Cyan',
    'Pilihan warna aksen': 'Accent color choices',
    'Localhost': 'Localhost',
    'Custom': 'Custom',
    'IP custom': 'Custom IP',
    'Binary aktif bisa diganti kapan saja dari menu ini (impor dari /sdcard atau file lain). Versi lama disimpan sebagai cadangan dan bisa ditukar balik. Tombol Back Android juga membuka menu ini.': 'The active binary can be swapped anytime from this menu (import from /sdcard or another file). The old version is kept as a backup and can be swapped back. The Android Back button also opens this menu.',
    'Panel privat: tanpa token, HTML panel tidak dikirim ke jaringan. Untuk akses dari perangkat lain buka': 'Private panel: without a token, the panel HTML is not sent over the network. To open it from another device use',
    'Panel web': 'Web panel',
    'Buka panel dari perangkat lain di jaringan yang sama': 'Open the panel from another device on the same network',
    'Panel web online': 'Web panel online',
    'Default mati. Panel tetap bisa dipakai di HP ini.': 'Off by default. The panel still works on this phone.',
    'Mati': 'Off',
    'Nyala': 'On',
    'Panel web mati: hanya bisa dibuka dari HP ini (127.0.0.1).': 'Web panel is off: it can only be opened from this phone (127.0.0.1).',
    'Panel web dimatikan': 'Web panel turned off',
    'Host custom kosong — memakai IP saat ini': 'Custom host is empty — using the current IP',
    'Host custom tidak aktif — memakai IP saat ini': 'Custom host is not active — using the current IP',
    'Nyalakan Panel web online dulu': 'Turn on Web panel online first',
    'Alamat server disimpan — restart server agar berlaku': 'Server address saved — restart the server to apply it',
    'Isi IP custom dulu': 'Enter a custom IP first',
    'Alamat server Pumpkin': 'Pumpkin server address',
    'Java & Bedrock memakai alamat bind yang sama': 'Java & Bedrock use the same bind address',
    'Alamat bind server': 'Server bind address',
    'Localhost (hanya HP ini)': 'Localhost (this phone only)',
    'IP saat ini (LAN)': 'Current IP (LAN)',
    'Simpan alamat server': 'Save server address',
    'Alamat ini ditulis ke pumpkin.toml. Restart server agar berlaku.': 'This address is written to pumpkin.toml. Restart the server to apply it.',
    'Atur alamat server di Pengaturan → Eksperimental': 'Set the server address in Settings → Experimental',
    'Aktivitas terakhir': 'Recent activity',
    'Tiga baris log terbaru': 'Latest three log lines',
    'Buka Console': 'Open Console',
    'Menunggu aktivitas…': 'Waiting for activity…',
    'Belum ada aktivitas.': 'No activity yet.',
    /* about popup */
    'Tentang Pumpkin Panel': 'About Pumpkin Panel',
    'Panel lokal untuk mengatur server Pumpkin langsung dari HP ini.': 'A local panel to manage your Pumpkin server right from this phone.',
    'Satu tempat untuk server': 'One place for your server',
    'Mulai dan hentikan server di Beranda, baca log dan kirim perintah di Console, ubah pumpkin.toml di Config, lalu kelola OP dan whitelist di Pemain.': 'Start and stop the server on Home, read logs and send commands in Console, edit pumpkin.toml in Config, then manage OP and whitelist in Players.',
    'Bekerja tanpa internet': 'Works without internet',
    'Semua kontrol berjalan di perangkat ini. Hanya tombol Unduh Pumpkin di aplikasi yang memakai koneksi internet.': 'All controls run on this device. Only the Download Pumpkin button in the app uses an internet connection.',
    'Token adalah kunci panel': 'The token is the panel key',
    'Token dibuat otomatis dan wajib dipakai. Jangan bagikan tautan yang berisi token. Ganti token kapan saja di Eksperimental → Panel web.': 'The token is generated automatically and is required. Do not share links that contain it. You can change it anytime in Experimental → Web panel.',
    'Buka dari perangkat lain': 'Open from another device',
    'Panel web default mati, jadi panel hanya bisa dibuka dari HP ini. Nyalakan Panel web online, pilih IP saat ini, lalu buka tautan bertoken dari HP atau laptop lain di Wi-Fi yang sama.': 'The web panel is off by default, so the panel only opens on this phone. Turn on Web panel online, choose the current IP, then open the token link from another phone or laptop on the same Wi-Fi.',
    'Alamat server terpisah': 'Separate server address',
    'Alamat panel dan alamat server Pumpkin diatur terpisah. Gunakan IP saat ini agar pemain di Wi-Fi yang sama bisa masuk, atau Localhost bila hanya untuk HP ini.': 'The panel address and the Pumpkin server address are configured separately. Use the current IP so players on the same Wi-Fi can join, or Localhost if only this phone needs access.',
    'Rawat world': 'Take care of your world',
    'Buat backup world secara berkala, terutama sebelum mengganti versi Pumpkin atau memasang plugin. Backup disimpan di folder server.': 'Create world backups regularly, especially before changing the Pumpkin version or installing plugins. Backups are stored in the server folder.',
    'Kalau panel bermasalah': 'If the panel has problems',
    'Tutup lalu buka kembali aplikasi. Di Console tersedia tombol unduh log untuk melihat penyebab masalah.': 'Close and reopen the app. Console has a log download button to help find the cause.',
    'Proyek tidak resmi. Pumpkin © Pumpkin-MC (GPLv3); Minecraft © Mojang Studios. Tidak berafiliasi.': 'Unofficial project. Pumpkin © Pumpkin-MC (GPLv3); Minecraft © Mojang Studios. Not affiliated.',
    'Mengerti': 'Got it',
    'Beranda': 'Home',
    'Lainnya': 'More',
    'Status & kontrol server': 'Status & server control',
    'Log langsung dan perintah': 'Live logs and commands',
    'Online, OP, dan whitelist': 'Online, OP, and whitelist',
    'Panel, path, dan backup': 'Panel, paths, and backups',
    'Umum, eksperimental, dan path': 'General, experimental, and paths',
    'Terang': 'Light',
    'Belum ada sampel': 'No samples yet',
    'Ubah nilai lalu simpan.': 'Change the value, then save.',
    'Token salah': 'Wrong token',
    'Konfirmasi': 'Confirm',
    'Yakin?': 'Are you sure?',
    'Batal': 'Cancel',
    'Lanjutkan': 'Continue',
    'Pilih': 'Select',
    'Edit': 'Edit',
    'Ubah config →': 'Edit config →',
    'Cari di log…': 'Search logs…',
    'Ketik perintah server… (mis. list, say halo, time set day)': 'Type a server command… (e.g. list, say hello, time set day)',
    'Nama pemain': 'Player name',
    'Alasan (opsional)': 'Reason (optional)',
    'Pumpkin Server': 'Pumpkin Server',
    '192.168.1.10': '192.168.1.10',
    'kosong = tidak diubah': 'empty = unchanged',
    'Perintah hanya bisa dikirim jika server dijalankan dari panel ini. Gunakan tombol Mulai di Ringkasan.': 'Commands can only be sent when the server is started from this panel. Use the Start button on Overview.',
    /* dialog */
    'Hentikan server?': 'Stop the server?',
    'Restart server?': 'Restart the server?',
    'Buat backup world?': 'Create a world backup?',
    'Matikan paksa?': 'Force kill?',
    'Hapus backup?': 'Delete backup?',
    'Ganti nama world': 'Rename world',
    'Buat ulang world': 'Recreate world',
    'Hapus world': 'Delete world',
    'Kick': 'Kick',
    'Cabut ban': 'Unban',
    'Cabut OP': 'Remove OP',
    /* toast & status */
    'Tersimpan.': 'Saved.',
    'Server berjalan': 'Server running',
    'Server berhenti': 'Server stopped',
    'Server belum berjalan': 'Server is not running',
    'Server berjalan di luar panel': 'Server is running outside the panel',
    'Server memang sudah berjalan': 'Server is already running',
    'Server dijalankan di luar panel — perintah tidak bisa dikirim': 'Server was started outside the panel — commands cannot be sent',
    'Server dimatikan paksa': 'Server force killed',
    'Menyimpan world…': 'Saving world…',
    'Restart dijadwalkan': 'Restart scheduled',
    'Konfigurasi disimpan': 'Configuration saved',
    'Pengaturan panel disimpan': 'Panel settings saved',
    'Path disimpan': 'Path saved',
    'Perubahan dibatalkan': 'Changes discarded',
    'Tampilan log dibersihkan': 'Log view cleared',
    'Backup dimulai': 'Backup started',
    'Backup dihapus': 'Backup deleted',
    'Backup gagal': 'Backup failed',
    'Data dimuat ulang': 'Data reloaded',
    'Semua data dimuat ulang': 'All data reloaded',
    'Menu aplikasi hanya tersedia di APK': 'The app menu is only available in the APK',
    'Isi host custom dulu': 'Enter a custom host first',
    'Isi nama pemain': 'Enter a player name',
    'Isi seed dulu': 'Enter a seed first',
    'Port harus 1-65535': 'Port must be 1-65535',
    'OP dicabut': 'OP removed',
    'Dijadikan OP': 'Promoted to OP',
    'Whitelist diaktifkan': 'Whitelist enabled',
    'Whitelist dimatikan': 'Whitelist disabled',
    'Dihapus dari whitelist': 'Removed from whitelist',
    'Dihapus': 'Deleted',
    'Mencari panel di 127.0.0.1…': 'Searching for panel at 127.0.0.1…',
    'Panel pindah ke': 'Panel moved to',
    'Ubah launcher': 'Change launcher',
    'Ubah folder server': 'Change server folder',
    'Ubah perintah start': 'Change start command',
    'Muat ulang': 'Reload',
    'Menu aplikasi': 'App menu',
    'Mulai / hentikan server': 'Start / stop server',
    'Auto-scroll': 'Auto-scroll',
    'Bersihkan tampilan': 'Clear view',
    'Unduh log': 'Download log',

    /* ---- status & overview (extra) ---- */
    'Terhubung': 'Connected',
    'Terputus': 'Disconnected',
    'Terhenti': 'Stopped',
    'Panel offline': 'Panel offline',
    'Versi': 'Version',
    'Max pemain Java': 'Max Java players',
    'Max pemain Bedrock': 'Max Bedrock players',
    'Tidak ada log yang cocok': 'No matching logs',
    'tidak diketahui': 'unknown',

    /* ---- players ---- */
    'Server sedang offline': 'The server is offline',
    'Belum ada pemain yang bergabung': 'No players have joined yet',
    'online sekarang': 'online now',
    'operator · online': 'operator · online',
    'Jadikan': 'Make OP',
    'Jadikan OP': 'Make OP',
    'Cabut': 'Remove',
    'Ban dicabut': 'Ban removed',
    'Belum ada operator': 'No operators yet',
    'Whitelist kosong': 'Whitelist is empty',
    'Aktif — hanya pemain terdaftar': 'On — only listed players',
    'Nonaktif — semua boleh masuk': 'Off — everyone can join',
    'Belum ada pemain diban': 'No players banned yet',
    'tanpa alasan': 'no reason',
    'Tidak ada ban aktif': 'No active bans',
    'Belum ada data usercache': 'No usercache data yet',
    'Belum ada data pemain di usercache.': 'No player data in usercache yet.',
    'Pemain akan dikeluarkan dari server.': 'The player will be kicked from the server.',
    'Pemain boleh masuk kembali.': 'The player can join again.',
    'Pemain tidak bisa masuk lagi. Alasan bisa diisi lewat kartu Daftar ban.': 'The player cannot join again. A reason can be added in the Ban list card.',
    'Pemain ini tidak bisa masuk saat whitelist aktif.': 'This player cannot join while the whitelist is on.',
    'Pilih mode bermain untuk pemain ini.': 'Choose a game mode for this player.',
    'Pemain tidak lagi punya akses admin.': 'The player no longer has admin access.',
    'Pemain mendapat akses perintah admin (level 4).': 'The player gets admin command access (level 4).',

    /* ---- worlds ---- */
    'Hapus': 'Delete',
    'Gagal': 'Failed',
    'Pakai': 'Use',
    'Belum ada world': 'No worlds yet',
    'World ini akan dipakai saat server dijalankan.': 'This world will be used when the server starts.',
    'Gagal memindahkan world': 'Failed to move world',
    'World dipindahkan': 'World moved',
    'Salin world ke /sdcard?': 'Copy world to /sdcard?',
    'Gagal menyalin world': 'Failed to copy world',
    'World disalin': 'World copied',
    'Folder world disalin menjadi world baru di folder server. Server harus berhenti dan ruang penyimpanan harus cukup.': 'The world folder will be copied as a new world inside the server folder. The server must be stopped and there must be enough storage.',
    'Gagal menduplikat world': 'Failed to duplicate world',
    'World diduplikat': 'World duplicated',
    'Buat ulang': 'Recreate',
    'World dibuat ulang': 'World recreated',
    'World dihapus': 'World deleted',
    'Folder world beserta seluruh isinya dihapus permanen. Backup dulu bila ragu.': 'The world folder and all its contents will be permanently deleted. Back it up first if unsure.',
    'Ganti nama': 'Rename',
    'Buat ulang (reset)': 'Recreate (reset)',
    'Semua isi world ini dihapus dan dibuat baru saat server dijalankan': 'All contents of this world will be deleted and recreated when the server starts',
    'dipindahkan ke': 'moved to',
    'disalin ke': 'copied to',
    'dengan seed': 'with seed',
    'Nama baru untuk': 'New name for',
    '(server harus berhenti).': '(the server must be stopped).',
    '. World akan hilang dari daftar karena berada di luar folder server. Pastikan server berhenti dan ruang penyimpanan cukup.': '. The world will disappear from the list because it is outside the server folder. Make sure the server is stopped and there is enough storage.',
    '. Server harus berhenti dan ruang penyimpanan harus cukup.': '. The server must be stopped and there must be enough storage.',

    /* ---- backup & console actions ---- */
    'Gagal mengirim perintah': 'Failed to send command',
    'Pemain akan terputus. World akan disimpan otomatis sebelum keluar.': 'Players will be disconnected. The world is saved automatically before shutdown.',
    'Hentikan': 'Stop',
    'Menghentikan server…': 'Stopping the server…',
    'Menjalankan server…': 'Starting the server…',
    'Server mulai · pantau di Console kapan saja': 'Server started · watch it in Console anytime',
    'Server akan berhenti, menyimpan world, lalu mulai lagi.': 'The server will stop, save the world, then start again.',
    'Hentikan server dulu agar backup konsisten': 'Stop the server first so the backup is consistent',
    '. Proses kompresi bisa memakan waktu dan CPU. Lanjutkan?': '. Compression can take time and CPU. Continue?',
    'Pilih file .tar.gz': 'Choose a .tar.gz file',
    'Gagal mengimpor backup': 'Failed to import backup',
    'Backup diimpor': 'Backup imported',
    'Backup dipulihkan': 'Backup restored',
    'Hentikan server dulu sebelum memulihkan backup': 'Stop the server before restoring a backup',
    'Pulihkan backup?': 'Restore backup?',
    'Isi': 'The contents of',
    'akan menimpa world dengan nama yang sama, lalu world itu dijadikan aktif.': 'will overwrite the world with the same name, then that world becomes active.',
    'Tidak bisa dibatalkan.': 'This cannot be undone.',
    'Gagal memulihkan backup': 'Failed to restore backup',
    'Gagal menyalin backup': 'Failed to copy backup',
    'Backup disalin': 'Backup copied',
    'Proses server dibunuh tanpa menyimpan world. Data yang belum tersimpan bisa hilang.': 'The server process is killed without saving the world. Unsaved data may be lost.',

    /* ---- messages from the Java backend ---- */
    'Backup sedang berjalan': 'A backup is already running',
    'Backup tidak berisi folder world yang valid': 'The backup does not contain a valid world folder',
    'Backup tidak ditemukan': 'Backup not found',
    'Belum ada world untuk dibackup': 'There are no worlds to back up',
    'File backup harus di /sdcard': 'The backup file must be on /sdcard',
    'Folder backup tidak bisa dibuat': 'The backup folder cannot be created',
    'Folder tujuan tidak boleh di dalam world': 'The destination cannot be inside the world',
    'Format tidak dikenal': 'Unknown format',
    'Gagal mengganti nama folder world': 'Failed to rename the world folder',
    'Gagal menghapus backup': 'Failed to delete the backup',
    'Gagal menyalin backup (ruang cukup?)': 'Failed to copy the backup (is there enough space?)',
    'Gagal menyalin world (ruang penyimpanan cukup?)': 'Failed to copy the world (is there enough storage?)',
    'Hentikan server dulu sebelum membuat ulang world': 'Stop the server before recreating the world',
    'Hentikan server dulu sebelum memindahkan world': 'Stop the server before moving the world',
    'Hentikan server dulu sebelum mengganti nama world': 'Stop the server before renaming the world',
    'Hentikan server dulu sebelum menghapus world': 'Stop the server before deleting the world',
    'Hentikan server dulu sebelum menyalin world': 'Stop the server before copying the world',
    'Host tidak boleh kosong': 'Host cannot be empty',
    'Isi token akses LAN dulu sebelum membuka panel ke jaringan.': 'Enter a panel access token before opening the panel to the network.',
    'Nama world diubah': 'World renamed',
    'Nama world sudah dipakai': 'That world name is already in use',
    'Nama world terlalu panjang untuk disalin': 'The world name is too long to copy',
    'Nama world tidak valid': 'Invalid world name',
    'Perintah kosong': 'Empty command',
    'Pilih file backup .tar.gz': 'Choose a .tar.gz backup file',
    'Pilih folder di luar folder server': 'Choose a folder outside the server folder',
    'Pilih folder lain, bukan folder server': 'Choose another folder, not the server folder',
    'Pilih folder tujuan di /sdcard': 'Choose a destination folder on /sdcard',
    'Port tidak valid (1-65535)': 'Invalid port (1-65535)',
    'Seed disimpan; berlaku saat world dibuat ulang': 'Seed saved; it applies when the world is recreated',
    'Seed tidak valid': 'Invalid seed',
    'Seed disimpan': 'Seed saved',
    'Server sudah berhenti': 'The server is already stopped',
    'Server sudah berjalan': 'The server is already running',
    'Server tidak berjalan dari panel ini': 'The server is not running from this panel',
    'Sudah OP': 'Already OP',
    'Tunggu backup selesai dulu': 'Wait for the backup to finish first',
    'World asal tidak ditemukan': 'Source world not found',
    'World dihapus; akan dibuat baru saat server dijalankan': 'World deleted; it will be recreated when the server starts',
    'World tidak ditemukan': 'World not found',
    'pumpkin.toml belum ada. Jalankan server sekali agar file konfigurasi dibuat.': 'pumpkin.toml does not exist yet. Run the server once so the configuration file is created.',
    'pumpkin.toml tidak ditemukan': 'pumpkin.toml not found',

    /* ---- dialogs, plugin placeholder, chunk help ---- */
    'Memuat…': 'Loading…',
    'Folder tidak bisa dibaca': 'Folder cannot be read',
    'Muat konfigurasi dulu': 'Load the configuration first',
    'Belum ada data konfigurasi': 'No configuration data yet',
    'Permintaan gagal': 'Request failed',
    'Respon bukan JSON': 'Response is not JSON',
    'Koneksi timeout': 'Connection timed out',
    'Salin token ini untuk akses LAN:': 'Copy this token for LAN access:',
    'gagal': 'failed',
    'Segera datang.': 'Coming soon.',
    'Panel plugin belum aktif. Nanti dari sini plugin bisa dipasang, diaktifkan, dan dihapus tanpa membuka folder server.': 'The plugin panel is not active yet. Later you will be able to install, enable, and delete plugins from here without opening the server folder.',
    'Sementara ini plugin masih dikelola lewat folder': 'For now, plugins are still managed through the',
    'di folder server.': 'folder in the server folder.',
    'Format penyimpanan world.': 'World storage format.',
    'Format region vanilla. World bisa dibuka server Java/Paper dan alat dunia lain.': 'Vanilla region format. The world can be opened by Java/Paper servers and other world tools.',
    'Format khusus Pumpkin. Lebih cepat dan hemat ruang, tetapi tidak bisa dibaca Paper atau Minecraft biasa.': 'Pumpkin-specific format. Faster and more space-efficient, but cannot be read by Paper or regular Minecraft.',
    'Tipe hanya dipakai saat world dibuat (atau dibuat ulang), dan world yang sudah ada tidak dikonversi. Jadi pilih dulu sebelum world pertama dibuat.': 'The type is only used when a world is created (or recreated), and existing worlds are not converted. Choose it before creating the first world.',

    /* ---- static markup (index.html) ---- */
    'cuaca cerah': 'clear weather',
    'Naik': 'Up',
    'Awal': 'Home',
    'Mulai atau hentikan server': 'Start or stop the server',
    'Restart server': 'Restart server',
    'Cari log': 'Search log',
    'Perintah server': 'Server command',
    'Selamat datang!': 'Welcome!',
    'contoh: 1794660286376045931': 'e.g. 1794660286376045931',
    'Apa itu tipe chunk?': 'What is a chunk type?',
    'Pilih direktori': 'Choose directory',
    'Kelola plugin langsung dari panel: pasang, aktifkan, dan hapus tanpa menyentuh folder server.': 'Manage plugins directly from the panel: install, enable, and delete them without touching the server folder.',
    'Atur alamat server di Pengaturan \u2192 Eksperimental': 'Set the server address in Settings \u2192 Experimental',
    'Proyek tidak resmi. Pumpkin \u00a9 Pumpkin-MC (GPLv3); Minecraft \u00a9 Mojang Studios. Tidak berafiliasi.': 'Unofficial project. Pumpkin \u00a9 Pumpkin-MC (GPLv3); Minecraft \u00a9 Mojang Studios. Not affiliated.',
    'Pastikan server berhenti dan ruang penyimpanan cukup.': 'Make sure the server is stopped and there is enough storage.',
    'aktif': 'active',
    'Backup berisi folder world dalam satu file .tar.gz. Hentikan server dulu agar isinya konsisten; backup bisa dipulihkan, disalin ke /sdcard, atau diimpor dari /sdcard.': 'A backup contains the world folders in one .tar.gz file. Stop the server first so the contents are consistent; a backup can be restored, copied to /sdcard, or imported from /sdcard.'
  };

  /* pattern-based dynamic text */
  var EN_PAT = [
    /* counters run first: "2 pemain diban" is a count, not "<name> was banned" */
    [/^(\d+) pemain online$/, '$1 players online'],
    [/^(\d+) pemain diban$/, '$1 players banned'],
    [/^(\d+) pemain di usercache$/, '$1 players in usercache'],
    [/^(\d+) baris log di Console$/, '$1 log lines in Console'],
    [/^(\d+) perubahan belum disimpan$/, '$1 unsaved changes'],
    [/^Aktif: (.+)$/, 'Active: $1'],
    [/^(Online|Offline) · (\d+) pemain · (.+)$/, '$1 · $2 players · $3'],
    [/^(\d+) MB dipakai world (.+)$/, '$1 MB used by world $2'],
    [/^Ukuran world (.+) MB$/, 'World size $1 MB'],
    [/^World (\d+) MB\. Buat backup dan pertimbangkan menghapus region lama\.$/, 'World is $1 MB. Make a backup and consider deleting old regions.'],
    [/^CPU tinggi \((\d+)%\)\. Turunkan view distance atau kurangi plugin\.$/, 'High CPU ($1%). Lower view distance or remove plugins.'],
    [/^RAM proses (\d+) MB\. Tutup aplikasi lain bila HP mulai lambat\.$/, 'Process RAM is $1 MB. Close other apps if the phone starts to lag.'],
    [/^Backup selesai: (.+)$/, 'Backup finished: $1'],
    [/^Disalin: (.+)$/, 'Copied: $1'],
    [/^Nama diisi: (.+)$/, 'Name filled: $1'],
    [/^Seed acak: (.+)$/, 'Random seed: $1'],
    [/^Gamemode: (.+)$/, 'Gamemode: $1'],
    [/^Auto-scroll aktif$/, 'Auto-scroll on'],
    [/^Auto-scroll nonaktif$/, 'Auto-scroll off'],
    [/^(.+) sekarang OP$/, '$1 is now OP'],
    [/^(.+) di-kick$/, '$1 was kicked'],
    [/^(.+) diban$/, '$1 was banned'],
    [/^(.+) ditambahkan ke whitelist$/, '$1 added to whitelist'],
    [/^World aktif: (.+)$/, 'Active world: $1'],
    [/^Panel pindah ke (.+) — tunggu 2 detik$/, 'Panel moved to $1 — wait 2 seconds'],
    [/^World diganti nama$/, 'World renamed'],
    [/^(.+) MB$/, '$1 MB'],

    /* world actions */
    [/^Pakai world (.+)\?$/, 'Use world $1?'],
    [/^Pindahkan world (.+)\?$/, 'Move world $1?'],
    [/^Duplikat world (.+)\?$/, 'Duplicate world $1?'],
    [/^Buat ulang world (.+)\?$/, 'Recreate world $1?'],
    [/^Hapus world (.+)\?$/, 'Delete world $1?'],
    [/^Hapus (.+) dari whitelist\?$/, 'Delete $1 from the whitelist?'],
    [/^Cabut OP (.+)\?$/, 'Remove OP from $1?'],
    [/^Jadikan OP (.+)\?$/, 'Make $1 OP?'],
    [/^Cabut ban (.+)\?$/, 'Unban $1?'],
    [/^Kick (.+)\?$/, 'Kick $1?'],
    [/^Ban (.+)\?$/, 'Ban $1?'],
    [/^Gamemode — (.+)$/, 'Gamemode — $1'],
    [/^Nama baru untuk (.+) \(server harus berhenti\)\.$/, 'New name for $1 (the server must be stopped).'],
    [/^Memindahkan (.+)…$/, 'Moving $1…'],
    [/^Menyalin (.+)…$/, 'Copying $1…'],
    [/^Menduplikat (.+)…$/, 'Duplicating $1…'],

    /* panel status + folder picker */
    [/^Panel web online di (.+)$/, 'Web panel online at $1'],
    [/^Error panel: (.+)$/, 'Panel error: $1'],
    [/^Folder tidak ditemukan: (.+)$/, 'Folder not found: $1'],
    [/^Folder tidak bisa dibaca: (.+)$/, 'Folder cannot be read: $1'],

    /* backend messages that carry a value */
    [/^Backup diimpor: (.+)$/, 'Backup imported: $1'],
    [/^Backup disalin ke (.+)$/, 'Backup copied to $1'],
    [/^Binary pumpkin belum ada: (.+)$/, 'Pumpkin binary not found: $1'],
    [/^Binary tidak bisa dipakai: (.+)$/, 'Binary cannot be used: $1'],
    [/^Folder server tidak bisa dibuat: (.+)$/, 'The server folder cannot be created: $1'],
    [/^Gagal memulihkan world (.+) \(ruang cukup\?\)$/, 'Failed to restore world $1 (is there enough space?)'],
    [/^Gagal memulihkan: (.+)$/, 'Failed to restore: $1'],
    [/^Host (.+) bukan alamat perangkat ini$/, 'Host $1 is not an address of this device'],
    [/^Port (\d+) tidak bisa dipakai: (.+)$/, 'Port $1 cannot be used: $2'],
    [/^World (.+) dihapus$/, 'World $1 deleted'],
    [/^World (.+) dipulihkan dan dijadikan aktif$/, 'World $1 restored and set as active'],
    [/^World dipindahkan ke (.+)$/, 'World moved to $1'],
    [/^World disalin ke (.+)$/, 'World copied to $1'],
    [/^World tersalin ke (.+)$/, 'World copied to $1']
  ];

  function lookup(s) {
    if (Object.prototype.hasOwnProperty.call(EN, s)) return EN[s];
    for (var i = 0; i < EN_PAT.length; i++) {
      var m = EN_PAT[i][0].exec(s);
      if (m) return s.replace(EN_PAT[i][0], EN_PAT[i][1]);
    }
    /* Relative times are embedded in composed rows such as
       "229 MB · 2 jam lalu · chunk pump", so replace them anywhere. */
    var rel = s
      .replace(/(\d+) menit lalu/g, '$1m ago')
      .replace(/(\d+) jam lalu/g, '$1h ago')
      .replace(/(\d+) hari lalu/g, '$1d ago')
      .replace(/\bbaru saja\b/g, 'just now');
    if (rel !== s) return rel;
    return null;
  }
  /* Used for dynamic text set after the active language changes (toast, hint, status). */
  window.ppT = function (s) { return lookup(String(s == null ? '' : s)) || String(s == null ? '' : s); };

  var SKIP = 'script,style,#logList,#knownNames';
  function skipped(node) {
    var el = node.nodeType === 1 ? node : node.parentNode;
    try { return !!(el && el.closest && el.closest(SKIP)); } catch (e) { return false; }
  }

  function trText(node, lang) {
    if (!node || node.nodeType !== 3 || skipped(node)) return;
    if (node.__ppSrc === undefined) node.__ppSrc = node.nodeValue;
    var src = node.__ppSrc;
    var key = src.replace(/\s+/g, ' ').trim();
    if (!key) return;
    if (lang === 'id') {
      if (node.nodeValue !== src) node.nodeValue = src;
      return;
    }
    var out = lookup(key);
    if (out === null) return;
    var lead = src.match(/^\s*/)[0];
    var tail = src.match(/\s*$/)[0];
    var next = lead + out + tail;
    if (node.nodeValue !== next) node.nodeValue = next;
  }

  function trAttrs(el, lang) {
    if (!el || el.nodeType !== 1 || skipped(el)) return;
    ['title', 'placeholder', 'aria-label'].forEach(function (a) {
      if (!el.hasAttribute(a)) return;
      if (!el.__ppAttrs) el.__ppAttrs = {};
      if (el.__ppAttrs[a] === undefined) el.__ppAttrs[a] = el.getAttribute(a);
      var src = el.__ppAttrs[a];
      var key = src.replace(/\s+/g, ' ').trim();
      var next = lang === 'id' ? src : (lookup(key) || src);
      if (el.getAttribute(a) !== next) el.setAttribute(a, next);
    });
  }

  function walk(root, lang) {
    if (!root) return;
    if (root.nodeType === 3) { trText(root, lang); return; }
    if (root.nodeType !== 1) return;
    if (skipped(root)) return;
    trAttrs(root, lang);
    var tw = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
    var n;
    while ((n = tw.nextNode())) trText(n, lang);
    var els = root.querySelectorAll('[title],[placeholder],[aria-label]');
    for (var i = 0; i < els.length; i++) trAttrs(els[i], lang);
  }

  var lang = 'en';
  function readLang() {
    var saved = LS.get('pp.lang', null);
    if (saved === 'en' || saved === 'id') return saved;
    try {
      if (window.PanelNative && typeof PanelNative.getLang === 'function') {
        var n = PanelNative.getLang();
        if (n === 'en' || n === 'id') return n;
      }
    } catch (e) {}
    return 'en';
  }
  function applyLang(next) {
    lang = next === 'en' ? 'en' : 'id';
    LS.set('pp.lang', lang);
    document.documentElement.lang = lang;
    /* Redraw programmatic text first, then walk once: the observer is installed
       after boot, so anything written after walk would stay in the old language. */
    if (typeof window.ppOnLang === 'function') { try { window.ppOnLang(lang); } catch (e) {} }
    if (typeof window.paintConn === 'function') { try { window.paintConn(); } catch (e) {} }
    walk(document.body, lang);
    var sw = document.getElementById('langSwitch');
    if (sw) {
      var bs = sw.querySelectorAll('button[data-lang]');
      for (var i = 0; i < bs.length; i++) {
        var on = bs[i].dataset.lang === lang;
        bs[i].classList.toggle('active', on);
        bs[i].setAttribute('aria-pressed', on ? 'true' : 'false');
      }
    }
  }
  window.ppLang = function () { return lang; };
  window.ppApplyLang = applyLang;

  /* ---------- custom dropdown ---------- */
  var ddId = 0;
  /* Clicking outside a dropdown closes every .dd menu (these menus are created dynamically). */
  document.addEventListener('click', function (ev) {
    /* Tapping an empty area inside the menu also closes it (without selecting an option). */
    if (ev.target && ev.target.closest && ev.target.closest('.dd-menu') && !ev.target.closest('.dd-opt')) {
      var all = document.querySelectorAll('.dd.open');
      for (var j = 0; j < all.length; j++) all[j].classList.remove('open');
      return;
    }
    var open = document.querySelectorAll('.dd.open');
    for (var i = 0; i < open.length; i++) {
      if (!open[i].contains(ev.target)) open[i].classList.remove('open');
    }
  });
  function enhanceSelect(sel) {
    if (!sel || sel.__dd || sel.dataset.nodd === '1') return;
    sel.__dd = true;
    var wrap = document.createElement('div');
    wrap.className = 'dd';
    if (sel.style.width) wrap.style.width = sel.style.width;
    if (sel.classList.contains('mono')) wrap.classList.add('mono');
    if (sel.id) wrap.id = sel.id + 'Dd';
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'input dd-btn';
    var menu = document.createElement('div');
    menu.className = 'dd-menu';
    menu.setAttribute('role', 'listbox');
    var txt = document.createElement('span');
    txt.className = 'dd-txt';
    var car = document.createElement('svg');
    car.className = 'ic';
    car.setAttribute('viewBox', '0 0 24 24');
    car.setAttribute('aria-hidden', 'true');
    car.innerHTML = '<path d="M6 9l6 6 6-6"/>';
    btn.appendChild(txt);
    btn.appendChild(car);
    wrap.appendChild(btn);
    wrap.appendChild(menu);
    sel.parentNode.insertBefore(wrap, sel.nextSibling);
    sel.style.display = 'none';
    sel.setAttribute('tabindex', '-1');
    menu.id = 'ddMenu' + (++ddId);

    function opts() { return Array.prototype.slice.call(sel.options); }
    function labelOf(o) {
      if (!o) return '—';
      var t = (o.label || o.textContent || '').replace(/\s+/g, ' ').trim();
      return t || o.value;
    }
    function paint() {
      var o = opts()[sel.selectedIndex];
      txt.textContent = labelOf(o);
      menu.innerHTML = '';
      opts().forEach(function (op) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'dd-opt' + (op.selected ? ' active' : '');
        b.setAttribute('role', 'option');
        b.setAttribute('aria-selected', op.selected ? 'true' : 'false');
        b.textContent = labelOf(op);
        b.addEventListener('click', function () {
          sel.selectedIndex = op.index;
          sel.dispatchEvent(new Event('change', { bubbles: true }));
          close();
          paint();
          btn.focus();
        });
        menu.appendChild(b);
      });
      if (lang === 'en') walk(menu, 'en');
    }
    function open() {
      document.querySelectorAll('.dd.open').forEach(function (d) {
        if (d !== wrap) d.classList.remove('open');
      });
      wrap.classList.add('open');
      var act = menu.querySelector('.dd-opt.active') || menu.querySelector('.dd-opt');
      if (act) { try { act.scrollIntoView({ block: 'nearest' }); } catch (e) {} }
    }
    function close() { wrap.classList.remove('open'); }

    btn.addEventListener('click', function (ev) {
      ev.stopPropagation();
      if (wrap.classList.contains('open')) close(); else open();
    });
    btn.addEventListener('keydown', function (ev) {
      var list = opts();
      if (!list.length) return;
      var i = sel.selectedIndex;
      if (ev.key === 'ArrowDown' || ev.key === 'ArrowUp') {
        ev.preventDefault();
        i = Math.max(0, Math.min(list.length - 1, i + (ev.key === 'ArrowDown' ? 1 : -1)));
        sel.selectedIndex = i;
        sel.dispatchEvent(new Event('change', { bubbles: true }));
        paint();
      } else if (ev.key === 'Enter' || ev.key === ' ') {
        ev.preventDefault();
        open();
      } else if (ev.key === 'Escape') {
        close();
      }
    });
    sel.addEventListener('change', paint);
    sel.__ddPaint = paint;
    if (window.MutationObserver) {
      new MutationObserver(paint).observe(sel, { childList: true, subtree: true, characterData: true });
    }
    paint();
  }

  function enhanceAll() {
    document.querySelectorAll('select.input').forEach(enhanceSelect);
  }
  window.ppRefreshSelects = function () {
    enhanceAll();
    document.querySelectorAll('select.input').forEach(function (el) {
      if (el.__ddPaint) el.__ddPaint();
    });
  };

  /* ---------- observer: translate new elements and new selects ---------- */
  var pending = null;
  function onMutations(list) {
    for (var i = 0; i < list.length; i++) {
      var m = list[i];
      if (m.type === 'attributes') {
        if (m.target && m.target.tagName === 'SELECT') enhanceSelect(m.target);
        continue;
      }
      for (var j = 0; j < m.addedNodes.length; j++) {
        var n = m.addedNodes[j];
        if (n.nodeType === 1) {
          if (n.tagName === 'SELECT' && n.classList.contains('input')) enhanceSelect(n);
          var sels = n.querySelectorAll && n.querySelectorAll('select.input');
          if (sels) for (var k = 0; k < sels.length; k++) enhanceSelect(sels[k]);
        }
        walk(n, lang);
      }
    }
  }

  function boot() {
    applyLang(readLang());
    enhanceAll();
    var sw = document.getElementById('langSwitch');
    if (sw) {
      sw.addEventListener('click', function (ev) {
        var b = ev.target.closest && ev.target.closest('button[data-lang]');
        if (!b) return;
        applyLang(b.dataset.lang);
        try { if (window.PanelNative && PanelNative.setLang) PanelNative.setLang(lang); } catch (e) {}
      });
    }
    if (window.MutationObserver) {
      new MutationObserver(onMutations).observe(document.body, { childList: true, subtree: true });
    }
  }

  if (document.body) boot();
  else document.addEventListener('DOMContentLoaded', boot);
})();

