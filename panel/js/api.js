'use strict';
/* ================================ api ================================ */
let lastConnErr = '';
async function pingBase(b, tok, ms) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), ms || 4000);
  try {
    const res = await fetch(String(b).replace(/\/+$/, '') + '/api/ping', {
      headers: tok ? { 'x-token': tok } : {}, cache: 'no-store', signal: ctrl.signal
    });
    if (!res.ok) return false;
    const j = await res.json();
    return !!j.ok;
  } catch (e) { return false; } finally { clearTimeout(timer); }
}

async function api(path, opt = {}) {
  const base = String(S.base || '').replace(/\/+$/, '');
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), opt.timeout || 15000);
  const req = {
    method: opt.method || 'GET',
    headers: { ...(opt.body ? { 'content-type': 'application/json' } : {}), ...(S.token ? { 'x-token': S.token } : {}) },
    body: opt.body ? JSON.stringify(opt.body) : undefined,
    signal: ctrl.signal, cache: 'no-store'
  };
  const go = (b) => fetch(String(b).replace(/\/+$/, '') + path, req);
  try {
    let res;
    try {
      res = await go(base);
    } catch (e1) {
      /* stale saved address (e.g. 0.0.0.1) — try 127.0.0.1 once, then use it */
      if (e1.name === 'AbortError' || opt.noAlt || base === BASE_URL) throw e1;
      res = await go(BASE_URL);
      adoptBase(BASE_URL);
    }
    /* The page token (provided by the server) always wins over an old token in localStorage. */
    if (res.status === 401) {
      if (window.__PP_TOKEN && S.token !== window.__PP_TOKEN) {
        S.token = window.__PP_TOKEN;
        LS.set('pp.token', S.token);
        req.headers = { ...(opt.body ? { 'content-type': 'application/json' } : {}), 'x-token': S.token };
        res = await go(base);
      }
      /* The saved address may point to another server that rejects the token: use the page origin. */
      if (res.status === 401 && SERVED && base !== DEFAULT_BASE) {
        adoptBase(DEFAULT_BASE);
        res = await go(DEFAULT_BASE);
      }
    }
    const txt = await res.text();
    let data = {};
    try { data = txt ? JSON.parse(txt) : {}; } catch (e) { throw new Error('Respon bukan JSON (HTTP ' + res.status + ')'); }
    if (!res.ok || data.ok === false) throw new Error(data.msg || ('HTTP ' + res.status));
    lastConnErr = '';
    /* One successful request means the panel is connected, regardless of bootstrap/SSE. */
    if (S.online !== true) setOnline(true);
    return data;
  } catch (e) {
    if (e.name === 'AbortError') { lastConnErr = 'timeout'; throw new Error('Koneksi timeout'); }
    lastConnErr = e.message || 'gagal';
    throw e;
  } finally { clearTimeout(timer); }
}

/* Connection status.
   The red banner is deliberately delayed for a few seconds: when the panel just opened,
   the WebView is still loading the page + token, and SSE may reconnect briefly. Without
   that grace period, users see "Panel not connected" while the panel is actually running. */
let connBannerOn = false;
let connBannerTimer = null;
let connEverOk = false;
let connFailAt = 0;

function connTokenErr() {
  return /token/i.test(lastConnErr || '');
}

function setOnline(v) {
  if (v) {
    connEverOk = true;
    connFailAt = 0;
    if (connBannerTimer) { clearTimeout(connBannerTimer); connBannerTimer = null; }
    connBannerOn = false;
    S.online = true;
    paintConn();
    return;
  }

  if (!connFailAt) connFailAt = Date.now();
  S.online = false;
  if (connBannerTimer) { clearTimeout(connBannerTimer); connBannerTimer = null; }
  const delay = connTokenErr() ? 500 : (connEverOk ? 1500 : 3000);
  connBannerTimer = setTimeout(() => {
    connBannerTimer = null;
    connBannerOn = true;
    paintConn();
  }, delay);
  paintConn();
}

function paintConn() {
  const v = S.online;
  const en = document.documentElement.lang === 'en';
  const banner = $('#connBanner');
  if (banner) banner.hidden = !connBannerOn;
  if (connBannerOn) {
    const hint = $('#connHint');
    if (hint) {
      const hostport = String(S.base || '').replace(/^https?:\/\//, '');
      if (connTokenErr()) {
        hint.textContent = en ? 'The panel token does not match. Reopen the app or copy the token from Settings.'
                              : 'Token panel tidak cocok. Buka ulang aplikasi atau salin token dari Pengaturan.';
      } else if (lastConnErr) {
        hint.textContent = (en ? 'Address ' : 'Alamat ') + hostport + ' · ' + lastConnErr;
      } else if (!connEverOk) {
        hint.textContent = (en ? 'Waiting for the local panel at ' : 'Menunggu panel lokal di ') + hostport;
      } else {
        hint.textContent = en ? 'Connection lost. Reconnecting…' : 'Koneksi terputus. Menyambung ulang…';
      }
    }
  }
  const dot = $('#sideDot');
  if (dot) dot.className = 'dot ' + (v ? 'on' : 'off');
  const sideConn = $('#sideConn');
  if (sideConn) sideConn.textContent = v ? (en ? 'Connected' : 'Terhubung') : (en ? 'Disconnected' : 'Terputus');
  const pill = $('#statusPill');
  if (pill) {
    const running = S.status && S.status.running;
    const waiting = !v && !connBannerOn;
    pill.className = 'pill ' + (v ? (running ? 'green' : 'amber') : waiting ? 'amber' : 'red');
    pill.innerHTML = '<span class="dot ' + (v ? (running ? 'on' : 'wait') : waiting ? 'wait' : 'off') + '"></span><span>' +
      (waiting ? (en ? 'Connecting' : 'Menyambung')
               : !v ? (en ? 'Panel offline' : 'Panel offline')
               : running ? 'Online'
               : (en ? 'Stopped' : 'Terhenti')) + '</span>';
  }
  const rt = S.settings && S.settings.runtime;
  const sideInfo = $('#sideInfo');
  if (sideInfo) sideInfo.textContent = rt ? 'node ' + rt.node + ' · :' + (S.settings.settings.port || '') : 'panel v1';
}

let retryTimer = null;
let sseFails = 0;
let pollTimer = null;

function startPolling() {
  if (pollTimer) return;
  pollTimer = setInterval(async () => {
    if (S.es) return;
    try {
      const st = await api('/api/status');
      setStatus(st);
      setOnline(true);
      const r = await api('/api/logs?limit=150');
      const last = S.logs.length ? (S.logs[S.logs.length - 1].id || 0) : 0;
      (r.lines || []).forEach((l) => { if ((l.id || 0) > last) addLog(l); });
    } catch (e) { setOnline(false); }
  }, 3000);
}

function connect() {
  if (S.es) return;
  let url;
  try { url = new URL(S.base.replace(/\/+$/, '') + '/api/stream'); } catch (e) { return; }
  if (S.token) url.searchParams.set('token', S.token);
  let es;
  try { es = new EventSource(url.toString()); } catch (e) { startPolling(); return; }
  S.es = es;
  es.onopen = () => { sseFails = 0; setOnline(true); };
  es.addEventListener('hello', () => { seedLogs(); });
  es.addEventListener('status', (e) => { try { setStatus(JSON.parse(e.data)); setOnline(true); } catch (err) {} });
  es.addEventListener('log', (e) => { try { addLog(JSON.parse(e.data)); } catch (err) {} });
  es.addEventListener('backup', (e) => { try { onBackup(JSON.parse(e.data)); } catch (err) {} });
  es.onerror = () => {
    try { es.close(); } catch (e) {}
    S.es = null;
    sseFails++;
    /* SSE can drop without meaning the panel is dead (WebView pauses the stream when the
       screen is off/tab changes). Do not show the red banner immediately: check the API first. */
    api('/api/ping', { timeout: 3000, noAlt: true })
      .then(() => {
        setOnline(true);
        clearTimeout(retryTimer);
        retryTimer = setTimeout(connect, sseFails >= 3 ? 15000 : 3000);
        if (sseFails >= 3) startPolling();
      })
      .catch(() => {
        setOnline(false);
        if (sseFails >= 3) startPolling();
        else { clearTimeout(retryTimer); retryTimer = setTimeout(connect, 6000); }
      });
  };
}

async function seedLogs() {
  if (S.logs.length) return;
  try {
    const r = await api('/api/logs?limit=400');
    S.logs = r.lines || [];
    if (S.tab === 'console') renderConsole(); else updateHomeLogs();
  } catch (e) {}
}

async function bootstrap() {
  try {
    await api('/api/ping', { timeout: 6000 });
  } catch (e) {
    /* find an address that is actually alive: page origin, 127.0.0.1, localhost */
    for (const b of baseCandidates()) {
      if (normBase(S.base, '') === b) continue;
      if (await pingBase(b, S.token)) { adoptBase(b); break; }
    }
  }
  try {
    await api('/api/ping', { timeout: 6000 });
    setOnline(true);
    await refreshAll();
    connect();
  } catch (e) {
    setOnline(false);
    setTimeout(bootstrap, 6000);
  }
}

async function refreshAll() {
  const jobs = [
    api('/api/status').then(setStatus),
    api('/api/players').then((p) => { S.players = p; renderPlayers(); }),
    api('/api/backups').then((b) => { S.backups = b.backups || []; renderBackups(); }),
    api('/api/config').then((c) => {
      S.cfgToml = c.toml || ''; S.cfgParsed = parseToml(S.cfgToml); S.cfgOriginal = JSON.parse(JSON.stringify(S.cfgParsed));
      fillConfig();
      loadServerBind();
    }).catch(() => {}),
    api('/api/settings').then((s) => { S.settings = s; fillSettings(); }).catch(() => {}),
    api('/api/worlds').then((w) => { S.worlds = w; renderWorlds(); renderHomeInfo(); }).catch(() => {})
  ];
  await Promise.allSettled(jobs);
}

