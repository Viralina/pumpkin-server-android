'use strict';
/* ============================== config ============================== */
function setFieldValue(el, v) {
  if (el.type === 'checkbox') el.checked = v === true;
  else if (Array.isArray(v)) el.value = JSON.stringify(v);
  else el.value = v === undefined || v === null ? '' : v;
}
const isLeaf = (v) => v !== null && (typeof v !== 'object' || Array.isArray(v));
function fullGroups() {
  const groups = [];
  (function walk(obj, path) {
    const keys = Object.keys(obj).filter((k) => isLeaf(obj[k]));
    if (keys.length) groups.push({ path, keys: keys.map((k) => ({ key: path ? path + '.' + k : k, name: k, val: obj[k] })) });
    Object.keys(obj).forEach((k) => {
      const v = obj[k];
      if (v && typeof v === 'object' && !Array.isArray(v)) walk(v, path ? path + '.' + k : k);
    });
  })(S.cfgParsed || {}, '');
  return groups;
}
function renderFull() {
  const box = $('#cfgFull');
  if (!box) return;
  const groups = fullGroups();
  if (!groups.length) { box.innerHTML = '<div class="empty">Belum ada data konfigurasi</div>'; return; }
  box.innerHTML = groups.map((g) => {
    const fields = g.keys.filter((k) => typeof k.val !== 'boolean');
    const bools = g.keys.filter((k) => typeof k.val === 'boolean');
    return '<div class="card cfg-card"><div class="card-h"><h3><span class="bracket">[</span>' + esc(g.path || 'root') +
      '<span class="bracket">]</span></h3></div>' +
      (fields.length ? '<div class="cfg-grid">' + fields.map((k) =>
        '<label class="cfg-field"><span title="' + esc(k.key) + '">' + esc(k.name) + '</span><input class="input mono" data-key="' + esc(k.key) + '"' +
        (typeof k.val === 'number' ? ' type="number" step="any"' : ' type="text" spellcheck="false" autocomplete="off"') + '></label>'
      ).join('') + '</div>' : '') +
      (bools.length ? '<div class="switches">' + bools.map((k) =>
        '<label class="switch"><input type="checkbox" data-key="' + esc(k.key) + '"><span class="track"></span><span class="sw-txt">' + esc(k.name) + '</span></label>'
      ).join('') + '</div>' : '') +
      '</div>';
  }).join('');
}
function fillConfig() {
  renderFull();
  $$('#view-config [data-key]').forEach((el) => setFieldValue(el, getPath(S.cfgParsed, el.dataset.key)));
  updateCfgDirty();
  if (window.ppRefreshSelects) ppRefreshSelects();
  $('#cfgSavedBanner').hidden = true;
  renderHomeInfo();
}
function cfgPatch() {
  const decided = new Map();
  $$('#view-config [data-key]').forEach((el) => {
    const key = el.dataset.key;
    const orig = getPath(S.cfgParsed, key);
    let val, same = true, bad = false;
    if (el.type === 'checkbox') {
      val = el.checked; same = val === (orig === true);
    } else if (typeof orig === 'number') {
      if (el.value !== '') {
        val = Number(el.value);
        if (isNaN(val)) bad = true; else same = val === orig;
      }
    } else if (Array.isArray(orig)) {
      try { val = JSON.parse(el.value); same = JSON.stringify(val) === JSON.stringify(orig); }
      catch (e) { bad = true; }
    } else {
      val = el.value; same = String(val) === String(orig === undefined || orig === null ? '' : orig);
    }
    el.setAttribute('aria-invalid', bad ? 'true' : 'false');
    decided.set(key, bad || same ? null : { key, value: val });
  });
  return [...decided.values()].filter(Boolean);
}
function updateCfgDirty() {
  const n = cfgPatch().length;
  $('#cfgDirty').textContent = n ? n + ' perubahan belum disimpan' : 'Tidak ada perubahan';
  $('#btnCfgSave').disabled = n === 0;
}
async function saveConfig() {
  const patch = cfgPatch();
  if (!patch.length) { toast('Tidak ada perubahan'); return; }
  await withBusy($('#btnCfgSave'), async () => {
    try {
      const r = await api('/api/config', { method: 'PUT', body: { patch } });
      S.cfgToml = r.toml || '';
      S.cfgParsed = parseToml(S.cfgToml);
      fillConfig();
      loadServerBind();
      $('#cfgSavedBanner').hidden = false;
      toast('Konfigurasi disimpan');
      refreshWorlds();
    } catch (e) { toast(e.message, 'err'); }
  });
}

