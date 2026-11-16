'use strict';
/* ============================== status ============================== */
/* Address family selector: IPv4, IPv6, or both (IPv6 wildcard with dual-stack). */
const IP_FAMILY_OPTS = '<option value="ipv4">IPv4</option><option value="ipv6">IPv6</option><option value="both">IPv4 + IPv6</option>';
function ipFamilyOf(a) { return a && a.family === 'IPv6' ? 'ipv6' : 'ipv4'; }
function ipFamily() { return (S.ipFamily === 'ipv6' || S.ipFamily === 'both') ? S.ipFamily : 'ipv4'; }
function srvFamily() { return (S.srvFamily === 'ipv6' || S.srvFamily === 'both') ? S.srvFamily : 'ipv4'; }
/* 0.0.0.0 = all IPv4 interfaces; :: = all IPv6 interfaces (dual-stack when available). */
function familyHost(f) { return f === 'ipv4' ? '0.0.0.0' : '::'; }
function familyMatches(a, f) { return f === 'both' || ipFamilyOf(a) === f; }
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
  const s = String(v == null ? '' : v).trim().replace(/^[a-z]+:\/\//i, '').split('/')[0];
  const br = s.match(/^\[(.*)\](?::\d+)?$/);
  if (br) return br[1];
  /* Bare IPv6 has more than one colon and no port to strip. */
  if (s.indexOf(':') >= 0 && s.split(':').length > 2) return s;
  return s.replace(/:\d+$/, '');
}
function currentIps(st, fam) {
  fam = fam || ipFamily();
  const all = ((st && st.ips) || []).filter((a) => !a.internal && familyMatches(a, fam));
  if (all.length) return all;
  if (fam === 'ipv6') return [{ name: 'loopback', address: '::1', family: 'IPv6', internal: true }];
  return [{ name: 'loopback', address: (st && st.ip) || '127.0.0.1', family: 'IPv4', internal: true }];
}
function hostMode() {
  if (S.hostMode === 'localhost' || S.hostMode === 'current' || S.hostMode === 'custom') return S.hostMode;
  const h = S.settings && S.settings.settings && S.settings.settings.host;
  if (!h || h === '127.0.0.1' || h === 'localhost' || h === '::1') return 'localhost';
  if (h === '0.0.0.0' || h === '::') return 'current';
  return 'custom';
}
function pickedIp(st, fam) {
  return currentIps(st, fam)[0];
}
function effHost() {
  const mode = hostMode();
  if (mode === 'custom') return cleanHost(S.hostCustom) || '127.0.0.1';
  if (mode === 'current') return familyHost(ipFamily());
  return '127.0.0.1';
}
function addrRow(lbl, tag, host, port, v6) {
  const a = fmtAddr(host, port);
  return '<button type="button" class="addr-row' + (v6 ? ' ipv6' : '') + '" data-copy="' + esc(a) + '"><span class="ar-main"><span class="ar-lbl">' + esc(lbl) +
    '</span><span class="ar-val">' + esc(a) + '</span></span><span class="ar-tag">' + esc(tag) +
    '</span><svg class="ic"><use href="#i-copy"/></svg></button>';
}
function allAddrs(st, fam) {
  const f = fam || srvFamily();
  const list = (st && st.ips && st.ips.length) ? st.ips.slice() : currentIps(st, f);
  const v4 = list.filter((a) => ipFamilyOf(a) === 'ipv4' && familyMatches(a, f));
  const v6 = list.filter((a) => ipFamilyOf(a) === 'ipv6' && familyMatches(a, f));
  return { v4, v6 };
}
/* One IPv4 and one IPv6 first, so "IPv4 + IPv6" shows both families when 2 rows fit. */
function interleave(a, b) {
  const out = [];
  const n = Math.max(a.length, b.length);
  for (let i = 0; i < n; i++) {
    if (a[i]) out.push(a[i]);
    if (b[i]) out.push(b[i]);
  }
  return out;
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
    if (currentIps(S.status, 'both').some((a) => a.address === h)) return 'current';
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
  const a = pickedIp(st || S.status, srvFamily());
  return (a && a.address) || '127.0.0.1';
}
function applyAddrs() {
  const st = S.status;
  if (!st) return;
  const jp = (st.ports && st.ports.java) || (S.serverBind && S.serverBind.java) || 25565;
  const bp = (st.ports && st.ports.bedrock) || (S.serverBind && S.serverBind.bedrock) || 19132;
  const off = (st.ports && st.ports.bedrockEnabled === false) || (S.serverBind && S.serverBind.bedrockEnabled === false);
  const mode = S.serverBind ? bindModeOf(S.serverBind.host) : hostMode();
  const { v4, v6 } = allAddrs(st, srvFamily());
  const all = interleave(v4, v6);
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
    const a = (bound && bound !== '0.0.0.0' && bound !== '::' && all.find((x) => x.address === bound)) || pickedIp(st, srvFamily());
    add(ipLabel(a), ipFamilyOf(a) === 'ipv6' ? 'IPv6' : 'IP', a.address);
    all.filter((x) => x.address !== a.address)
      .sort((x, y) => Number(x.internal) - Number(y.internal))
      .forEach((x) => add(ipLabel(x), ipFamilyOf(x) === 'ipv6' ? 'IPv6' : 'IP', x.address));
  } else if (mode === 'custom') {
    add('custom', 'CUSTOM', S.serverBind ? S.serverBind.host : cleanHost(S.hostCustom));
  } else {
    add('localhost', 'LOCAL', '127.0.0.1');
  }
  // At most 2 addresses per edition (Java/Bedrock).
  const list = rows.slice(0, 2);
  const paint = (port) => list.map((r) => addrRow(r.lbl, r.tag, r.host, port, r.host.indexOf(':') >= 0)).join('');
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
  if (mode === 'current') hosts = currentIps(S.status, ipFamily()).map((a) => a.address);
  else if (mode === 'custom' && cleanHost(S.hostCustom)) hosts = [cleanHost(S.hostCustom)];
  nu.innerHTML = hosts.map((h) => '<span class="inline-code" style="display:inline-block;margin:2px 4px 0 0">' + esc('http://' + fmtAddr(h, port)) + '</span>').join('');
}

