'use strict';
/* ============================== events ============================== */
document.addEventListener('click', (ev) => {
  const t = ev.target;
  const tab = t.closest('[data-tab]');
  if (tab) { ev.preventDefault(); setTab(tab.dataset.tab); return; }

  const cp = t.closest('[data-copy]');
  if (cp) { copyText(cp.dataset.copy || cp.textContent.trim()); return; }

  const qc = t.closest('[data-cmd]');
  if (qc) { sendCmd(qc.dataset.cmd); return; }

  const lv = t.closest('[data-level]');
  if (lv) { S.level = lv.dataset.level; $$('[data-level]').forEach((b) => b.classList.toggle('active', b === lv)); renderConsole(); return; }

  const kick = t.closest('[data-kick]');
  if (kick) {
    const name = kick.dataset.kick;
    confirmDlg({ title: 'Kick ' + name + '?', msg: 'Pemain akan dikeluarkan dari server.', ok: 'Kick', danger: true })
      .then(async (ok) => {
        if (!ok) return;
        try { await api('/api/players/kick', { method: 'POST', body: { name } }); toast(name + ' di-kick'); }
        catch (e) { toast(e.message, 'err'); }
      });
    return;
  }

  const opBtn = t.closest('[data-op]');
  if (opBtn) {
    const name = opBtn.dataset.op;
    const isOp = opBtn.dataset.opstate === '1';
    confirmDlg({
      title: (isOp ? 'Cabut OP ' : 'Jadikan OP ') + name + '?',
      msg: isOp ? 'Pemain tidak lagi punya akses admin.' : 'Pemain mendapat akses perintah admin (level 4).',
      ok: isOp ? 'Cabut' : 'Jadikan',
      danger: isOp
    }).then(async (ok) => {
      if (!ok) return;
      try {
        await api('/api/players/' + (isOp ? 'deop' : 'op'), { method: 'POST', body: { name, level: 4 } });
        toast(isOp ? 'OP dicabut' : 'Dijadikan OP');
        refreshPlayers();
      } catch (e) { toast(e.message, 'err'); }
    });
    return;
  }

  const gmBtn = t.closest('[data-gm]');
  if (gmBtn) {
    const name = gmBtn.dataset.gm;
    choiceDlg({
      title: 'Gamemode — ' + name,
      msg: 'Pilih mode bermain untuk pemain ini.',
      options: [
        { label: 'Survival', value: 'survival', primary: true },
        { label: 'Creative', value: 'creative' },
        { label: 'Adventure', value: 'adventure' },
        { label: 'Spectator', value: 'spectator' }
      ]
    }).then(async (opt) => {
      if (!opt) return;
      try {
        await api('/api/players/gamemode', { method: 'POST', body: { name, mode: opt.value } });
        toast('Gamemode: ' + opt.value);
      } catch (e) { toast(e.message, 'err'); }
    });
    return;
  }

  const banBtn = t.closest('[data-ban]');
  if (banBtn) {
    const name = banBtn.dataset.ban;
    confirmDlg({
      title: 'Ban ' + name + '?',
      msg: 'Pemain tidak bisa masuk lagi. Alasan bisa diisi lewat kartu Daftar ban.',
      ok: 'Ban', danger: true
    }).then(async (ok) => {
      if (!ok) return;
      try { await api('/api/players/ban', { method: 'POST', body: { name } }); toast(name + ' diban'); refreshPlayers(); }
      catch (e) { toast(e.message, 'err'); }
    });
    return;
  }

  const unbanBtn = t.closest('[data-unban]');
  if (unbanBtn) {
    const name = unbanBtn.dataset.unban;
    confirmDlg({ title: 'Cabut ban ' + name + '?', msg: 'Pemain boleh masuk kembali.', ok: 'Cabut ban' })
      .then(async (ok) => {
        if (!ok) return;
        try { await api('/api/players/unban', { method: 'POST', body: { name } }); toast('Ban dicabut'); refreshPlayers(); }
        catch (e) { toast(e.message, 'err'); }
      });
    return;
  }

  const pe = t.closest('[data-path]');
  if (pe) { editPath(pe.dataset.path); return; }

  const deop = t.closest('[data-deop]');
  if (deop) {
    const name = deop.dataset.deop;
    confirmDlg({ title: 'Cabut OP ' + name + '?', msg: 'Pemain tidak lagi punya akses admin.', ok: 'Cabut', danger: true })
      .then(async (ok) => {
        if (!ok) return;
        try { await api('/api/players/deop', { method: 'POST', body: { name } }); toast('OP dicabut'); refreshPlayers(); }
        catch (e) { toast(e.message, 'err'); }
      });
    return;
  }
  const unwl = t.closest('[data-unwl]');
  if (unwl) {
    const name = unwl.dataset.unwl;
    confirmDlg({ title: 'Hapus ' + name + ' dari whitelist?', msg: 'Pemain ini tidak bisa masuk saat whitelist aktif.', ok: 'Hapus', danger: true })
      .then(async (ok) => {
        if (!ok) return;
        try { await api('/api/players/whitelist-remove', { method: 'POST', body: { name } }); toast('Dihapus dari whitelist'); refreshPlayers(); }
        catch (e) { toast(e.message, 'err'); }
      });
    return;
  }
  const fill = t.closest('[data-fill]');
  if (fill) {
    const n = fill.dataset.fill;
    $('#opName').value = n; $('#wlName').value = n;
    toast('Nama diisi: ' + n);
    return;
  }
  const wUse = t.closest('[data-world-use]');
  if (wUse) { genWorldAction('use', wUse.dataset.worldUse); return; }
  const wRen = t.closest('[data-world-rename]');
  if (wRen) { genWorldAction('rename', wRen.dataset.worldRename); return; }
  const wReg = t.closest('[data-world-regen]');
  if (wReg) { genWorldAction('regen', wReg.dataset.worldRegen); return; }
  const wDel = t.closest('[data-world-del]');
  if (wDel) { genWorldAction('del', wDel.dataset.worldDel); return; }
  const pth = t.closest('[data-path]');
  if (pth) { editPath(pth.dataset.path); return; }
  const del = t.closest('[data-delbackup]');
  if (del) {
    const name = del.dataset.delbackup;
    confirmDlg({ title: 'Hapus backup?', msg: esc(name), ok: 'Hapus', danger: true })
      .then(async (ok) => {
        if (!ok) return;
        try { await api('/api/backup', { method: 'DELETE', body: { name } }); toast('Backup dihapus'); api('/api/backups').then((b) => { S.backups = b.backups || []; renderBackups(); }); }
        catch (e) { toast(e.message, 'err'); }
      });
    return;
  }
  const ac = t.closest('[data-accent]');
  if (ac) {
    S.accent = ac.dataset.accent; LS.set('pp.accent', S.accent); applyTheme();
    const pick = $('#accentPick');
    if (pick) pick.classList.remove('open');
    return;
  }
});

function refreshPlayers() {
  api('/api/players').then((p) => { S.players = p; renderPlayers(); }).catch(() => {});
}

/* power & quick actions */
$('#btnPower').addEventListener('click', doPower);
$('#btnRestart').addEventListener('click', doRestart);
$('#btnSaveWorld').addEventListener('click', doSaveWorld);
$('#btnBackup').addEventListener('click', doBackup);
$('#btnBackup2').addEventListener('click', doBackup);
$('#btnRestartFromCfg').addEventListener('click', doRestart);
$('#btnRefreshPlayers').addEventListener('click', refreshPlayers);
$('#btnReload').addEventListener('click', async () => {
  try { await refreshAll(); toast('Data dimuat ulang'); } catch (e) { toast(e.message, 'err'); }
});
$('#btnReloadCfg').addEventListener('click', async () => {
  try { await refreshAll(); toast('Semua data dimuat ulang'); } catch (e) { toast(e.message, 'err'); }
});
$('#btnRetryConn').addEventListener('click', () => {
  if (S.es) { try { S.es.close(); } catch (e) {} S.es = null; }
  connFailAt = 0;
  connBannerOn = false;
  if (connBannerTimer) { clearTimeout(connBannerTimer); connBannerTimer = null; }
  if (SERVED) adoptBase(DEFAULT_BASE);
  paintConn();
  bootstrap();
});
$('#btnKill').addEventListener('click', async () => {
  const ok = await confirmDlg({ title: 'Matikan paksa?', msg: 'Proses server dibunuh tanpa menyimpan world. Data yang belum tersimpan bisa hilang.', ok: 'SIGKILL', danger: true });
  if (!ok) return;
  try { await api('/api/kill', { method: 'POST' }); toast('Server dimatikan paksa', 'err'); } catch (e) { toast(e.message, 'err'); }
});

/* console */
$('#cmdForm').addEventListener('submit', (ev) => {
  ev.preventDefault();
  const inp = $('#cmdInput');
  const v = inp.value;
  inp.value = '';
  sendCmd(v);
});
$('#cmdInput').addEventListener('keydown', (ev) => {
  if (ev.key === 'ArrowUp' || ev.key === 'ArrowDown') {
    ev.preventDefault();
    if (!S.cmdHist.length) return;
    if (ev.key === 'ArrowUp') S.histIdx = S.histIdx < 0 ? S.cmdHist.length - 1 : Math.max(0, S.histIdx - 1);
    else S.histIdx = S.histIdx < 0 ? -1 : Math.min(S.cmdHist.length - 1, S.histIdx + 1);
    $('#cmdInput').value = S.histIdx >= 0 ? S.cmdHist[S.histIdx] : '';
  }
});
$('#logSearch').addEventListener('input', (ev) => { S.q = ev.target.value.trim(); renderConsole(); });
$('#btnClearLog').addEventListener('click', () => { S.logs = []; $('#logList').innerHTML = ''; toast('Tampilan log dibersihkan'); });
$('#btnDownloadLog').addEventListener('click', () => {
  const text = S.logs.map((e) => (e.time ? e.time + ' ' : '') + (e.level || e.src || '') + ' ' + (e.msg || '')).join('\n');
  const blob = new Blob([text || '(kosong)'], { type: 'text/plain' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'pumpkin-log-' + new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-') + '.txt';
  a.click(); setTimeout(() => URL.revokeObjectURL(a.href), 4000);
});
$('#btnAutoScroll').addEventListener('click', (ev) => {
  S.autoscroll = !S.autoscroll;
  LS.set('pp.autoscroll', S.autoscroll);
  ev.currentTarget.classList.toggle('on', S.autoscroll);
  if (S.autoscroll) $('#logWrap').scrollTop = $('#logWrap').scrollHeight;
  toast('Auto-scroll ' + (S.autoscroll ? 'aktif' : 'nonaktif'));
});

/* config */
function mirrorKey(ev) {
  const t = ev.target;
  if (!t.matches || !t.matches('[data-key]')) return;
  $$('#view-config [data-key]').forEach((el) => {
    if (el === t || el.dataset.key !== t.dataset.key) return;
    if (t.type === 'checkbox') el.checked = t.checked; else el.value = t.value;
  });
  updateCfgDirty();
}
document.addEventListener('input', mirrorKey);
document.addEventListener('change', mirrorKey);
$('#btnCfgSave').addEventListener('click', saveConfig);
$('#btnCfgReset').addEventListener('click', () => { fillConfig(); toast('Perubahan dibatalkan'); });

/* world & seed */
$('#btnSeedSave').addEventListener('click', async () => {
  const seed = ($('#worldSeed').value || '').trim();
  if (!seed) { toast('Isi seed dulu', 'err'); return; }
  await withBusy($('#btnSeedSave'), async () => {
    try {
      const r = await api('/api/worlds/seed', { method: 'POST', body: { seed } });
      if (!r.ok) { toast(r.msg || 'Gagal', 'err'); return; }
      toast(r.msg || 'Seed disimpan');
      await refreshAll();
    } catch (e) { toast(e.message, 'err'); }
  });
});
$('#btnSeedRandom').addEventListener('click', () => {
  let s = makeSeed();
  const cur = ($('#worldSeed').value || '').trim();
  while (s === cur) s = makeSeed();
  $('#worldSeed').value = s;
  toast('Seed acak: ' + s);
});
/* players */
$('#btnAddOp').addEventListener('click', async () => {
  const name = $('#opName').value.trim();
  if (!name) { toast('Isi nama pemain', 'err'); return; }
  try {
    await api('/api/players/op', { method: 'POST', body: { name, level: Number($('#opLevel').value) } });
    $('#opName').value = '';
    toast(name + ' sekarang OP');
    refreshPlayers();
  } catch (e) { toast(e.message, 'err'); }
});
$('#btnAddWl').addEventListener('click', async () => {
  const name = $('#wlName').value.trim();
  if (!name) { toast('Isi nama pemain', 'err'); return; }
  try {
    await api('/api/players/whitelist-add', { method: 'POST', body: { name } });
    $('#wlName').value = '';
    toast(name + ' ditambahkan ke whitelist');
    refreshPlayers();
  } catch (e) { toast(e.message, 'err'); }
});
$('#btnAddBan').addEventListener('click', async () => {
  const name = $('#banName').value.trim();
  if (!name) { toast('Isi nama pemain', 'err'); return; }
  const reason = $('#banReason').value.trim();
  try {
    const r = await api('/api/players/ban', { method: 'POST', body: { name, reason } });
    $('#banName').value = ''; $('#banReason').value = '';
    toast(name + ' diban', 'err');
    if (r.msg) toast(r.msg, 'err');
    refreshPlayers();
  } catch (e) { toast(e.message, 'err'); }
});
$('#wlToggle').addEventListener('change', async (ev) => {
  const on = ev.target.checked;
  try {
    const r = await api('/api/players/whitelist-mode', { method: 'POST', body: { enabled: on } });
    toast(on ? 'Whitelist diaktifkan' : 'Whitelist dimatikan');
    if (r.msg) toast(r.msg, 'err');
    refreshPlayers();
  } catch (e) {
    ev.target.checked = !on;
    toast(e.message, 'err');
  }
});

/* jaringan panel: Localhost / IP saat ini / Custom */
function repaintHost() {
  syncHostUi();
  applyAddrs();
  if (S.tab === 'home') paintPageSub();
}
$('#setHostMode').addEventListener('change', (ev) => {
  S.hostMode = ev.target.value;
  LS.set('pp.hostMode', S.hostMode);
  repaintHost();
});
$('#setHostPick').addEventListener('change', (ev) => {
  S.ipPick = ev.target.value;
  LS.set('pp.ip', S.ipPick);
  repaintHost();
});
$('#setHostCustom').addEventListener('input', (ev) => {
  S.hostCustom = ev.target.value;
  LS.set('pp.hostCustom', S.hostCustom);
  repaintHost();
});
$('#setPort').addEventListener('input', paintNet);
$('#btnCopyToken').addEventListener('click', async () => {
  const t = (S.settings && S.settings.settings && S.settings.settings.token) || '';
  if (!t) { toast('Token belum tersedia', 'err'); return; }
  try { await navigator.clipboard.writeText(t); toast('Token disalin'); }
  catch (e) { textDlg({ title: 'Token akses panel', msg: 'Salin token ini untuk akses LAN:', value: t }); }
});
$('#btnSaveNet').addEventListener('click', async () => {
  if (!S.panelOnline) { toast('Nyalakan Panel web online dulu', 'err'); return; }
  const mode = $('#setHostMode').value;
  let host;
  if (mode === 'current') host = ($('#setHostPick').value || '').trim() || '0.0.0.0';
  else if (mode === 'custom') host = cleanHost($('#setHostCustom').value);
  else host = '127.0.0.1';
  if (!host) { toast('Isi host custom dulu', 'err'); return; }
  const port = Number($('#setPort').value) || 8087;
  if (port < 1 || port > 65535) { toast('Port harus 1-65535', 'err'); return; }
  const name = $('#setName').value.trim();
  const tok = $('#setToken').value.trim();
  const body = { host, port };
  if (tok) body.token = tok;
  if (name) body.name = name;
  await withBusy($('#btnSaveNet'), async () => {
    try {
      const r = await api('/api/settings', { method: 'PUT', body });
      if (tok) {
        S.token = tok; LS.set('pp.token', tok); $('#setToken').value = '';
        try { const u = new URL(location.href); u.searchParams.set('token', tok); history.replaceState(history.state, '', u); } catch (e) {}
      }
      S.hostMode = mode;
      LS.set('pp.hostMode', mode);
      if (mode === 'custom') { S.hostCustom = host; LS.set('pp.hostCustom', host); }
      if (r.needRelisten && scheduleRebase(host, port)) {
        /* halaman akan dimuat ulang ke alamat baru */
      } else {
        try { S.settings = await api('/api/settings'); fillSettings(); } catch (e) {}
        toast('Pengaturan panel disimpan');
      }
    } catch (e) { toast(e.message, 'err'); }
  });
});

/* panel web online: toggle di atas pengaturan */
$('#panelOnlineToggle').addEventListener('change', (ev) => { setPanelOnline(ev.target.checked); });

/* alamat server Pumpkin: tiga mode, terpisah dari alamat panel */
$('#srvBindMode').addEventListener('change', (ev) => {
  S.srvMode = ev.target.value; S.srvEditing = true; syncSrvUi();
});
$('#srvBindPick').addEventListener('change', (ev) => {
  S.srvIp = ev.target.value; LS.set('pp.srvIp', S.srvIp); S.srvEditing = true; syncSrvUi();
});
$('#srvBindCustom').addEventListener('input', (ev) => {
  S.srvCustom = ev.target.value; LS.set('pp.srvCustom', S.srvCustom); S.srvEditing = true; syncSrvUi();
});
$('#btnSaveSrv').addEventListener('click', saveServerBind);

/* bagian Umum / Eksperimental */
$('#moreSeg').addEventListener('click', (ev) => {
  const b = ev.target.closest('[data-more]');
  if (b) showMore(b.dataset.more);
});

/* tentang: tombol membuka popup, kartu tetap bisa ditekan lama untuk backup */
$('#btnAbout').addEventListener('click', () => {
  const card = document.getElementById('aboutCard');
  if (card && card.dataset.held === '1') { delete card.dataset.held; return; }
  const m = $('#aboutModal');
  if (m) m.classList.add('show');
});
$('#aboutClose').addEventListener('click', () => { const m = $('#aboutModal'); if (m) m.classList.remove('show'); });
$('#aboutModal').addEventListener('click', (ev) => { if (ev.target.id === 'aboutModal') ev.currentTarget.classList.remove('show'); });

$('#accentBtn').addEventListener('click', (ev) => {
  ev.stopPropagation();
  const p = $('#accentPick');
  if (p) p.classList.toggle('open');
});
$('#accentBtn').addEventListener('keydown', (ev) => {
  if (ev.key !== 'ArrowDown' && ev.key !== 'ArrowUp') return;
  ev.preventDefault();
  const p = $('#accentPick');
  if (p) p.classList.add('open');
  const opts = $$('#accentMenu .dd-opt');
  if (opts.length) opts[ev.key === 'ArrowDown' ? 0 : opts.length - 1].focus();
});
document.addEventListener('click', (ev) => {
  const p = $('#accentPick');
  if (p && !p.contains(ev.target)) p.classList.remove('open');
});

$('#themeToggle').addEventListener('change', (ev) => {
  S.theme = ev.target.checked ? 'dark' : 'light';
  LS.set('pp.theme', S.theme);
  applyTheme();
});

/* modal */
$('#modalOk').addEventListener('click', () => closeModal(true));
$('#modalCancel').addEventListener('click', () => closeModal(false));
$('#modal').addEventListener('click', (ev) => { if (ev.target.id === 'modal') closeModal(false); });
document.addEventListener('keydown', (ev) => {
  if (ev.key === 'Escape' && $('#modal').classList.contains('show')) closeModal(false);
  if (ev.key === 'Escape') $$('.dd.open').forEach((d) => d.classList.remove('open'));
});

