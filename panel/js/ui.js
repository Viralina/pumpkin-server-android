'use strict';
/* ============================== toast/ui ============================== */
function toast(msg, type = 'ok') {
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.innerHTML = '<svg class="ic" style="width:17px;height:17px"><use href="#' + (type === 'err' ? 'i-alert' : 'i-check') + '"/></svg><span>' + esc(msg) + '</span>';
  $('#toasts').appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0'; el.style.transform = 'translateY(6px)'; el.style.transition = '.25s';
    setTimeout(() => el.remove(), 260);
  }, 2800);
}

let modalResolve = null;
function confirmDlg({ title, msg, ok = 'Lanjutkan', danger = false }) {
  $('#modalTitle').textContent = title;
  $('#modalMsg').innerHTML = msg;
  const btn = $('#modalOk');
  btn.textContent = ok;
  btn.className = 'btn ' + (danger ? 'btn-danger' : 'btn-primary');
  $('#modal').classList.add('show');
  return new Promise((res) => { modalResolve = res; });
}
function closeModal(v) {
  $('#modal').classList.remove('show');
  if (modalResolve) { const r = modalResolve; modalResolve = null; r(v); }
}

/* Info sheet with one button (plugin "coming soon", etc.). */
function infoDlg({ title, msg, ok = 'Mengerti' }) {
  $('#modalTitle').textContent = title;
  $('#modalMsg').innerHTML = msg;
  const b = $('#modalOk');
  b.textContent = ok;
  b.className = 'btn btn-primary';
  const c = $('#modalCancel');
  const wasHidden = c.hidden;
  c.hidden = true;
  const acts = $('#modal .modal-actions');
  if (acts) acts.classList.add('one');
  $('#modal').classList.add('show');
  return new Promise((res) => {
    modalResolve = () => {
      c.hidden = wasHidden;
      if (acts) acts.classList.remove('one');
      res(true);
    };
  });
}

/* choice dialog (used for gamemode and similar) */
let choiceResolve = null;
function choiceDlg({ title, msg = '', options = [] }) {
  $('#choiceTitle').textContent = title;
  $('#choiceMsg').innerHTML = msg;
  $('#choiceBtns').innerHTML = options.map((o, i) =>
    '<button class="btn ' + (o.danger ? 'btn-danger' : o.primary ? 'btn-primary' : 'btn-soft') +
    '" data-choice="' + i + '">' + esc(o.label) + '</button>'
  ).join('');
  $('#choiceModal').classList.add('show');
  return new Promise((res) => {
    choiceResolve = (idx) => res(idx == null ? null : options[idx]);
  });
}
function closeChoice(idx) {
  $('#choiceModal').classList.remove('show');
  if (choiceResolve) { const r = choiceResolve; choiceResolve = null; r(idx); }
}
$('#choiceCancel').addEventListener('click', () => closeChoice(null));
$('#choiceBtns').addEventListener('click', (ev) => {
  const b = ev.target.closest('[data-choice]');
  if (b) closeChoice(Number(b.dataset.choice));
});
$('#choiceModal').addEventListener('click', (ev) => {
  if (ev.target === $('#choiceModal')) closeChoice(null);
});

let textResolve = null;
function textDlg({ title, msg = '', value = '', dir = false }) {
  $('#textTitle').textContent = title;
  $('#textMsg').textContent = msg;
  const inp = $('#textInput');
  inp.value = value;
  const db = $('#textDir');
  if (db) {
    db.hidden = !dir;
    db.dataset.mode = dir === 'file' ? 'file' : 'dir';
  }
  const act = $('#textModal .modal-actions');
  if (act) act.classList.toggle('three', !!dir);
  $('#textModal').classList.add('show');
  setTimeout(() => { try { inp.focus(); inp.select(); } catch (e) {} }, 60);
  return new Promise((res) => { textResolve = res; });
}
function closeText(v) {
  $('#textModal').classList.remove('show');
  if (textResolve) { const r = textResolve; textResolve = null; r(v); }
}
$('#textOk').addEventListener('click', () => closeText($('#textInput').value.trim()));
$('#textCancel').addEventListener('click', () => closeText(null));
$('#textModal').addEventListener('click', (ev) => { if (ev.target === $('#textModal')) closeText(null); });

/* Path editor: "Direktori" opens the folder picker and writes the result back. */
$('#textDir').addEventListener('click', async () => {
  const cur = ($('#textInput').value || '').trim();
  const mode = $('#textDir').dataset.mode === 'file' ? 'file' : 'dir';
  const start = cur || (S.paths && S.paths.serverDir) || '/sdcard';
  const p = await dirDlg(start, mode);
  if (p) $('#textInput').value = p;
});

/* ============================== folder picker ============================== */
let dirResolve = null;
let dirMode = 'dir';
let dirPath = '';
let dirParent = '';

function dirClose(v) {
  const m = $('#dirModal');
  if (m) m.classList.remove('show');
  if (dirResolve) { const r = dirResolve; dirResolve = null; r(v); }
}

function dirRow(path, name, sub, icon, isFile) {
  return '<button type="button" class="list-item known-row" ' + (isFile ? 'data-dir-file' : 'data-dir') +
    '="' + esc(path) + '"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#' + icon + '"/></svg></div>' +
    '<div class="li-main"><div class="li-name">' + esc(name) + '</div><div class="li-sub">' + esc(sub) + '</div></div></button>';
}

async function dirLoad(p) {
  const want = p || '/sdcard';
  /* Show the requested path immediately: never leave the previous folder's
     listing on screen while another path is being loaded. */
  dirPath = want;
  dirParent = want.replace(/\/[^/]+\/?$/, '') || '';
  $('#dirPath').textContent = want;
  $('#dirList').innerHTML = '<div class="empty">Memuat…</div>';
  let r;
  try {
    r = await api('/api/fs?path=' + encodeURIComponent(want) + '&files=' + (dirMode === 'file' ? '1' : '0'));
  } catch (e) {
    $('#dirList').innerHTML = '<div class="empty">' + esc(e.message || 'Folder tidak bisa dibaca') + '</div>';
    return;
  }
  if (!r || r.ok === false) {
    $('#dirList').innerHTML = '<div class="empty">' + esc((r && r.msg) || ('Folder tidak bisa dibaca: ' + want)) + '</div>';
    return;
  }
  dirPath = r.path || want;
  dirParent = r.parent || '';
  $('#dirPath').textContent = dirPath;
  const dirs = r.dirs || [], files = r.files || [];
  const rows = [];
  if (dirParent) rows.push(dirRow(dirParent, '..', 'folder di atas', 'i-up', false));
  dirs.forEach((d) => rows.push(dirRow(d.path, d.name, 'folder', 'i-folder', false)));
  files.forEach((f) => rows.push(dirRow(f.path, f.name, 'file · ketuk untuk memakai', 'i-file', true)));
  $('#dirList').innerHTML = rows.length ? rows.join('') : '<div class="empty">Folder ini kosong</div>';
  $('#dirPick').textContent = dirMode === 'file' ? 'Pakai folder ini' : 'Pilih folder ini';
}

function dirDlg(start, mode) {
  dirMode = mode === 'file' ? 'file' : 'dir';
  $('#dirTitle').textContent = dirMode === 'file' ? 'Pilih file' : 'Pilih folder';
  dirPath = '';
  dirParent = '';
  $('#dirPath').textContent = '';
  $('#dirList').innerHTML = '<div class="empty">Memuat…</div>';
  $('#dirModal').classList.add('show');
  const p = new Promise((res) => { dirResolve = res; });
  dirLoad(start || '/sdcard');
  return p;
}

$('#dirUp').addEventListener('click', () => { if (dirParent) dirLoad(dirParent); });
$('#dirRoot').addEventListener('click', () => dirLoad('/sdcard'));
$('#dirPick').addEventListener('click', () => dirClose(dirPath || null));
$('#dirCancel').addEventListener('click', () => dirClose(null));
$('#dirModal').addEventListener('click', (ev) => { if (ev.target.id === 'dirModal') dirClose(null); });
$('#dirList').addEventListener('click', (ev) => {
  const f = ev.target.closest('[data-dir-file]');
  if (f) { dirClose(f.dataset.dirFile); return; }
  const d = ev.target.closest('[data-dir]');
  if (d) dirLoad(d.dataset.dir);
});
$('#textInput').addEventListener('keydown', (ev) => {
  if (ev.key === 'Enter') { ev.preventDefault(); closeText($('#textInput').value.trim()); }
  if (ev.key === 'Escape') closeText(null);
});

async function withBusy(btn, fn) {
  if (!btn || btn.disabled) return;
  const html = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = '<svg class="ic spin" style="width:16px;height:16px"><use href="#i-restart"/></svg>';
  try { await fn(); }
  finally {
    btn.disabled = false;
    /* if server status already changed the button label, do not overwrite it with the old one */
    if (btn.querySelector('.spin')) btn.innerHTML = html;
  }
}

async function copyText(t) {
  try { await navigator.clipboard.writeText(t); }
  catch (e) {
    const ta = document.createElement('textarea');
    ta.value = t; ta.style.position = 'fixed'; ta.style.opacity = '0';
    document.body.appendChild(ta); ta.select();
    try { document.execCommand('copy'); } catch (err) {}
    ta.remove();
  }
  toast('Disalin: ' + t);
}

/* ============================== formats ============================== */
const fmtNum = (n) => (n === null || n === undefined || isNaN(n)) ? '—' : Number(n).toLocaleString('id-ID');
function fmtDur(sec) {
  sec = Math.max(0, Math.round(sec || 0));
  if (!sec) return '—';
  const d = Math.floor(sec / 86400), h = Math.floor((sec % 86400) / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
  if (d) return d + 'h ' + h + 'j';
  if (h) return h + 'j ' + m + 'm';
  if (m) return m + 'm ' + s + 's';
  return s + 's';
}
function fmtAgo(ms) {
  const s = Math.round((Date.now() - ms) / 1000);
  if (s < 60) return 'baru saja';
  if (s < 3600) return Math.floor(s / 60) + ' menit lalu';
  if (s < 86400) return Math.floor(s / 3600) + ' jam lalu';
  return Math.floor(s / 86400) + ' hari lalu';
}
function uptimeSec() {
  const st = S.status;
  if (!st || !st.running) return 0;
  if (st.startedAt) return Math.max(0, Math.round((Date.now() - st.startedAt) / 1000));
  return st.uptime || 0;
}

