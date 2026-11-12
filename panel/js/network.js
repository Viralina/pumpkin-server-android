'use strict';
/* ---------- web panel online (experimental) ---------- */
function syncPanelOnline() {
  const on = !!S.panelOnline;
  const lock = $('#netLock');
  if (lock) {
    lock.dataset.ro = on ? '0' : '1';
    lock.querySelectorAll('input,select,button,textarea').forEach((el) => { el.disabled = !on; });
  }
  const tg = $('#panelOnlineToggle');
  if (tg) tg.checked = on;
  const st = $('#panelOnlineState');
  if (st) {
    const raw = on ? 'Nyala' : 'Mati';
    st.__ppSrc = raw;
    st.textContent = (window.ppT && window.ppT(raw)) || raw;
    if (st.firstChild) st.firstChild.__ppSrc = raw;
  }
  paintNet();
}
async function setPanelOnline(on) {
  const prev = S.panelOnline;
  S.panelOnline = on;
  syncPanelOnline();
  if (!on) {
    try {
      await api('/api/settings', { method: 'PUT', body: { panelOnline: false } });
      const port = (S.settings && S.settings.settings && S.settings.settings.port) || 8087;
      if (S.base && !/^http:\/\/(127\.0\.0\.1|localhost|\[::1\]):/.test(S.base)) {
        scheduleRebase('127.0.0.1', port);
      } else {
        toast('Panel web dimatikan');
      }
    }
    catch (e) { S.panelOnline = prev; syncPanelOnline(); toast(e.message, 'err'); }
    return;
  }
  let mode = hostMode();
  let host = '';
  if (mode === 'custom') {
    host = cleanHost(S.hostCustom) || cleanHost($('#setHostCustom').value) ||
      cleanHost(S.settings && S.settings.settings && S.settings.settings.host);
  } else if (mode === 'current') {
    host = ($('#setHostPick').value || '').trim() || (pickedIp(S.status) || {}).address || '';
  } else {
    host = '127.0.0.1';
  }
  const altHost = (pickedIp(S.status) || {}).address || '127.0.0.1';
  /* Never lock the toggle: empty/stale custom host → use the current IP. */
  if (!host) {
    host = altHost; mode = 'current';
    toast('Host custom kosong — memakai IP saat ini');
  } else if (host === '0.0.0.0' || host === '::') {
    host = altHost; mode = 'current';
  } else if (mode === 'custom' && !currentIps(S.status).some((a) => a.address === host)) {
    host = altHost; mode = 'current';
    toast('Host custom tidak aktif — memakai IP saat ini');
  }
  if (host === '127.0.0.1' || host === 'localhost' || host === '::1') mode = 'localhost';
  const port = Number($('#setPort').value) || (S.settings && S.settings.settings && S.settings.settings.port) || 8087;
  try {
    await api('/api/settings', { method: 'PUT', body: { panelOnline: true, host, port } });
    S.hostMode = mode; LS.set('pp.hostMode', mode);
    if (mode === 'custom') { S.hostCustom = host; LS.set('pp.hostCustom', host); }
    try { S.settings = await api('/api/settings'); } catch (e) {}
    toast('Panel web online di ' + fmtAddr(host, port));
    if (!scheduleRebase(host, port)) {
      try { S.settings = await api('/api/settings'); fillSettings(); } catch (e) {}
    }
  } catch (e) {
    /* The host may be stale: fetch fresh status, then retry once with the current IP. */
    try { const fresh = await api('/api/status'); if (fresh) S.status = fresh; } catch (e2) {}
    const alt = (pickedIp(S.status) || {}).address || '';
    if (mode === 'custom' && alt && alt !== host && alt !== '127.0.0.1') {
      try {
        await api('/api/settings', { method: 'PUT', body: { panelOnline: true, host: alt, port } });
        S.hostMode = 'current'; LS.set('pp.hostMode', 'current');
        try { S.settings = await api('/api/settings'); } catch (e2) {}
        toast('Host custom tidak aktif — memakai IP saat ini');
        if (!scheduleRebase(alt, port)) {
          try { S.settings = await api('/api/settings'); fillSettings(); } catch (e2) {}
        }
        return;
      } catch (e2) {}
    }
    S.panelOnline = prev; syncPanelOnline(); toast(e.message, 'err');
  }
}
function scheduleRebase(host, port) {
  const hn = (host === '0.0.0.0' || host === '::') ? ((location.hostname && location.hostname !== '0.0.0.0') ? location.hostname : '127.0.0.1') : host;
  const next = 'http://' + fmtAddr(hn, port);
  if (next === String(S.base || '').replace(/\/+$/, '')) return false;
  S.base = next;
  LS.set('pp.base', S.base);
  const b = $('#setBase'); if (b) b.value = S.base;
  toast('Panel pindah ke ' + S.base + ' — tunggu 2 detik');
  setTimeout(() => { if (S.es) { try { S.es.close(); } catch (e) {} S.es = null; } setOnline(false); bootstrap(); }, 2200);
  return true;
}
function syncHostUi() {
  const mode = hostMode();
  const m = $('#setHostMode');
  if (m && m.value !== mode) m.value = mode;
  const cw = $('#hostCurrentWrap');
  if (cw) cw.hidden = mode !== 'current';
  const uw = $('#hostCustomWrap');
  if (uw) uw.hidden = mode !== 'custom';
  const ci = $('#setHostCustom');
  if (ci && ci !== document.activeElement && ci.value !== S.hostCustom) ci.value = S.hostCustom;
  paintNet();
}
function renderHostPick(st) {
  const sel = $('#setHostPick');
  if (sel) {
    const list = currentIps(st);
    const key = list.map((a) => a.address + '|' + a.name).join(',');
    if (sel.dataset.key !== key) {
      sel.dataset.key = key;
      sel.innerHTML = list.map((a) => '<option value="' + esc(a.address) + '">' + esc(ipLabel(a) + ' \u00b7 ' + a.address) + '</option>').join('');
    }
    sel.value = pickedIp(st).address;
  }
  if (window.ppRefreshSelects) ppRefreshSelects();
  syncHostUi();
  applyAddrs();
}

/* ---------- Pumpkin server address (pumpkin.toml) ---------- */
function srvHostFromUi() {
  const m = $('#srvBindMode');
  const mode = m ? m.value : 'localhost';
  if (mode === 'custom') return cleanHost($('#srvBindCustom').value) || '';
  if (mode === 'current') {
    const p = $('#srvBindPick');
    return (p && p.value) || (pickedIp(S.status) || {}).address || '0.0.0.0';
  }
  return '127.0.0.1';
}
function syncSrvUi() {
  const m = $('#srvBindMode');
  const mode = m ? m.value : 'localhost';
  const pw = $('#srvPickWrap'); if (pw) pw.hidden = mode !== 'current';
  const cw = $('#srvCustomWrap'); if (cw) cw.hidden = mode !== 'custom';
  const host = srvHostFromUi() || '127.0.0.1';
  const sb = S.serverBind || { java: 25565, bedrock: 19132, bedrockEnabled: true };
  const pv = $('#srvAddrPreview');
  if (pv) {
    const on = sb.bedrockEnabled !== false;
    pv.innerHTML = 'Java <span class="inline-code">' + esc(fmtAddr(host, sb.java || 25565)) + '</span>' +
      (on ? '<br>Bedrock <span class="inline-code">' + esc(fmtAddr(host, sb.bedrock || 19132)) + '</span>' : '');
  }
}
function renderServerBind() {
  const sb = S.serverBind;
  if (!sb) return;
  const mode = S.srvMode || bindModeOf(sb.host);
  const sel = $('#srvBindMode');
  if (sel && !S.srvEditing && sel.value !== mode) sel.value = mode;
  const list = currentIps(S.status);
  const pick = $('#srvBindPick');
  if (pick) {
    const extra = (mode === 'current' && sb.host && sb.host !== '0.0.0.0' && sb.host !== '::'
      && !list.some((a) => a.address === sb.host)) ? [{ name: 'bind', address: sb.host, internal: false }] : [];
    const all = list.concat(extra);
    const key = all.map((a) => a.address + '|' + a.name).join(',');
    if (pick.dataset.key !== key) {
      const prevVal = pick.value;
      pick.dataset.key = key;
      pick.innerHTML = all.map((a) => '<option value="' + esc(a.address) + '">' + esc(ipLabel(a) + ' \u00b7 ' + a.address) + '</option>').join('');
      if (S.srvEditing && prevVal && all.some((a) => a.address === prevVal)) pick.value = prevVal;
    }
    const want = (mode === 'custom' || mode === 'localhost') ? pickedIp(S.status).address : (sb.host === '0.0.0.0' || sb.host === '::' ? pickedIp(S.status).address : sb.host);
    if (!S.srvEditing) pick.value = want;
  }
  const ci = $('#srvBindCustom');
  if (ci && !S.srvEditing && ci !== document.activeElement) ci.value = mode === 'custom' ? sb.host : (S.srvCustom || '');
  if (window.ppRefreshSelects) ppRefreshSelects();
  syncSrvUi();
}
function loadServerBind() {
  if (!S.cfgParsed || !Object.keys(S.cfgParsed).length) return;
  const j = splitAddr(getPath(S.cfgParsed, 'networking.java.address'), 25565);
  const b = splitAddr(getPath(S.cfgParsed, 'networking.bedrock.nethernet.address'), 19132);
  const be = getPath(S.cfgParsed, 'networking.bedrock.enabled');
  S.serverBind = { host: j.host, java: j.port, bedrock: b.port, bedrockEnabled: be !== false };
  /* Mode always follows pumpkin.toml, not a previously saved choice. */
  if (!S.srvEditing) S.srvMode = bindModeOf(j.host);
  renderServerBind();
  applyAddrs();
  if (S.tab === 'home') paintPageSub();
}
async function saveServerBind() {
  const m = $('#srvBindMode');
  const mode = m ? m.value : 'localhost';
  const host = srvHostFromUi();
  if (!host) { toast('Isi IP custom dulu', 'err'); return; }
  const sb = S.serverBind;
  if (!sb) { toast('Muat konfigurasi dulu', 'err'); return; }
  const patch = [
    { key: 'networking.java.address', value: fmtAddr(host, sb.java || 25565) },
    { key: 'networking.bedrock.nethernet.address', value: fmtAddr(host, sb.bedrock || 19132) }
  ];
  await withBusy($('#btnSaveSrv'), async () => {
    try {
      await api('/api/config', { method: 'PUT', body: { patch } });
      S.srvMode = mode;
      if (mode === 'custom') {
        S.srvCustom = cleanHost($('#srvBindCustom').value); LS.set('pp.srvCustom', S.srvCustom);
      }
      S.srvEditing = false;
      try {
        const c = await api('/api/config');
        S.cfgToml = c.toml || ''; S.cfgParsed = parseToml(S.cfgToml);
        S.cfgOriginal = JSON.parse(JSON.stringify(S.cfgParsed));
        fillConfig();
        loadServerBind();
      } catch (e) {}
      toast('Alamat server disimpan — restart server agar berlaku');
    } catch (e) { toast(e.message, 'err'); }
  });
}

