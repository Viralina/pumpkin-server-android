'use strict';
/* ================================ init ================================ */
/* native APK menu: topbar button + card in Settings */
function hasAppMenu() {
  try { return !!(window.PanelNative && typeof PanelNative.openMenu === 'function'); }
  catch (e) { return false; }
}
function openAppMenu() {
  try {
    if (hasAppMenu()) { PanelNative.openMenu(); return; }
  } catch (e) {}
  toast('Menu aplikasi hanya tersedia di APK', 'err');
}
const nativeMenu = hasAppMenu();
const appMenuBtn = $('#btnAppMenu');
if (appMenuBtn) {
  appMenuBtn.style.display = nativeMenu ? '' : 'none';
  appMenuBtn.addEventListener('click', openAppMenu);
}
const appCard = $('#appCard');
if (appCard) appCard.style.display = nativeMenu ? '' : 'none';
if ($('#btnAppMenu2')) $('#btnAppMenu2').addEventListener('click', openAppMenu);
if ($('#btnSwapBin')) $('#btnSwapBin').addEventListener('click', openAppMenu);

applyTheme();
$('#btnAutoScroll').classList.toggle('on', S.autoscroll);
paintConn();
setTab('home');
renderPlayers();
renderBackups();

/* Backup = secret menu: hidden until the "About & notes" card is long-pressed. */
function applySecretBackup() {
  const on = LS.get('pp.menu.backup', '') === '1';
  ['btnBackup', 'backupCard'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.hidden = !on;
  });
  const q = document.querySelector('#view-home .quick');
  if (q) q.classList.toggle('two', !on);
}
applySecretBackup();
(function () {
  const card = document.getElementById('aboutCard');
  if (!card) return;
  let timer = null, sx = 0, sy = 0;
  function cancel() { if (timer) { clearTimeout(timer); timer = null; } }
  card.addEventListener('pointerdown', (e) => {
    sx = e.clientX; sy = e.clientY;
    /* New gesture: always reset the previous long-press flag. */
    delete card.dataset.held;
    cancel();
    timer = setTimeout(() => {
      timer = null;
      /* The flag is set in both branches so a click after a long press does not open the popup. */
      card.dataset.held = '1';
      if (LS.get('pp.menu.backup', '') === '1') return;
      LS.set('pp.menu.backup', '1');
      applySecretBackup();
      toast('Menu backup dibuka');
    }, 900);
  });
  card.addEventListener('pointermove', (e) => {
    if (timer && (Math.abs(e.clientX - sx) > 10 || Math.abs(e.clientY - sy) > 10)) cancel();
  });
  ['pointerup', 'pointercancel', 'pointerleave'].forEach((ev) => card.addEventListener(ev, cancel));
})();

bootstrap();
startNet();

/* Connection watchdog: if status gets stuck "offline" (e.g. the SSE stream goes quiet after
   the screen turns off), one lightweight request restores the banner without waiting for the user. */
setInterval(() => {
  if (document.hidden || S.online) return;
  api('/api/ping', { timeout: 3000 }).catch(() => {});
}, 4000);

document.addEventListener('visibilitychange', () => {
  if (document.hidden) return;
  api('/api/ping', { timeout: 3000 })
    .then(() => { if (!S.es) connect(); })
    .catch(() => {});
});

setInterval(() => {
  if (S.status && S.status.running) {
    const u = fmtDur(uptimeSec());
    if ($('#hUptime')) $('#hUptime').textContent = u;
  }
}, 1000);
