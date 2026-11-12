'use strict';
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch (e) { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) {} }
};
const BASE_URL = 'http://127.0.0.1:8087';
const SERVED = /^https?:$/.test(location.protocol);

/* The saved address may be invalid (e.g. 0.0.0.0 / 0.0.0.1) — always sanitize it. */
function normBase(v, fb) {
  let s = String(v == null ? '' : v).trim();
  if (!s) return fb;
  if (!/^[a-z]+:\/\//i.test(s)) s = 'http://' + s;
  try {
    const u = new URL(s);
    if (!/^https?:$/.test(u.protocol)) return fb;
    const h = String(u.hostname || '').replace(/^\[|\]$/g, '').toLowerCase();
    if (!h || h === '0.0.0.0' || h === '0.0.0.1' || h === '::' || h === '::1') u.hostname = '127.0.0.1';
    return u.origin.replace(/\/+$/, '');
  } catch (e) { return fb; }
}
const DEFAULT_BASE = SERVED ? normBase(location.origin, BASE_URL) : BASE_URL;
const STORED_BASE = normBase(LS.get('pp.base', ''), '');

const S = {
  /* If this page is served by the panel server, the API address is always the page origin.
     A saved address can be stale (old LAN IP / old port) and show the "not connected"
     banner even though the panel was opened from the same server. */
  base: SERVED ? DEFAULT_BASE : (STORED_BASE || DEFAULT_BASE),
  token: LS.get('pp.token', ''),
  tab: 'home',
  status: null, logs: [], cfgToml: '', cfgParsed: {}, cfgOriginal: {}, players: null,
  backups: [], settings: null, online: false, es: null,
  level: 'ALL', q: '', autoscroll: LS.get('pp.autoscroll', true), ipPick: LS.get('pp.ip', ''), hostMode: LS.get('pp.hostMode', ''), hostCustom: LS.get('pp.hostCustom', ''), paths: {},
  panelOnline: false, serverBind: null, srvMode: '', srvIp: LS.get('pp.srvIp', ''), srvCustom: LS.get('pp.srvCustom', ''), srvEditing: false,
  theme: LS.get('pp.theme', 'dark'),
  accent: (function () {
    const a = String(LS.get('pp.accent', '') || '').trim().toLowerCase();
    if (a === '#ff8a3d' || a === '#ffaa00') return '#22c55e';
    return /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/.test(a) ? a : '#22c55e';
  })(),
  cmdHist: LS.get('pp.cmdhist', []), histIdx: -1,
  busy: false, logSeen: 0
};

/* Token from the server (embedded in the page) or ?token= in the URL. */
(function () {
  let q = '';
  try { q = new URLSearchParams(location.search).get('token') || ''; } catch (e) {}
  if (window.__PP_TOKEN) q = window.__PP_TOKEN;
  if (q) {
    S.token = q;
    LS.set('pp.token', q);
    try {
      if (!new URL(location.href).searchParams.get('token')) {
        const u = new URL(location.href); u.searchParams.set('token', q);
        history.replaceState(history.state, '', u);
      }
    } catch (e) {}
  }
})();

/* Called by the APK after the page loads: keep the token in sync. */
window.ppSetToken = function (t) {
  if (!t || t === S.token) return;
  S.token = t;
  LS.set('pp.token', t);
  try {
    const u = new URL(location.href); u.searchParams.set('token', t);
    history.replaceState(history.state, '', u);
  } catch (e) {}
  bootstrap();
};

/* safely switch the API address (used when the saved address is dead) */
function adoptBase(b) {
  const v = normBase(b, '');
  if (!v) return false;
  S.base = v;
  LS.set('pp.base', v);
  lastConnErr = '';
  return true;
}
function baseCandidates() {
  const list = [];
  const add = (v) => { const b = normBase(v, ''); if (b && !list.includes(b)) list.push(b); };
  add(S.base);
  if (SERVED) add(location.origin);
  add(BASE_URL);
  add('http://localhost:8087');
  return list;
}

