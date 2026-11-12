# Project notes — Pumpkin Panel (Android)

This document covers things that are not always obvious from the code: how to build, features
hidden in the native menu, the security model, and a checklist before publishing the repo.

App version: **1.0.0** (`versionCode` 5). Do not bump the version without changing
`build.sh` and `app/build.gradle.kts` together.

## Architecture at a glance

```
MainActivity (WebView + native menu)
        │  HTTP + SSE to 127.0.0.1:8087
        ▼
ApiServer (Java, replacement for server.js)
        │
        ├─ Pumpkin.java   process, log, world, backup, config, settings
        ├─ PanelService   foreground service + wakelock
        └─ Store          SharedPreferences
```

- Web panel: `panel/` (modular HTML/CSS/JS) → bundled into
  `app/src/main/assets/index.html` — one file, no framework/CDN, **always from the APK assets**.
  The bundled asset is not committed; `build.sh` regenerates it.
- Server data: `/sdcard/server` (world, `pumpkin.toml`, logs, plugins).
- Binary: `/data/data/com.pumpkin.panel/files/pumpkin`.
- Backups: `/sdcard/server/backups`.
- Panel diagnostic log: `/sdcard/server/panel.log`.

## Build

### Termux (used for this release APK)

```bash
cd pumpkin-server-android

# Plain APK
./build.sh
# output: build/apk/pumpkin-panel-plain.apk

# Full APK (official binary bundled)
./build.sh --with-binary /sdcard/pumpkin-aarch64-android
# output: build/apk/pumpkin-panel-full.apk
```

`build.sh` uses `aapt2`, `javac`, `d8`, `zip`, and `apksigner`; it does not need Gradle. The
debug keystore is generated automatically and is **not** committed (`.gitignore`). The panel and
binary are written to the staging folder `build/assets/`; the binary never enters the source
tree or git.

### Service notification

The foreground notification is shown only while the Pumpkin server is running. When the server
stops (from the panel or via the **Stop server** action on the notification), the service leaves
foreground mode and the notification is cancelled, but the panel service stays alive so the
local API remains usable. When the server starts again, the notification returns.

### Modular panel

The panel is no longer a single source file. All HTML/CSS/JS lives in `panel/`; `build.sh` runs
`tools/bundle-panel.sh` to merge it into one asset before `aapt2`. To inspect just the bundle
output:

```bash
bash tools/bundle-panel.sh /tmp/preview.html
```

Structure details and ordering rules are in [`docs/PANEL.md`](PANEL.md).

### GitHub Actions

The `.github/workflows/build.yml` workflow uses the same `build.sh` on an Ubuntu runner
(JDK 17 + Android SDK 34 + build-tools 34.0.0). APKs from CI are signed with the runner's debug
key, so they differ from a local APK. Uninstall first when switching APK sources.

### After changing the panel

Edit files in `panel/` (not the bundled asset), then rebuild. `build.sh` re-bundles
automatically; the panel is never read from `/sdcard` at runtime.

```bash
./build.sh
```

## Panel security

The panel is **private**:

- **An access token is generated automatically** the first time the app runs (16 random bytes, hex).
- **Every** panel request — HTML and `/api/*`, from localhost or the LAN — must include the token:
  `?token=...` for pages or the `x-token` header for the API.
- The app's WebView opens the panel with the token in the URL, so users do not have to type anything.
- Without a matching token, the server **does not send the panel HTML at all**; only a simple lock
  page with a token form appears.
- The token can be viewed/copied from **Settings → Panel access token → Copy token**.
- Binding to `0.0.0.0`/a LAN IP without a token is still forced to `127.0.0.1` as a safeguard.
- The WebView blocks every non-local `http`/`https` request. The panel loads no external assets.

The token is stored in the device's SharedPreferences. **Never** commit a token, keystore, or
`/sdcard/gh-token.txt` to the repo.

## Features that are not obvious in the UI

Some features deliberately have no large button, but they are available:

1. **Native app menu** — the phone icon in the panel topbar, the *Android app* card in Settings,
   or the Android **Back** button while the panel is open.
2. **Swap Pumpkin binary** — import from `/sdcard`, `/sdcard/Download`, `/sdcard/Download/Pumpkin`,
   or `/sdcard/server`. The old binary is kept as `pumpkin.bak`; the *Swap with backup* button
   flips them both ways.
3. **glibc binary detection** — import/start rejects Linux ELFs (`ld-linux-aarch64`) and only
   accepts Android builds (`/system/bin/linker64`).
4. **Download log** and **copy address** — small buttons in the console and on the connection
   address card.
5. **World seed** — save/randomize the seed from the Configuration panel.
6. **Theme & accent** — dark/light mode and a compact accent color dropdown in Settings.
7. **Minecraft button sound** — can be turned off with a toggle in Settings.
8. **Local JSON API** — every panel action uses `/api/*`; useful for automation on the same
   device. See the route list in the README.
9. **`panel.log` diagnostics** — records `START`, `EXIT`, and `EXEC GAGAL` along with the ELF
   interpreter.
10. **Fallback app folder** — the *Use app folder* button moves server data to
    `/data/data/com.pumpkin.panel/files/server` when `/sdcard` has problems.
11. **Hidden backup menu** — the *World backup* card and the Backup button on Overview are hidden
    initially. Long-press the **About & notes** card (±900 ms) to reveal them. The state is stored
    in localStorage `pp.menu.backup`; delete that key to hide it again.
12. **Online web panel (experimental)** — a card under **Experimental** contains the server name,
    panel address, port, and token. The **Online web panel** toggle defaults to **off**; while it
    is off, all its settings are grayed out and read-only, and the panel server is forced to bind
    to `127.0.0.1`. When turned on, the new address/port/token take effect.
13. **Separate Pumpkin server address (experimental)** — a three-mode choice (localhost / current
    IP / custom) for the **Pumpkin server** bind, separate from the web panel address. The value
    is written to `networking.java.address` and `networking.bedrock.nethernet.address` in
    `pumpkin.toml` while keeping the old port. Restart the server for it to take effect.
14. **Green gradient accent colors** — a compact dropdown with six colors from lime → green →
    teal, default `#22c55e`. Old defaults (`#ffaa00`/`#ff8a3d`) migrate to green automatically.
    The accent also drives "Minecraft green" surfaces: primary buttons, the power button, the
    hero strip/glow when online, toggles, avatars, quick icons, IP chips, and the brand badge.
15. **About & notes** — now a button that opens a user guide popup (usage, token, access from
    other devices, server address, backup). Build notes are no longer shown in the panel; this
    document remains for developers.
16. **Latest activity** — a card below Home shows the three most recent log lines and a shortcut
    button to the Console, filling the empty space at the end of Home.

## General / Experimental sections

The **More** menu is split into two sections by a segmented control. Every time the menu opens,
the **General** section is active again. Network settings still under test live in
**Experimental**, so they do not make the main settings page longer.

## Online web panel vs Pumpkin server

These two addresses are deliberately separate:

| Purpose | Storage | Default |
|---|---|---|
| Web panel bind (another phone opens the panel) | SharedPreferences `host` + `panelOnline` | `127.0.0.1`, online **off** |
| Pumpkin server bind (players connect) | `pumpkin.toml` → `networking.java.address`, `networking.bedrock.nethernet.address` | `0.0.0.0` |

- `Store.panelOnline()` defaults to `false`; `ApiServer.openListener()` forces loopback while off.
- `PUT /api/settings` accepts `panelOnline` and triggers `relisten()` when the value changes.
- `MainActivity.panelHost()` uses `127.0.0.1` while the panel is offline, so the WebView stays safe.
- The address on Home is derived from the **Pumpkin server** bind (TOML), not the panel address.
- A `0.0.0.0`/`::` bind address is shown as the device IP on the address card and the Home
  subtitle, because a wildcard address cannot be used by players to connect.
- The server bind mode is always derived from the contents of `pumpkin.toml` when data loads; an
  unsaved choice is preserved while switching between General/Experimental.
- When the online panel is enabled with an empty custom host, or one that is no longer active on
  the device, the panel automatically uses the current IP and informs the user, so the toggle does
  not get stuck.
- A rebase is only scheduled when the panel address actually changes (e.g. loopback → LAN or the
  reverse); enabling the panel at the same address does not reload the page.

## Console UX

The console panel on phones uses a **fixed** position measured from below the topbar to above the
tabbar (via the `--console-top`/`--console-bottom` variables updated on resize/orientation). The
log lives inside a bordered panel with internal scrolling, the command input sits above the log,
and the command chip row stays visible above the bottom navigation — the log no longer overlaps
the tabbar.

## Panel connection UX

The red **"Panel not connected"** banner does not appear instantly. The reason: when the WebView
just opened the panel, the page + token are still loading and SSE may reconnect briefly.

- On first opening the panel, the banner gets a **3 second** grace period; after a successful
  connection it is **1.5 seconds**.
- During the grace period the status pill shows **Connecting** (yellow), not red.
- If the cause is the token, the message is specific: *Panel token does not match*.
- The API address always uses `location.origin` when the page is served by the panel server, so an
  old address in `localStorage` (stale IP/port) no longer makes the banner appear falsely.
- If it is still 401, the panel automatically tries the server-embedded token (`window.__PP_TOKEN`)
  and then the page origin before giving up.
- **Connection status is determined by the API, not SSE.** One successful `/api/*` request
  immediately marks the panel as connected (and hides the banner). A dropped SSE — for example
  when the screen turns off or the WebView pauses the stream — no longer triggers the red banner;
  the panel checks `/api/ping` first before declaring itself offline.
- **Cause of the SSE drops (fixed):** `PanelService` sent `status` from the main thread, so the SSE
  socket write threw `NetworkOnMainThreadException`. The server then closed the connection and the
  panel thought it was offline. All SSE writes now run on the `sse-tx` executor (single thread,
  daemon), so the stream survives and the banner does not appear falsely.
- A 4 second watchdog calls `/api/ping` while the panel is visible and the status is still
  offline, and `visibilitychange` rechecks when the app is reopened.

The currently selected menu/option uses a **white background + black text** with a border and check
mark, so it is clearly distinct from fields that must be filled in manually. This rule applies to
dropdowns, segmented controls, the sidebar, and the tab bar in all themes.

## Intentional behavior

- `targetSdk 28` so the ELF binary may be executed from the app data folder (Android 10+ W^X
  policy). It also grants legacy access to `/sdcard`.
- The panel only shows **IPv4**, unique (no duplicates), at most **2 addresses per edition**
  (Java and Bedrock).
- Real-time traffic uses a **fixed 100 ms** sample and only reads `/proc/net/dev`.
- The console renders only the last ±400 lines; the full log is still available via download.
- All panel data stays local; there is no telemetry.

## Checklist before publishing to GitHub

- [ ] `versionName` stays `1.0.0`, `versionCode` 5 (or change both together).
- [ ] `app/src/main/assets/index.html` is regenerated from `panel/` (`bash tools/bundle-panel.sh`).
- [ ] No tokens/keystores/personal paths in the repo (`git status`, `git grep -i token`).
- [ ] The README explains both variants (plain and full) and the internet requirement for downloads.
- [ ] The repo is public; `LICENSE` (Apache-2.0) and `NOTICE` (Pumpkin/Mojang attribution) are correct.
- [ ] The `pumpkin-panel-plain.apk` and `pumpkin-panel-full.apk` APKs are tested on a clean device.
