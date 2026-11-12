'use strict';
/* ============================== status ============================== */
function ipLabel(a) {
  if (a.internal) return 'HP ini';
  const n = String(a.name || '').toLowerCase();
  if (/^(ap|wlan|wifi|swlan)/.test(n)) return n + ' · wifi/hotspot';
  if (/^(ccmni|rmnet|wwan|pdp|data|cell)/.test(n)) return n + ' · data';
  return n || 'jaringan';
}
/* ---- panel address: Localhost / current IP / Custom ---- */
const fmtAddr = (h, p) => (String(h).includes(':') ? '[' + h + ']:' + p : h + ':' + p);
function cleanHost(v) {
  return String(v == null ? '' : v).trim().replace(/^[a-z]+:\/\//i, '').split('/')[0].replace(/:\d+$/, '');
}
function currentIps(st) {
  const v4 = ((st && st.ips) || []).filter((a) => a.family === 'IPv4' && !a.internal);
  return v4.length ? v4 : [{ name: 'loopback', address: (st && st.ip) || '127.0.0.1', family: 'IPv4', internal: true }];
}
function hostMode() {
  if (S.hostMode === 'localhost' || S.hostMode === 'current' || S.hostMode === 'custom') return S.hostMode;
  const h = S.settings && S.settings.settings && S.settings.settings.host;
  if (!h || h === '127.0.0.1' || h === 'localhost' || h === '::1') return 'localhost';
  if (h === '0.0.0.0' || h === '::') return 'current';
  return 'custom';
}
function pickedIp(st) {
  const list = currentIps(st);
  return list.find((a) => a.address === S.ipPick) || list[0];
}
function effHost() {
  const mode = hostMode();
  if (mode === 'custom') return cleanHost(S.hostCustom) || '127.0.0.1';
  if (mode === 'current') return pickedIp(S.status).address;
  return '127.0.0.1';
}
function addrRow(lbl, tag, host, port, v6) {
  const a = fmtAddr(host, port);
  return '<button type="button" class="addr-row' + (v6 ? ' ipv6' : '') + '" data-copy="' + esc(a) + '"><span class="ar-main"><span class="ar-lbl">' + esc(lbl) +
    '</span><span class="ar-val">' + esc(a) + '</span></span><span class="ar-tag">' + esc(tag) +
    '</span><svg class="ic"><use href="#i-copy"/></svg></button>';
}
function allAddrs(st) {
  const list = (st && st.ips && st.ips.length) ? st.ips.slice() : currentIps(st);
  const v4 = list.filter((a) => a.family === 'IPv4' || /^\d+\.\d+\.\d+\.\d+$/.test(a.address));
  // The panel uses IPv4 only; IPv6 addresses are not shown.
  return { v4, v6: [] };
}
function splitAddr(v, defPort) {
  let s = String(v == null ? '' : v).trim().replace(/^["']+|["']+$/g, '');
  /* [IPv6]:port */
  let m = s.match(/^\[(.*)\]:(\d+)$/);
  if (m) return { host: m[1], port: Number(m[2]) || defPort };
  /* host:port (host without a colon, e.g. IPv4 / hostname) */
  m = s.match(/^([^:]+):(\d+)$/);
  if (m) return { host: m[1], port: Number(m[2]) || defPort };
  /* host without a port (including IPv6 without brackets) */
  return { host: s.replace(/^\[|\]$/g, '') || '0.0.0.0', port: defPort };
}
function bindModeOf(host) {
  const h = String(host || '').trim();
  if (!h || h === '127.0.0.1' || h === 'localhost' || h === '::1') return 'localhost';
  if (h === '0.0.0.0' || h === '::') return 'current';
  /* current device IP: treat as "current IP" mode, not custom. */
  try {
    if (currentIps(S.status).some((a) => a.address === h)) return 'current';
  } catch (e) {}
  return 'custom';
}
/* Host actually used by the Pumpkin server (from pumpkin.toml), not the panel host. */
function serverBindHost() {
  const sb = S.serverBind;
  if (sb) return sb.host;
  return effHost();
}
/* 0.0.0.0/:: cannot be typed on another phone; show the device IP. */
function dialHost(host, st) {
  const h = String(host == null ? '' : host).trim();
  if (h && h !== '0.0.0.0' && h !== '::') return h;
  const a = pickedIp(st || S.status);
  return (a && a.address) || '127.0.0.1';
}
function applyAddrs() {
  const st = S.status;
  if (!st) return;
  const jp = (st.ports && st.ports.java) || (S.serverBind && S.serverBind.java) || 25565;
  const bp = (st.ports && st.ports.bedrock) || (S.serverBind && S.serverBind.bedrock) || 19132;
  const off = (st.ports && st.ports.bedrockEnabled === false) || (S.serverBind && S.serverBind.bedrockEnabled === false);
  const mode = S.serverBind ? bindModeOf(S.serverBind.host) : hostMode();
  const { v4 } = allAddrs(st);
  const seen = new Set();
  const rows = [];
  const add = (lbl, tag, host) => {
    host = String(host || '').trim();
    if (!host || seen.has(host)) return;
    seen.add(host);
    rows.push({ lbl: lbl, tag: tag, host: host });
  };
  if (mode === 'current') {
    const bound = S.serverBind && S.serverBind.host;
    const a = (bound && bound !== '0.0.0.0' && bound !== '::' && v4.find((x) => x.address === bound)) || pickedIp(st);
    add(ipLabel(a), 'IP', a.address);
    v4.filter((x) => x.address !== a.address)
      .sort((x, y) => Number(x.internal) - Number(y.internal))
      .forEach((x) => add(ipLabel(x), 'IP', x.address));
  } else if (mode === 'custom') {
    add('custom', 'CUSTOM', S.serverBind ? S.serverBind.host : cleanHost(S.hostCustom));
  } else {
    add('localhost', 'LOCAL', '127.0.0.1');
  }
  // Unique IPv4 only, at most 2 addresses per edition (Java/Bedrock).
  const list = rows.slice(0, 2);
  const paint = (port) => list.map((r) => addrRow(r.lbl, r.tag, r.host, port)).join('');
  const empty = '<div class="addr-empty">Atur alamat server di Pengaturan &rarr; Eksperimental</div>';
  const jl = $('#javaAddrList');
  if (jl) jl.innerHTML = list.length ? paint(jp) : empty;
  const bl = $('#bedrockAddrList');
  if (bl) bl.innerHTML = off ? '<div class="addr-empty">Bedrock nonaktif</div>' : (list.length ? paint(bp) : empty);
  const pj = $('#portJava');
  if (pj) pj.textContent = jp;
  const pb = $('#portBedrock');
  if (pb) pb.textContent = bp;
  const bs = $('#bedrockSub');
  if (bs) bs.innerHTML = off ? 'Nonaktif' : 'Port <b>' + bp + '</b>';
}
function paintPageSub() {
  const st = S.status;
  if (!st || S.tab !== 'home') return;
  const jp = (st.ports && st.ports.java) || (S.serverBind && S.serverBind.java) || 25565;
  $('#pageSub').textContent = (st.running ? 'Online' : 'Offline') + ' \u00b7 ' +
    (st.players ? st.players.count : 0) + ' pemain \u00b7 ' + fmtAddr(dialHost(serverBindHost(), st), jp);
}
function paintNet() {
  const mode = hostMode();
  const nh = $('#netHint');
  const on = !!S.panelOnline;
  const T = (s) => (window.ppT ? window.ppT(s) : s);
  if (nh) {
    const raw = !on
      ? 'Panel web mati: hanya bisa dibuka dari HP ini (127.0.0.1).'
      : (mode === 'localhost'
        ? 'Panel hanya bisa dibuka dari HP ini.'
        : 'Panel terbuka di jaringan. Gunakan token untuk mengakses dari perangkat lain.');
    nh.__ppSrc = raw;
    nh.textContent = T(raw);
    if (nh.firstChild) nh.firstChild.__ppSrc = raw;
  }
  const nu = $('#netUrls');
  if (!nu) return;
  if (!on) { nu.innerHTML = ''; return; }
  const portEl = $('#setPort');
  const port = Number(portEl && portEl.value) || (S.settings && S.settings.settings && S.settings.settings.port) || 8087;
  let hosts = [];
  if (mode === 'current') hosts = currentIps(S.status).map((a) => a.address);
  else if (mode === 'custom' && cleanHost(S.hostCustom)) hosts = [cleanHost(S.hostCustom)];
  nu.innerHTML = hosts.map((h) => '<span class="inline-code" style="display:inline-block;margin:2px 4px 0 0">' + esc('http://' + fmtAddr(h, port)) + '</span>').join('');
}

