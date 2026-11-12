#!/usr/bin/env bash
# Bundel panel modular (panel/index.html + css/ + js/) menjadi satu file HTML
# yang siap dimasukkan ke APK. Tidak butuh Node/Python — hanya bash + cat.
#
#   bash tools/bundle-panel.sh                 # -> app/src/main/assets/index.html
#   bash tools/bundle-panel.sh /path/out.html  # -> file lain (mis. untuk dites)
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
TPL="$ROOT/panel/index.html"
OUT=${1:-$ROOT/app/src/main/assets/index.html}
TMP="$OUT.tmp.$$"

[ -f "$TPL" ] || { echo "panel/index.html tidak ditemukan" >&2; exit 1; }
mkdir -p "$(dirname "$OUT")"
trap 'rm -f "$TMP"' EXIT

: > "$TMP"
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    '<link rel="stylesheet" href="css/'*'.css">')
      rel=${line#*href=\"}
      rel=${rel%\"\>}
      src="$ROOT/panel/$rel"
      [ -f "$src" ] || { echo "CSS tidak ditemukan: $rel" >&2; exit 1; }
      printf '<style>\n' >> "$TMP"
      cat "$src" >> "$TMP"
      printf '</style>\n' >> "$TMP"
      ;;
    '<script src="js/'*'></script>')
      rel=${line#*src=\"}
      rel=${rel%\"\></script>}
      src="$ROOT/panel/$rel"
      [ -f "$src" ] || { echo "JS tidak ditemukan: $rel" >&2; exit 1; }
      printf '<script>\n' >> "$TMP"
      cat "$src" >> "$TMP"
      printf '</script>\n' >> "$TMP"
      ;;
    *)
      printf '%s\n' "$line" >> "$TMP"
      ;;
  esac
done < "$TPL"

mv "$TMP" "$OUT"
trap - EXIT
printf 'Panel dibundel -> %s (%s bytes)\n' "$OUT" "$(wc -c < "$OUT" | tr -d ' ')"
