'use strict';
/* ============================== backups ============================== */
function renderBackups() {
  const box = $('#backupList');
  if (!box) return;
  const arr = S.backups || [];
  const total = arr.reduce((a, b) => a + (b.bytes || (b.sizeMB || 0) * 1048576), 0);
  $('#backupSub').textContent = arr.length ? arr.length + ' backup · ' + fmtBytes(total) : 'Belum ada backup';
  box.innerHTML = arr.length ? arr.map((b) =>
    '<div class="list-item"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#i-archive"/></svg></div>' +
    '<div class="li-main"><div class="li-name">' + esc(b.name) + '</div>' +
    '<div class="li-sub">' + fmtBytes(b.bytes || (b.sizeMB || 0) * 1048576) + ' · ' + fmtAgo(b.at) + '</div></div>' +
    '<div class="li-actions">' +
    '<button class="icon-btn sm" data-restorebackup="' + esc(b.name) + '" title="Pulihkan backup"><svg class="ic"><use href="#i-restart"/></svg></button>' +
    '<button class="icon-btn sm" data-exportbackup="' + esc(b.name) + '" title="Salin ke /sdcard"><svg class="ic"><use href="#i-download"/></svg></button>' +
    '<button class="icon-btn sm" data-delbackup="' + esc(b.name) + '" title="Hapus"><svg class="ic"><use href="#i-trash"/></svg></button>' +
    '</div></div>'
  ).join('') : '<div class="empty">Belum ada backup</div>';
}

/* =============================== worlds =============================== */
function renderWorlds() {
  const W = S.worlds || {};
  const list = W.worlds || [];
  const hasWorld = list.length > 0;
  const seedEl = $('#worldSeed');
  if (seedEl) {
    if (document.activeElement !== seedEl) seedEl.value = W.seed || '';
    seedEl.disabled = hasWorld;
  }
  ['#btnSeedSave', '#btnSeedRandom'].forEach((s) => { const b = $(s); if (b) b.disabled = hasWorld; });
  const sub = $('#worldSub');
  if (sub) sub.textContent = W.active ? 'Aktif: ' + W.active : 'Kelola world & seed';
  const chunk = getPath(S.cfgParsed || {}, 'world.chunk.type') || '';
  const pill = $('#worldChunkPill');
  if (pill) pill.textContent = 'chunk: ' + (chunk || '?');
  /* Keep a config value outside the two known options visible instead of blank. */
  const chunkSel = $('#chunkTypeSel');
  if (chunkSel) {
    if (chunk && !Array.prototype.some.call(chunkSel.options, (o) => o.value === chunk)) {
      const opt = document.createElement('option');
      opt.value = chunk;
      opt.textContent = chunk;
      chunkSel.appendChild(opt);
    }
    chunkSel.disabled = hasWorld;
  }
  const state = $('#newWorldState');
  if (state) {
    state.textContent = hasWorld ? 'terkunci' : 'siap dibuat';
    state.classList.toggle('accent', !hasWorld);
  }
  const newBox = $('#newWorldBox');
  if (newBox) newBox.classList.toggle('locked', hasWorld);
  const hint = $('#newWorldHint');
  if (hint) hint.textContent = hasWorld
    ? 'Terkunci: world sudah ada. Hapus semua world dulu untuk memakai seed/tipe chunk baru.'
    : 'Seed dan tipe chunk dipakai saat world dibuat, lalu keduanya terkunci.';
  const box = $('#worldList');
  if (!box) return;
  box.innerHTML = list.length ? list.map((w) =>
    '<div class="list-item"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#i-box"/></svg></div>' +
    '<div class="li-main"><div class="li-name">' + esc(w.name) +
    (w.active ? ' <span class="pill accent" style="height:17px;font-size:7px;padding:0 5px">aktif</span>' : '') + '</div>' +
    '<div class="li-sub">' + [w.mb != null ? w.mb + ' MB' : '', w.at ? fmtAgo(w.at) : 'world', chunk ? 'chunk ' + chunk : ''].filter(Boolean).join(' · ') + '</div></div>' +
    '<div class="li-actions">' +
    '<button class="icon-btn sm" data-world-move="' + esc(w.name) + '" title="Pindahkan world ke /sdcard"><svg class="ic"><use href="#i-move"/></svg></button>' +
    '<button class="icon-btn sm" data-world-export="' + esc(w.name) + '" title="Salin ke /sdcard"><svg class="ic"><use href="#i-download"/></svg></button>' +
    '<button class="icon-btn sm" data-world-dup="' + esc(w.name) + '" title="Duplikat jadi world baru"><svg class="ic"><use href="#i-copy"/></svg></button>' +
    '<button class="icon-btn sm" data-world-use="' + esc(w.name) + '" title="' + (w.active ? 'Sedang aktif' : 'Ganti world') + '"' + (w.active ? ' disabled' : '') + '><svg class="ic"><use href="#i-swap"/></svg></button>' +
    '<button class="icon-btn sm" data-world-rename="' + esc(w.name) + '" title="Ganti nama"><svg class="ic"><use href="#i-edit"/></svg></button>' +
    '<button class="icon-btn sm" data-world-regen="' + esc(w.name) + '" title="Buat ulang (reset)"><svg class="ic"><use href="#i-refresh"/></svg></button>' +
    '<button class="icon-btn sm" data-world-del="' + esc(w.name) + '" title="Hapus world"><svg class="ic"><use href="#i-trash"/></svg></button>' +
    '</div></div>'
  ).join('') : '<div class="empty">Belum ada world</div>';
}
function refreshWorlds() {
  return api('/api/worlds').then((w) => { S.worlds = w; renderWorlds(); renderHomeInfo(); }).catch(() => {});
}
function makeSeed() {
  const n = Math.floor(Math.random() * 9e17) + 1e17;
  return String(n);
}
function genWorldAction(kind, name) {
  if (kind === 'use') {
    confirmDlg({ title: 'Pakai world ' + name + '?', msg: 'World ini akan dipakai saat server dijalankan.', ok: 'Pakai' })
      .then(async (ok) => {
        if (!ok) return;
        try {
          await api('/api/config', { method: 'PUT', body: { patch: [{ key: 'default_level_name', value: name }] } });
          toast('World aktif: ' + name);
          await refreshAll();
        } catch (e) { toast(e.message, 'err'); }
      });
    return;
  }
  if (kind === 'move') {
    dirDlg('/sdcard', 'dir').then(async (dest) => {
      if (!dest) return;
      const ok = await confirmDlg({
        title: 'Pindahkan world ' + name + '?',
        msg: 'Folder <b>' + esc(name) + '</b> dipindahkan ke <span class="mono">' + esc(dest) +
          '</span>. World akan hilang dari daftar karena berada di luar folder server. ' +
          'Pastikan server berhenti dan ruang penyimpanan cukup.',
        ok: 'Pindahkan',
        danger: true
      });
      if (!ok) return;
      try {
        toast('Memindahkan ' + name + '…');
        const r = await api('/api/worlds/move', { method: 'POST', body: { name, dest } });
        if (!r.ok) { toast(r.msg || 'Gagal memindahkan world', 'err'); return; }
        toast(r.msg || 'World dipindahkan');
        await refreshAll();
      } catch (e) { toast(e.message, 'err'); }
    });
    return;
  }
  if (kind === 'export') {
    dirDlg('/sdcard', 'dir').then(async (dest) => {
      if (!dest) return;
      confirmDlg({
        title: 'Salin world ke /sdcard?',
        msg: 'World <b>' + esc(name) + '</b> disalin ke <span class="mono">' + esc(dest) + '</span>. Server harus berhenti dan ruang penyimpanan harus cukup.',
        ok: 'Salin'
      }).then(async (ok) => {
        if (!ok) return;
        try {
          toast('Menyalin ' + name + '…');
          const r = await api('/api/worlds/export', { method: 'POST', body: { name, dest } });
          if (!r.ok) { toast(r.msg || 'Gagal menyalin world', 'err'); return; }
          toast(r.msg || 'World disalin');
        } catch (e) { toast(e.message, 'err'); }
      });
    });
    return;
  }
  if (kind === 'dup') {
    confirmDlg({
      title: 'Duplikat world ' + name + '?',
      msg: 'Folder world disalin menjadi world baru di folder server. Server harus berhenti dan ruang penyimpanan harus cukup.',
      ok: 'Duplikat'
    }).then(async (ok) => {
      if (!ok) return;
      try {
        toast('Menduplikat ' + name + '…');
        const r = await api('/api/worlds/copy', { method: 'POST', body: { name } });
        if (!r.ok) { toast(r.msg || 'Gagal menduplikat world', 'err'); return; }
        toast(r.msg || 'World diduplikat');
        await refreshAll();
      } catch (e) { toast(e.message, 'err'); }
    });
    return;
  }
  if (kind === 'rename') {
    textDlg({ title: 'Ganti nama world', msg: 'Nama baru untuk ' + name + ' (server harus berhenti).', value: name })
      .then(async (to) => {
        if (!to || to === name) return;
        try {
          const r = await api('/api/worlds/rename', { method: 'POST', body: { from: name, to } });
          if (!r.ok) { toast(r.msg || 'Gagal', 'err'); return; }
          toast('World diganti nama');
          await refreshAll();
        } catch (e) { toast(e.message, 'err'); }
      });
    return;
  }
  if (kind === 'regen') {
    const seed = ($('#worldSeed').value || '').trim();
    confirmDlg({
      title: 'Buat ulang world ' + name + '?',
      msg: 'Semua isi world ini dihapus dan dibuat baru saat server dijalankan' + (seed ? ' dengan seed <b>' + esc(seed) + '</b>' : '') + '. <b>Tidak bisa dibatalkan.</b>',
      ok: 'Buat ulang', danger: true
    }).then(async (ok) => {
      if (!ok) return;
      try {
        const r = await api('/api/worlds/regenerate', { method: 'POST', body: { name, seed } });
        if (!r.ok) { toast(r.msg || 'Gagal', 'err'); return; }
        toast(r.msg || 'World dibuat ulang');
        await refreshAll();
      } catch (e) { toast(e.message, 'err'); }
    });
    return;
  }
  if (kind === 'del') {
    confirmDlg({ title: 'Hapus world ' + name + '?', msg: 'Folder world beserta seluruh isinya dihapus permanen. Backup dulu bila ragu.', ok: 'Hapus', danger: true })
      .then(async (ok) => {
        if (!ok) return;
        try {
          const r = await api('/api/worlds/delete', { method: 'POST', body: { name } });
          if (!r.ok) { toast(r.msg || 'Gagal', 'err'); return; }
          toast('World dihapus');
          await refreshAll();
        } catch (e) { toast(e.message, 'err'); }
      });
  }
}

