'use strict';
/* ============================== backups ============================== */
function renderBackups() {
  const box = $('#backupList');
  if (!box) return;
  const arr = S.backups || [];
  const total = arr.reduce((a, b) => a + (b.sizeMB || 0), 0);
  $('#backupSub').textContent = arr.length ? arr.length + ' backup · ' + total + ' MB' : 'Belum ada backup';
  box.innerHTML = arr.length ? arr.map((b) =>
    '<div class="list-item"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#i-archive"/></svg></div>' +
    '<div class="li-main"><div class="li-name">' + esc(b.name) + '</div><div class="li-sub">' + b.sizeMB + ' MB · ' + fmtAgo(b.at) + '</div></div>' +
    '<button class="icon-btn" data-delbackup="' + esc(b.name) + '" title="Hapus"><svg class="ic" style="width:15px;height:15px"><use href="#i-trash"/></svg></button></div>'
  ).join('') : '<div class="empty">Belum ada backup</div>';
}

/* =============================== worlds =============================== */
function renderWorlds() {
  const W = S.worlds || {};
  const seedEl = $('#worldSeed');
  if (seedEl && document.activeElement !== seedEl) seedEl.value = W.seed || '';
  const sub = $('#worldSub');
  if (sub) sub.textContent = W.active ? 'Aktif: ' + W.active : 'Kelola world & seed';
  const box = $('#worldList');
  if (!box) return;
  const list = W.worlds || [];
  box.innerHTML = list.length ? list.map((w) =>
    '<div class="list-item"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#i-box"/></svg></div>' +
    '<div class="li-main"><div class="li-name">' + esc(w.name) +
    (w.active ? ' <span class="pill accent" style="height:17px;font-size:7px;padding:0 5px">aktif</span>' : '') + '</div>' +
    '<div class="li-sub">' + (w.mb != null ? w.mb + ' MB · ' : '') + (w.at ? fmtAgo(w.at) : 'world') + '</div></div>' +
    '<div class="li-actions">' +
    (w.active ? '' : '<button class="icon-btn sm" data-world-use="' + esc(w.name) + '" title="Jadikan world aktif"><svg class="ic"><use href="#i-check"/></svg></button>') +
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

