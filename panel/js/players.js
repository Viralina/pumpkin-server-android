'use strict';
/* ============================== players ============================== */
function renderOnlineList() {
  const box = $('#onlineList');
  if (!box) return;
  const list = (S.status && S.status.players && S.status.players.list) ||
    (S.players && S.players.online) || [];
  const ops = (S.players && S.players.ops) || [];
  const opNames = new Set(ops.map((o) => String(o.name || '').toLowerCase()));
  $('#onlineSub').textContent = list.length + ' pemain online';
  if (!S.status || !S.status.running) {
    box.innerHTML = '<div class="empty"><svg class="ic"><use href="#i-power"/></svg><div>Server sedang offline</div></div>';
    return;
  }
  if (!list.length) {
    box.innerHTML = '<div class="empty"><svg class="ic"><use href="#i-users"/></svg><div>Belum ada pemain yang bergabung</div></div>';
    return;
  }
  box.innerHTML = list.map((n) => {
    const isOp = opNames.has(String(n).toLowerCase());
    return '<div class="list-item"><div class="avatar">' + esc(String(n).slice(0, 1).toUpperCase()) + '</div>' +
      '<div class="li-main"><div class="li-name">' + esc(n) + '</div>' +
      '<div class="li-sub">' + (isOp ? 'operator · online' : 'online sekarang') + '</div></div>' +
      '<div class="li-actions">' +
      '<button class="icon-btn sm" data-op="' + esc(n) + '" data-opstate="' + (isOp ? '1' : '0') + '" title="' +
      (isOp ? 'Cabut OP' : 'Jadikan OP') + '"><svg class="ic"><use href="#i-shield"/></svg></button>' +
      '<button class="icon-btn sm" data-gm="' + esc(n) + '" title="Gamemode"><svg class="ic"><use href="#i-sliders"/></svg></button>' +
      '<button class="icon-btn sm" data-kick="' + esc(n) + '" title="Kick"><svg class="ic"><use href="#i-x"/></svg></button>' +
      '<button class="icon-btn sm" data-ban="' + esc(n) + '" title="Ban"><svg class="ic"><use href="#i-trash"/></svg></button>' +
      '</div></div>';
  }).join('');
}
function renderPlayers() {
  renderOnlineList();
  const P = S.players || {};
  const ops = P.ops || [], wl = P.whitelist || [], known = P.known || [];

  $('#opsList').innerHTML = ops.length ? ops.map((o) =>
    '<div class="list-item"><div class="avatar">' + esc(String(o.name || '?').slice(0, 1).toUpperCase()) + '</div>' +
    '<div class="li-main"><div class="li-name">' + esc(o.name) + '</div><div class="li-sub">level ' + (o.level || 4) + '</div></div>' +
    '<button class="icon-btn" data-deop="' + esc(o.name) + '" title="Cabut OP"><svg class="ic" style="width:15px;height:15px"><use href="#i-x"/></svg></button></div>'
  ).join('') : '<div class="empty">Belum ada operator</div>';

  $('#wlList').innerHTML = wl.length ? wl.map((w) =>
    '<div class="list-item"><div class="avatar b">' + esc(String(w.name || '?').slice(0, 1).toUpperCase()) + '</div>' +
    '<div class="li-main"><div class="li-name">' + esc(w.name) + '</div><div class="li-sub">' + esc(String(w.uuid || '').slice(0, 8)) + '</div></div>' +
    '<button class="icon-btn" data-unwl="' + esc(w.name) + '" title="Hapus"><svg class="ic" style="width:15px;height:15px"><use href="#i-x"/></svg></button></div>'
  ).join('') : '<div class="empty">Whitelist kosong</div>';

  const flags = P.flags || {};
  const wlOn = typeof flags.whitelistActive === 'boolean' ? flags.whitelistActive
    : (getPath(S.cfgParsed, 'white_list') === true || getPath(S.cfgParsed, 'enforce_whitelist') === true);
  $('#wlSub').textContent = wlOn ? 'Aktif — hanya pemain terdaftar' : 'Nonaktif — semua boleh masuk';
  const wt = $('#wlToggle');
  if (wt) wt.checked = wlOn;
  const wtx = $('#wlTxt');
  if (wtx) wtx.textContent = wlOn ? 'Aktif' : 'Nonaktif';

  const bans = P.bans || [];
  const bs = $('#banSub');
  if (bs) bs.textContent = bans.length ? bans.length + ' pemain diban' : 'Belum ada pemain diban';
  const bl = $('#banList');
  if (bl) {
    bl.innerHTML = bans.length ? bans.map((b) =>
      '<div class="list-item"><div class="avatar b"><svg class="ic" style="width:15px;height:15px"><use href="#i-trash"/></svg></div>' +
      '<div class="li-main"><div class="li-name">' + esc(b.name || '?') + '</div>' +
      '<div class="li-sub">' + esc(b.reason || 'tanpa alasan') + '</div></div>' +
      '<button class="icon-btn sm" data-unban="' + esc(b.name || '') + '" title="Cabut ban"><svg class="ic"><use href="#i-x"/></svg></button></div>'
    ).join('') : '<div class="empty">Tidak ada ban aktif</div>';
  }

  const names = known.map((k) => k.name);
  $('#knownNames').innerHTML = names.map((n) => '<option value="' + esc(n) + '"></option>').join('');
  $('#knownList').innerHTML = names.length
    ? names.map((n) => '<button class="chip" data-fill="' + esc(n) + '">' + esc(n) + '</button>').join('')
    : '<span class="hint">Belum ada data pemain di usercache.</span>';
}

