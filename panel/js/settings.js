'use strict';
/* ============================== settings ============================== */
const PATH_LABELS = { serverDir: 'Folder server (host)', guestCmd: 'Perintah start', launcher: 'Launcher' };
function fillSettings() {
  if (!S.settings) return;
  const st = S.settings.settings || {};
  const paths = S.settings.paths || {};
  S.paths = {
    serverDir: st.serverDir || '', guestCmd: st.guestCmd || '',
    launcher: st.launcher || ''
  };
  Object.keys(S.paths).forEach((k) => {
    const el = $('#path' + k.charAt(0).toUpperCase() + k.slice(1));
    if (el) { el.textContent = S.paths[k] || '\u2014'; el.title = S.paths[k]; }
  });
  const nm = $('#setName');
  if (nm && nm !== document.activeElement) nm.value = st.name || '';
  const ps = $('#setPort');
  if (ps) ps.value = st.port || 8087;
  const h = st.host || '127.0.0.1';
  if (h === '0.0.0.0') {
    if (S.ipFamily !== 'ipv4') { S.ipFamily = 'ipv4'; LS.set('pp.ipFamily', 'ipv4'); }
  } else if (h === '::') {
    if (S.ipFamily !== 'ipv6' && S.ipFamily !== 'both') { S.ipFamily = 'both'; LS.set('pp.ipFamily', 'both'); }
  } else if (h !== '127.0.0.1' && h !== 'localhost' && h !== '::1') {
    if (!S.hostCustom) { S.hostCustom = h; LS.set('pp.hostCustom', h); }
  }
  renderHostPick(S.status);
  S.panelOnline = st.panelOnline === true;
  syncPanelOnline();
  $('#brandName').textContent = st.name || 'Pumpkin Panel';
  document.title = (st.name || 'Pumpkin') + ' \u00b7 Panel';
  if (window.ppRefreshSelects) ppRefreshSelects();
  paintConn();
}
async function editPath(key) {
  const cur = S.paths[key] || '';
  /* serverDir = folder, launcher = file, guestCmd = command (no picker). */
  const mode = key === 'launcher' ? 'file' : (key === 'guestCmd' ? false : 'dir');
  const v = await textDlg({
    title: PATH_LABELS[key] || key,
    msg: mode ? 'Ketik manual, atau pilih lewat tombol Dir.' : 'Ubah nilai lalu simpan.',
    value: cur,
    dir: mode
  });
  if (v === null || v === cur) return;
  try {
    await api('/api/settings', { method: 'PUT', body: { ...S.paths, [key]: v } });
    toast('Path disimpan');
    await refreshAll();
  } catch (e) { toast(e.message, 'err'); }
}

/* ============================== appearance ============================== */
/* Green gradient: lime → green → teal. Default = middle green (#22c55e). */
const ACCENTS = ['#a3e635', '#4ade80', '#22c55e', '#10b981', '#14b8a6', '#06b6d4'];
const ACCENT_ID = ['Hijau limau', 'Hijau muda', 'Hijau', 'Hijau teal', 'Teal', 'Sian'];
const ACCENT_EN = ['Lime green', 'Light green', 'Green', 'Emerald', 'Teal', 'Cyan'];
function hexA(hex, a) {
  const h = hex.replace('#', '');
  const n = parseInt(h.length === 3 ? h.split('').map((c) => c + c).join('') : h, 16);
  return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + a + ')';
}
/* Blend toward white (amt > 0) or black (amt < 0). */
function shade(hex, amt) {
  const h = hex.replace('#', '');
  const n = parseInt(h.length === 3 ? h.split('').map((c) => c + c).join('') : h, 16);
  const t = amt >= 0 ? 255 : 0;
  const k = Math.abs(amt);
  const ch = (v) => Math.round(v + (t - v) * k);
  const r = ch((n >> 16) & 255), g = ch((n >> 8) & 255), b = ch(n & 255);
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('');
}
function accentName(c) {
  const i = ACCENTS.indexOf(c);
  if (i < 0) return c;
  return (document.documentElement.lang === 'en' ? ACCENT_EN : ACCENT_ID)[i];
}
function renderAccentPicker() {
  const menu = $('#accentMenu');
  const dot = $('#accentDot');
  if (dot) dot.style.background = S.accent;
  const nm = $('#accentName');
  if (nm) nm.textContent = accentName(S.accent);
  if (!menu) return;
  menu.innerHTML = ACCENTS.map((c) =>
    '<button type="button" class="dd-opt' + (c === S.accent ? ' active' : '') + '" role="option" aria-selected="' + (c === S.accent) + '" data-accent="' + c + '">' +
    '<span class="accent-dot" style="background:' + c + '"></span><span style="margin-left:9px">' + esc(accentName(c)) + '</span></button>'
  ).join('');
}
window.ppOnLang = function () {
  renderAccentPicker();
  /* Programmatic text (status pill, counters, row subtitles) is language-specific.
     Redraw it after a language switch so going back to Indonesian restores the
     original source text instead of keeping the English snapshot. */
  try { if (S.status) setStatus(S.status); } catch (e) {}
  try { renderPlayers(); } catch (e) {}
  try { if (S.worlds) renderWorlds(); } catch (e) {}
  try { renderBackups(); } catch (e) {}
  try { renderHomeInfo(); } catch (e) {}
  try { paintNet(); } catch (e) {}
};
function applyTheme() {
  document.documentElement.dataset.theme = S.theme;
  const me = document.documentElement.style;
  /* Every "Minecraft green" surface follows the selected accent color. */
  me.setProperty('--accent', S.accent);
  me.setProperty('--accent-soft', hexA(S.accent, 0.14));
  me.setProperty('--accent-line', hexA(S.accent, 0.38));
  me.setProperty('--mc-grass', S.accent);
  me.setProperty('--mc-grass-l', shade(S.accent, 0.34));
  me.setProperty('--mc-grass-d', shade(S.accent, -0.34));
  me.setProperty('--mc-grass-x', shade(S.accent, -0.62));
  me.setProperty('--green', S.accent);
  me.setProperty('--green-soft', hexA(S.accent, 0.13));
  me.setProperty('--green-line', hexA(S.accent, 0.34));
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.content = S.theme === 'dark' ? '#101010' : '#dfe3d8';
  const t = $('#themeToggle');
  if (t) t.checked = S.theme === 'dark';
  const tt = $('#themeTxt');
  if (tt) tt.textContent = S.theme === 'dark' ? 'Gelap' : 'Terang';
  renderAccentPicker();
}

