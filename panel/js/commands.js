'use strict';
/* ========================= commands & power ========================= */
function pushHist(cmd) {
  if (!cmd) return;
  S.cmdHist = S.cmdHist.filter((c) => c !== cmd);
  S.cmdHist.push(cmd);
  if (S.cmdHist.length > 60) S.cmdHist.shift();
  LS.set('pp.cmdhist', S.cmdHist);
  S.histIdx = -1;
}
async function sendCmd(cmd, quiet) {
  cmd = String(cmd || '').trim();
  if (!cmd) return;
  if (!S.status || !S.status.running) { toast('Server belum berjalan', 'err'); return; }
  if (!S.status.managed) { toast('Server dijalankan di luar panel — perintah tidak bisa dikirim', 'err'); return; }
  try {
    await api('/api/command', { method: 'POST', body: { cmd } });
    pushHist(cmd);
  } catch (e) { toast(e.message || 'Gagal mengirim perintah', 'err'); }
}
async function doPower(ev) {
  const powerBtn = (ev && ev.currentTarget) || $('#btnPower');
  const running = S.status && S.status.running;
  if (running) {
    const ok = await confirmDlg({
      title: 'Hentikan server?',
      msg: 'Pemain akan terputus. World akan disimpan otomatis sebelum keluar.',
      ok: 'Hentikan', danger: true
    });
    if (!ok) return;
  }
  await withBusy(powerBtn, async () => {
    try {
      await api('/api/' + (running ? 'stop' : 'start'), { method: 'POST' });
      toast(running ? 'Menghentikan server…' : 'Menjalankan server…');
      const poll = () => api('/api/status').then((st) => { setStatus(st); setOnline(true); }).catch(() => {});
      setTimeout(poll, 800);
      setTimeout(poll, 2500);
      setTimeout(poll, 6000);
      if (!running) setTimeout(() => toast('Server mulai · pantau di Console kapan saja'), 3200);
    } catch (e) {
      if (/sudah berjalan/i.test(e.message)) {
        toast('Server memang sudah berjalan');
        api('/api/status').then((st) => { setStatus(st); setOnline(true); }).catch(() => {});
      } else if (!S.online || /failed to fetch|networkerror|load failed|koneksi timeout/i.test(e.message)) {
        toast('Mencari panel di 127.0.0.1…', 'err');
        if (S.es) { try { S.es.close(); } catch (e2) {} S.es = null; }
        adoptBase(BASE_URL);
        bootstrap();
      } else {
        toast(e.message, 'err');
      }
    }
  });
}
async function doRestart() {
  const ok = await confirmDlg({ title: 'Restart server?', msg: 'Server akan berhenti, menyimpan world, lalu mulai lagi.', ok: 'Restart' });
  if (!ok) return;
  await withBusy($('#btnRestart'), async () => {
    try {
      await api('/api/restart', { method: 'POST' });
      toast('Restart dijadwalkan');
    } catch (e) { toast(e.message, 'err'); }
  });
}
async function doSaveWorld() {
  if (!S.status || !S.status.running) { toast('Server belum berjalan', 'err'); return; }
  if (!S.status.managed) { toast('Server berjalan di luar panel', 'err'); return; }
  await sendCmd('save-all');
  toast('Menyimpan world…');
}
async function doBackup() {
  const mb = S.status ? S.status.worldMB : 0;
  const ok = await confirmDlg({
    title: 'Buat backup world?',
    msg: 'Ukuran world <b>~' + mb + ' MB</b>. Proses kompresi bisa memakan waktu dan CPU. Lanjutkan?',
    ok: 'Buat backup'
  });
  if (!ok) return;
  try { await api('/api/backup', { method: 'POST' }); toast('Backup dimulai'); }
  catch (e) { toast(e.message, 'err'); }
}
function onBackup(ev) {
  const run = $('#backupRun');
  if (run) run.hidden = !ev.running;
  if (!ev.running) {
    toast(ev.ok ? 'Backup selesai: ' + ev.name : 'Backup gagal', ev.ok ? 'ok' : 'err');
    api('/api/backups').then((b) => { S.backups = b.backups || []; renderBackups(); }).catch(() => {});
  }
}

/* =============================== tabs =============================== */
const TITLES = {
  home: ['Ringkasan', 'Status & kontrol server'],
  console: ['Console', 'Log langsung dan perintah'],
  config: ['Konfigurasi', 'pumpkin.toml'],
  players: ['Pemain', 'Online, OP, dan whitelist'],
  more: ['Pengaturan', 'Umum, eksperimental, dan path']
};
function fitConsole() {
  const on = document.body.classList.contains('on-console') && window.innerWidth <= 980;
  if (!on) {
    document.documentElement.style.removeProperty('--console-top');
    document.documentElement.style.removeProperty('--console-bottom');
    return;
  }
  const bar = document.querySelector('.topbar');
  const nav = document.querySelector('.tabbar');
  const top = bar ? bar.getBoundingClientRect().bottom : 58;
  const bottom = nav ? Math.max(0, window.innerHeight - nav.getBoundingClientRect().top) : 0;
  document.documentElement.style.setProperty('--console-top', Math.round(top) + 'px');
  document.documentElement.style.setProperty('--console-bottom', Math.round(bottom) + 'px');
}
window.addEventListener('resize', fitConsole);
window.addEventListener('orientationchange', () => setTimeout(fitConsole, 160));
if (window.visualViewport) window.visualViewport.addEventListener('resize', fitConsole);

/* Bagian Umum / Eksperimental — selalu kembali ke Umum saat menu dibuka. */
function showMore(name) {
  const exp = name === 'exp';
  $$('.dd.open').forEach((d) => d.classList.remove('open'));
  const u = $('#moreUmum'), e = $('#moreExp');
  if (u) u.hidden = exp;
  if (e) e.hidden = !exp;
  $$('#moreSeg button').forEach((b) => {
    const on = b.dataset.more === name;
    b.classList.toggle('active', on);
    b.setAttribute('aria-selected', on ? 'true' : 'false');
  });
  if (exp) loadServerBind();
}
function setTab(t) {
  S.tab = t;
  $$('.view').forEach((v) => v.classList.toggle('active', v.id === 'view-' + t));
  $$('.nav-item[data-tab], .tab[data-tab]').forEach((el) => el.classList.toggle('active', el.dataset.tab === t));
  document.body.classList.toggle('on-console', t === 'console');
  fitConsole();
  if (t === 'console') requestAnimationFrame(fitConsole);
  const tt = TITLES[t] || ['', ''];
  $('#pageTitle').textContent = tt[0];
  if (t !== 'home') $('#pageSub').textContent = tt[1];
  else if (S.status) setStatus(S.status);
  else $('#pageSub').textContent = tt[1];
  if (t === 'console') { S.logSeen = 0; updateNavDot(); renderConsole(); setTimeout(() => { $('#logWrap').scrollTop = $('#logWrap').scrollHeight; }, 30); }
  if (t === 'players') { renderPlayers(); api('/api/players').then((p) => { S.players = p; renderPlayers(); }).catch(() => {}); }
  if (t === 'config') { $('#cfgSavedBanner').hidden = !S.cfgDirty; }
  if (t === 'more') { showMore('umum'); renderBackups(); }
  window.scrollTo(0, 0);
}
