# Web panel — modular structure

The panel is vanilla HTML + CSS + JavaScript, with no framework, no CDN, and no Node build
step. When the APK is built, all files are merged into **one** `index.html` so the APK stays
self-contained and the runtime never reads `/sdcard`.

```
panel/
├── index.html          template (uses <link> and <script src>)
├── css/
│   ├── base.css        theme variables, reset, shell, sidebar, app bar, tabbar
│   ├── components.css  cards, buttons, chips, forms, console, toast, modal
│   ├── responsive.css  small screen / landscape adjustments
│   ├── minecraft.css   Minecraft-style surfaces, shadows, and textures
│   └── minecraft-extra.css  accents, animations, and theme details
├── js/
│   ├── core.js         `$`, `esc`, localStorage, initial state helpers
│   ├── api.js          API fetch, token, SSE, connection detection
│   ├── ui.js           toast, modal, view helpers
│   ├── status.js       server status, IP, metrics
│   ├── network.js      online panel + Pumpkin server bind
│   ├── home.js         home cards and summary
│   ├── commands.js     console, commands, start/stop/restart
│   ├── toml.js         client-side TOML parser
│   ├── config.js       pumpkin.toml editor
│   ├── players.js      player list, OP, whitelist
│   ├── worlds.js       worlds and backups
│   ├── settings.js     panel settings and paths
│   ├── events.js       all click/input handlers
│   ├── init.js         boot, polling, native APK menu
│   ├── interactions.js click sound, haptics, press animations
│   └── i18n.js         ID/EN dictionary and language dropdown
└── ../tools/bundle-panel.sh
```

## Bundling

```bash
bash tools/bundle-panel.sh                        # -> app/src/main/assets/index.html
bash tools/bundle-panel.sh /tmp/preview.html      # -> another file, for inspection
```

The script uses only `bash` + `cat`: each `<link rel="stylesheet" href="css/...">` line is
replaced by the file contents inside `<style>`, and each `<script src="js/..."></script>` line
is replaced by the file contents inside `<script>`. Every other line is copied as is.

`build.sh` runs the bundler automatically before `aapt2`. The bundled asset
(`app/src/main/assets/index.html`) is **not committed** — it is always regenerated so there is
no second source of truth.

## Adding or changing files

1. Add the file under `panel/css/` or `panel/js/`.
2. Register it in `panel/index.html` in the correct order.
3. Run `bash tools/bundle-panel.sh`, then open the result (or build the APK).

Order matters:

- CSS: `base` → `components` → `responsive` → `minecraft` → `minecraft-extra`.
  Later files may override earlier rules.
- JS: `core` must come first because it provides `$`, `esc`, `LS`, and the state.
  `i18n` must come last because it scans the DOM and already rendered text.
  Other modules may share global function names (no ES modules).

Most modules use an IIFE or plain global functions. If you add top-level `const`/`let`, make
sure the name is unique across the whole bundle — everything runs in one scope.

## Previewing in a browser

`panel/index.html` can be opened directly in a desktop browser. The layout appears, but the API
is unavailable so the status stays "not connected". The most accurate approach is still to build
the APK and open the panel from the app's WebView.

## Keeping it a single file

Do not add external assets (CDNs, fonts, images from the internet). The panel must keep working
offline and all of its content goes into one `index.html`. The click sound is currently embedded
as a data URI in `js/interactions.js`.
