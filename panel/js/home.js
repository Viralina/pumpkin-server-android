'use strict';
/* ---- informasi server (beranda): dari status + pumpkin.toml, bukan teks console ---- */
function renderHomeInfo() {
  const box = $('#homeInfo');
  if (!box) return;
  const st = S.status || {};
  const g = (k) => getPath(S.cfgParsed || {}, k);
  const txt = (v) => (v === undefined || v === null || v === '' ? '\u2014' : String(v));
  const yn = (v) => (v === true ? ['Aktif', 'good'] : v === false ? ['Nonaktif', 'bad'] : ['\u2014', '']);
  const jp = st.ports && st.ports.java;
  const bp = st.ports && st.ports.bedrock;
  const javaOff = g('networking.java.enabled') === false;
  const bedOff = (st.ports && st.ports.bedrockEnabled === false) || g('networking.bedrock.enabled') === false;
  const wl = (g('white_list') === undefined && g('enforce_whitelist') === undefined) ? undefined
    : (g('white_list') === true || g('enforce_whitelist') === true);
  const items = [
    ['Versi', [st.version ? 'v' + st.version : '\u2014', '']],
    ['Max pemain Java', [txt(st.maxPlayers || g('networking.java.max_players')), '']],
    ['Max pemain Bedrock', [txt(g('networking.bedrock.max_players')), '']],
    ['Gamemode', [txt(g('default_gamemode')), '']],
    ['Difficulty', [txt(g('default_difficulty')), '']],
    ['World', [txt(g('default_level_name')), '']],
    ['Seed', [txt((S.worlds && S.worlds.seed) || (st.world && st.world.seed) || g('seed')), ''], true],
    ['Ukuran world', [st.worldMB ? st.worldMB + ' MB' : '\u2014', '']],
    ['Java', javaOff ? ['Nonaktif', 'bad'] : [jp ? 'Port ' + jp : '\u2014', jp ? 'good' : '']],
    ['Bedrock', bedOff ? ['Nonaktif', 'bad'] : [bp ? 'Port ' + bp : '\u2014', bp ? 'good' : '']],
    ['Online mode Java', yn(g('networking.java.online_mode'))],
    ['Online mode Bedrock', yn(g('networking.bedrock.online_mode'))],
    ['Whitelist', yn(wl)],
    ['PvP', yn(g('pvp.enabled'))],
    ['Hardcore', yn(g('hardcore'))],
    ['Nether', yn(g('allow_nether'))],
    ['The End', yn(g('allow_end'))],
    ['View distance Java', [txt(g('networking.java.view_distance')), '']],
    ['Sim distance Java', [txt(g('networking.java.simulation_distance')), '']],
    ['View distance Bedrock', [txt(g('networking.bedrock.view_distance')), '']],
    ['Sim distance Bedrock', [txt(g('networking.bedrock.simulation_distance')), '']],
    ['Spawn protection', [txt(g('spawn_protection')), '']],
    ['OP level', [txt(g('op_permission_level')), '']],
    ['MOTD Java', [txt(st.motd || g('networking.java.motd')), ''], true],
    ['MOTD Bedrock', [txt(g('networking.bedrock.motd')), ''], true]
  ];
  box.innerHTML = items.map((it) =>
    '<div class="info-item' + (it[2] ? ' wide' : '') + '"><span class="ii-k">' + esc(it[0]) + '</span>' +
    '<span class="ii-v ' + it[1][1] + '" title="' + esc(it[1][0]) + '">' + esc(it[1][0]) + '</span></div>'
  ).join('');
}

function setStatus(st) {
  const prev = S.status;
  S.status = st;
  const running = !!st.running;
  const ip = st.ip || '127.0.0.1';
  const jp = (st.ports && st.ports.java) || 25565;
  const bp = (st.ports && st.ports.bedrock) || 19132;

  $('#hero').classList.toggle('on', running);
  $('#heroDot').className = 'dot ' + (running ? 'on' : 'off');
  $('#heroStatus').textContent = running ? 'Server online' : 'Server offline';
  $('#heroSub').textContent = running
    ? (st.managed ? 'Dikelola panel · siap menerima pemain' : 'Berjalan di luar panel · kontrol terbatas')
    : 'Tekan mulai untuk menjalankan server';
  $('#heroVer').textContent = st.version ? 'v' + st.version : 'Pumpkin';

  const bp2 = $('#btnPower');
  bp2.className = 'btn btn-lg ' + (running ? 'btn-danger' : 'btn-primary btn-lg pulse-btn');
  bp2.dataset.action = running ? 'stop' : 'start';
  bp2.innerHTML = '<svg class="ic"><use href="#' + (running ? 'i-stop' : 'i-play') + '"/></svg><span>' + (running ? 'Hentikan server' : 'Mulai server') + '</span>';

  const top = $('#btnPowerTop');
  if (top) {
    top.classList.toggle('on', running);
    top.dataset.action = running ? 'stop' : 'start';
    top.innerHTML = '<svg class="ic"><use href="#' + (running ? 'i-stop' : 'i-power') + '"/></svg>';
    top.title = running ? 'Hentikan server' : 'Mulai server';
    top.setAttribute('aria-label', top.title);
  }

  if (running && st.players) {
    S.players = S.players || {};
    S.players.online = st.players.list || [];
    renderOnlineList();
  }

  $('#hUptime').textContent = running ? fmtDur(uptimeSec()) : '—';
  $('#hPlayers').textContent = running ? (st.players ? st.players.count : 0) : '—';
  $('#hCpu').innerHTML = (running ? (st.cpu || 0) : '—') + ' <small>%</small>';
  $('#hRam').innerHTML = (running ? (st.mem || 0) : '—') + ' <small>MB</small>';
  const world = $('#sWorld');
  if (world) world.innerHTML = (st.worldMB || 0) + ' <small>MB</small>';
  const worldBar = $('#sWorldBar');
  if (worldBar) worldBar.style.width = Math.min(100, (st.worldMB || 0) / 20) + '%';
  const worldLbl = $('#sWorldLbl');
  if (worldLbl) worldLbl.textContent = st.worldMB ? (st.worldMB + ' MB dipakai world ' + ((st.world && st.world.name) || 'world')) : 'Belum dihitung';

  $('#pillMotd').lastElementChild.textContent = st.motd || 'Tanpa MOTD';
  renderHostPick(st);
  renderTips(st);

  if (S.tab === 'home') paintPageSub();
  paintConn();
  updateHomeLogs();
  $('#backupHint').textContent = st.worldMB ? '~' + st.worldMB + ' MB' : 'tar.gz';
  if (!prev || prev.running !== running) {
    if (running && !prev) toast('Server berjalan');
    if (!running && prev && prev.running) toast('Server berhenti', 'err');
  }
}

function renderTips(st) {
  const box = $('#homeTips');
  if (!box) return;
  const tips = [];
  const mb = st.worldMB || 0;
  if (!st.running) tips.push(['play', 'Server sedang mati. Tekan Mulai untuk menjalankan Pumpkin.']);
  if (mb > 800) tips.push(['archive', 'World ' + mb + ' MB. Buat backup dan pertimbangkan menghapus region lama.']);
  else if (!mb) tips.push(['archive', 'Ukuran world belum terbaca. Jalankan server sebentar lalu cek lagi.']);
  if ((st.cpu || 0) > 80) tips.push(['cpu', 'CPU tinggi (' + st.cpu + '%). Turunkan view distance atau kurangi plugin.']);
  if ((st.mem || 0) > 700) tips.push(['ram', 'RAM proses ' + st.mem + ' MB. Tutup aplikasi lain bila HP mulai lambat.']);
  if (st.ports && st.ports.bedrockEnabled === false) tips.push(['shield', 'Bedrock nonaktif. Aktifkan di Konfigurasi bila pemain HP ingin ikut.']);
  if (!tips.length) tips.push(['check', 'Semua terlihat normal. Server siap menerima pemain.']);
  box.innerHTML = tips.slice(0, 3).map((t) =>
    '<div class="tip"><svg class="ic"><use href="#i-' + t[0] + '"/></svg><span>' + esc(t[1]) + '</span></div>').join('');
}

/* traffic real-time: sampel tetap 100 ms, tanpa opsi ubah */
const NET_MS = 100;
let netPrev = null, netTimer = null;
const netSession = { rx: 0, tx: 0 };
function fmtRate(v) {
  v = Math.max(0, v || 0);
  if (v >= 1048576) return (v / 1048576).toFixed(2) + ' MB/s';
  if (v >= 1024) return (v / 1024).toFixed(1) + ' KB/s';
  return Math.round(v) + ' B/s';
}
function fmtBytes(v) {
  v = Math.max(0, v || 0);
  if (v >= 1073741824) return (v / 1073741824).toFixed(2) + ' GB';
  if (v >= 1048576) return (v / 1048576).toFixed(1) + ' MB';
  if (v >= 1024) return (v / 1024).toFixed(1) + ' KB';
  return Math.round(v) + ' B';
}
function paintNetRate(rx, tx) {
  if ($('#netDown')) $('#netDown').textContent = fmtRate(rx);
  if ($('#netUp')) $('#netUp').textContent = fmtRate(tx);
  if ($('#netTotal')) $('#netTotal').textContent = '↓ ' + fmtBytes(netSession.rx) + ' · ↑ ' + fmtBytes(netSession.tx);
}
let netBusy = false;
function startNet() {
  if (netTimer) return;
  netTimer = setInterval(async () => {
    if (document.hidden || S.tab !== 'home') { netPrev = null; return; }
    if (netBusy) return;
    netBusy = true;
    try {
      const n = await api('/api/net');
      const now = n.ts || Date.now();
      if (netPrev) {
        const dt = (now - netPrev.ts) / 1000;
        if (dt > 0) {
          const drx = Math.max(0, n.rx - netPrev.rx);
          const dtx = Math.max(0, n.tx - netPrev.tx);
          netSession.rx += drx; netSession.tx += dtx;
          paintNetRate(drx / dt, dtx / dt);
        }
      }
      netPrev = { rx: n.rx, tx: n.tx, ts: now };
    } catch (e) {} finally { netBusy = false; }
  }, NET_MS);
}

function renderSpark(metrics) {
  const box = $('#spark');
  if (!metrics.length) {
    box.innerHTML = '<div class="empty" style="height:100%;display:grid;place-items:center"><div><svg class="ic"><use href="#i-cpu"/></svg><div>Belum ada sampel</div></div></div>';
    return;
  }
  const W = 320, H = 100, pad = 6;
  const cpu = metrics.map((m) => Math.max(0, m.cpu || 0));
  const mem = metrics.map((m) => Math.max(0, m.mem || 0));
  const maxC = Math.max(10, ...cpu), maxM = Math.max(10, ...mem);
  const n = metrics.length;
  const X = (i) => pad + (i * (W - 2 * pad)) / Math.max(1, n - 1);
  const YC = (v) => H - pad - (v / maxC) * (H - 2 * pad);
  const YM = (v) => H - pad - (v / maxM) * (H - 2 * pad);
  const line = (arr, y) => arr.map((v, i) => (i ? 'L' : 'M') + X(i).toFixed(1) + ',' + y(v).toFixed(1)).join(' ');
  const area = (arr, y) => line(arr, y) + ' L' + X(n - 1).toFixed(1) + ',' + (H - pad) + ' L' + X(0).toFixed(1) + ',' + (H - pad) + ' Z';
  const A = S.accent || '#22c55e', B = '#55ffff';
  box.innerHTML =
    '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" height="100%" preserveAspectRatio="none">' +
    '<defs><linearGradient id="gA" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="' + A + '" stop-opacity=".35"/><stop offset="1" stop-color="' + A + '" stop-opacity="0"/></linearGradient>' +
    '<linearGradient id="gB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="' + B + '" stop-opacity=".25"/><stop offset="1" stop-color="' + B + '" stop-opacity="0"/></linearGradient></defs>' +
    '<g stroke="rgba(255,255,255,.05)" stroke-width="1"><line x1="0" y1="' + (H / 3) + '" x2="' + W + '" y2="' + (H / 3) + '"/><line x1="0" y1="' + (2 * H / 3) + '" x2="' + W + '" y2="' + (2 * H / 3) + '"/></g>' +
    '<path d="' + area(mem, YM) + '" fill="url(#gB)"/>' +
    '<path d="' + area(cpu, YC) + '" fill="url(#gA)"/>' +
    '<path d="' + line(mem, YM) + '" fill="none" stroke="' + B + '" stroke-width="1.8" stroke-linejoin="round" opacity=".8"/>' +
    '<path d="' + line(cpu, YC) + '" fill="none" stroke="' + A + '" stroke-width="2" stroke-linejoin="round"/>' +
    '</svg>';
}

/* ============================== logs ============================== */
function logMatches(e) {
  if (S.level !== 'ALL' && (e.level || '') !== S.level) return false;
  if (S.q) {
    const q = S.q.toLowerCase();
    return (e.msg || '').toLowerCase().includes(q) || (e.level || '').toLowerCase().includes(q) || (e.time || '').includes(q);
  }
  return true;
}
function lineHTML(e) {
  const lv = (e.level || '').toLowerCase();
  const srcCls = e.src === 'cmd' ? 'cmd' : e.src === 'panel' ? 'panel' : '';
  const label = e.level || (e.src === 'cmd' ? 'CMD' : e.src === 'panel' ? 'PANEL' : '');
  return '<div class="log-line ' + lv + ' ' + srcCls + '">' +
    '<span class="lt">' + esc(e.time || '') + '</span>' +
    '<span class="lv">' + esc(label) + '</span>' +
    '<span class="lm">' + esc(e.msg || '') + '</span></div>';
}
function addLog(e) {
  S.logs.push(e);
  if (S.logs.length > 1000) S.logs.splice(0, S.logs.length - 1000);
  if (S.tab === 'console') {
    if (logMatches(e)) appendLogLine(e, true);
  } else {
    if (e.src !== 'file') S.logSeen++;
    updateNavDot();
    updateHomeLogs();
  }
}
function appendLogLine(e, auto) {
  const list = $('#logList');
  if (!list) return;
  const wrap = $('#logWrap');
  const nearBottom = wrap.scrollHeight - wrap.scrollTop - wrap.clientHeight < 90;
  list.insertAdjacentHTML('beforeend', lineHTML(e));
  while (list.children.length > 400) list.removeChild(list.firstElementChild);
  if (S.autoscroll && (nearBottom || auto)) wrap.scrollTop = wrap.scrollHeight;
}
function renderConsole() {
  const list = $('#logList');
  const rows = S.logs.filter(logMatches).slice(-400);
  list.innerHTML = rows.map(lineHTML).join('') ||
    '<div class="empty" style="margin:14px"><svg class="ic"><use href="#i-term"/></svg><div>Tidak ada log yang cocok</div></div>';
  $('#logWrap').scrollTop = $('#logWrap').scrollHeight;
}
function updateHomeLogs() {
  const box = $('#homeAct');
  if (!box) return;
  const rows = S.logs.filter((e) => e.src !== 'file').slice(-3);
  const show = rows.length ? rows : S.logs.slice(-3);
  box.innerHTML = show.length ? show.map((e) => {
    const cls = e.level === 'ERROR' ? ' err' : e.level === 'WARN' ? ' warn' : '';
    return '<div class="act-row' + cls + '"><span class="at">' + esc(e.time || '--:--:--') + '</span>' +
      '<span class="am">' + esc(e.msg || '') + '</span></div>';
  }).join('') : '<div class="act-row"><span class="at">--:--:--</span><span class="am">Belum ada aktivitas.</span></div>';
  const hint = $('#homeActHint');
  if (hint) hint.textContent = S.logs.length ? (S.logs.length + ' baris log di Console') : 'Menunggu aktivitas…';
}
function updateNavDot() {
  const d = $('#navDot');
  if (d) d.hidden = S.tab === 'console' || S.logSeen === 0;
}

