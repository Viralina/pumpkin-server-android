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
