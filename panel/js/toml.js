'use strict';
/* ============================ TOML (klien) ============================ */
function parseVal(v) {
  v = String(v).trim();
  if (/^".*"$/.test(v) || /^'.*'$/.test(v)) {
    try { return v[0] === '"' ? JSON.parse(v) : v.slice(1, -1); } catch (e) { return v.slice(1, -1); }
  }
  if (v === 'true') return true;
  if (v === 'false') return false;
  if (/^\[/.test(v)) { try { return JSON.parse(v.replace(/'/g, '"')); } catch (e) { return []; } }
  const n = Number(v);
  if (v !== '' && !isNaN(n)) return n;
  return v;
}
function parseToml(text) {
  const out = {};
  let cur = out;
  for (const raw of String(text || '').split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    if (line.startsWith('[') && line.endsWith(']')) {
      const parts = line.slice(1, -1).trim().split('.');
      cur = out;
      for (const p of parts) {
        if (typeof cur[p] !== 'object' || cur[p] === null) cur[p] = {};
        cur = cur[p];
      }
      continue;
    }
    const m = line.match(/^([A-Za-z0-9_-]+)\s*=\s*(.+)$/);
    if (!m) continue;
    cur[m[1]] = parseVal(m[2]);
  }
  return out;
}
function getPath(obj, key) {
  return key.split('.').reduce((o, k) => (o && typeof o === 'object' ? o[k] : undefined), obj);
}

