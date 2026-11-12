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
function textDlg({ title, msg = '', value = '' }) {
  $('#textTitle').textContent = title;
  $('#textMsg').textContent = msg;
  const inp = $('#textInput');
  inp.value = value;
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

