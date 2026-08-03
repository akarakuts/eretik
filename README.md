# Eretik

[![License: GPL v2+](https://img.shields.io/badge/License-GPL%20v2%2B-blue.svg)](LICENSE)
[![CI](https://github.com/akarakuts/eretik/actions/workflows/ci.yml/badge.svg)](https://github.com/akarakuts/eretik/actions/workflows/ci.yml)

Russian / Русский: [README.ru.md](README.ru.md)

**Eretik** — Android port of classic **Heretic** (Raven Software, 1994), built on
[Crispy Heretic](https://github.com/fabiangreffrath/crispy-doom)
(a Chocolate Heretic fork with high-res 640×400, truecolor, uncapped FPS) and **SDL2**.

![Gameplay](docs/screenshot-demo.png)

| | |
|---|---|
| Package | `com.eretik.heretic` |
| Version | `1.1.1` (see `app/build.gradle.kts`) |
| Engine | Crispy Heretic (`crispy-doom` git master) |
| Orientation | Landscape |

## Features

- **Engine** — Crispy Heretic with compile-time truecolor (`-DCRISPY_TRUECOLOR`),
  high-res / truecolor enabled in `default.cfg` / `extra.cfg`.
- **Touch overlay** — on-screen joystick and buttons (HOLD / TOGGLE / TAP) that
  emit SDL key events; keyboard, mouse, and gamepad work as in desktop Crispy Heretic.
- **IWAD picker** — looks for `heretic.wad` → `heretic1.wad` → `blasphem.wad`
  in app storage; can import from the app-specific external files directory.
- **Android packaging** — `arm64-v8a` + `armeabi-v7a`, minSdk 21, 16 KB page-size
  alignment (Android 15+).

## Stack

| Component | Version | Role |
|---|---|---|
| crispy-doom | git master | engine (`crispy-heretic`), C |
| SDL2 | 2.32.10 | video, input, audio |
| SDL2_mixer | 2.8.1 | sound effects (WAV only; music via OPL) |
| SDL2_net | 2.2.0 | network play |
| Android Gradle Plugin | 9.2.1 | APK / AAB |
| NDK | 28.2.13676358 | native build (ndk-build) |
| Java | 17+ | app shell |

Own code lives in `com.eretik.heretic` (~touch controls + activity). Engine sources
are fetched by `scripts/fetch-deps.sh` into `deps/` / `crispy-doom/` (gitignored)
and linked from `app/src/main/jni/`.

## Requirements

- **JDK 17+**
- **Android SDK** (platforms android-35+, build-tools)
- **Android NDK 28.2.13676358** (pinned in `app/build.gradle.kts`)
- `local.properties` with `sdk.dir=...`
- Network once, to run `scripts/fetch-deps.sh`

## CI & automation

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [CI](.github/workflows/ci.yml) | push / PR to `main`, manual | fetch deps, `assembleDebug`, `assembleRelease` |
| [Security](.github/workflows/security.yml) | push / PR to `main`, weekly | OSV dependency scan, CodeQL (Java) |
| [Release](.github/workflows/release.yml) | tag `v*` | Upload-keystore–signed **APK + AAB** + GitHub Release (requires secrets) |

[Dependabot](.github/dependabot.yml) opens weekly PRs for Gradle and GitHub Actions.

## Build & run

```bash
git clone https://github.com/akarakuts/eretik.git
cd eretik
scripts/fetch-deps.sh
./gradlew assembleDebug
./gradlew installDebug   # device / emulator
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

For **signed release** builds, see [Release signing](#release-signing).

## Game data (IWAD)

The engine needs an IWAD. Options:

- **Full game**: `heretic.wad` from a legal Heretic copy (Steam / GOG).
- **Shareware**: `heretic1.wad` (episode 1; freely redistributable — idgames mirrors,
  e.g. `htic_v12.zip`). Prefer the canonical v1.2 file
  (md5 `ae779722390ec32fa37b0d361f7d82f8`).
- **Free replacement**: [Blasphemer](https://github.com/Blasphemer/blasphemer)
  as `blasphem.wad` (partial vanilla compatibility — see Known issues).

Place the file (keep the original name) in one of:

1. **Internal app storage** (debug):
   ```bash
   adb push heretic1.wad /data/local/tmp/
   adb shell "run-as com.eretik.heretic sh -c 'mkdir -p files && cat /data/local/tmp/heretic1.wad > files/heretic1.wad'"
   ```
2. **App external files dir** — `/sdcard/Android/data/com.eretik.heretic/files/`:
   on first launch the app copies a recognized IWAD into internal storage.

Configs (`default.cfg`, `extra.cfg`) and saves (`savegames/`) live in internal
storage; paths are passed to the engine from `HereticActivity`.

Commercial `heretic.wad` / `heretic1.wad` are **not** shipped in this repository
(`wad/*.wad` is gitignored).

## Controls

On-screen overlay plus keyboard / mouse / gamepad (same bindings as Crispy Heretic):

- **Joystick** (bottom-left): move / turn; diagonals press two keys at once
- **STR**: strafe latch — when active, left/right strafe instead of turn
- **RUN**: run latch (Shift)
- **FIRE** (Ctrl), **USE** (Space), **F+ / F-** (fly up / down)
- **ESC**, **MAP** (Tab), **WPN** (cycle weapons 1–7),
  **[ / ]** (inventory), **ART** (Enter — use artifact / confirm in menus)

![Touch controls](docs/screenshot-touch.png)

## Known issues

- **Blasphemer + demos**: demo loops can desync under vanilla visplane limits and
  hit `R_FindPlane: no more visplanes`. Prefer a real `heretic.wad` / `heretic1.wad`
  for play.
- Corrupt PSP-ripped shareware WADs can crash the menu when drawing font patches;
  use the canonical shareware v1.2 IWAD above.

## Release signing

`app/build.gradle.kts` loads **`keystore.properties`** from the repo root; if it
exists, **`signingConfigs.upload`** is applied to **`release`**; otherwise
**`release`** uses the **debug** keystore so fresh clones and CI still build
installable APKs.

### 1. Create an upload keystore (once)

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Keep **`upload-keystore.jks`** and passwords in a password manager; **back up**
the file — without it you cannot ship compatible updates.

### 2. Local signed `release` builds

1. Copy [`keystore.properties.example`](keystore.properties.example) to
   **`keystore.properties`** in the **repository root** (gitignored).
2. Set `storeFile`, passwords, and `keyAlias` to match your keystore.
3. Run:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs: `app/build/outputs/apk/release/*.apk` and
`app/build/outputs/bundle/release/*.aab`.

If **`keystore.properties` is missing**, `release` still signs with the **debug**
keystore — **do not** publish that build to a store.

### 3. GitHub Actions tag releases (`v*`)

Configure these **repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64 of `upload-keystore.jks` (e.g. `base64 -i upload-keystore.jks \| tr -d '\n'` on macOS) |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (e.g. `upload`) |
| `RELEASE_KEY_PASSWORD` | Key password |

The [Release](.github/workflows/release.yml) workflow writes `keystore.properties`
and `upload-keystore.jks` on the runner, builds signed **APK + AAB**, and attaches
`eretik-<tag>.apk` / `.aab` to the GitHub Release. Missing secrets fail the job
on purpose (no silent debug-signed store builds).

## Project layout

```
app/src/main/
├── AndroidManifest.xml
├── java/com/eretik/heretic/   # own Java: activity, IWAD, touch overlay
├── java/org/libsdl/app/       # vendored SDL2 Java glue (do not edit lightly)
└── jni/                       # ndk-build: crispy-heretic + SDL2*
scripts/fetch-deps.sh          # clone crispy-doom, download SDL2 tarballs, symlinks
docs/                          # screenshots + development notes
```

## Contact

**Aleksey Karakuts** — [aleksey@karakuts.com](mailto:aleksey@karakuts.com)

## License

Own code and the application as a whole are **GNU GPL v2 or later**
(see [`LICENSE`](LICENSE) / [`COPYING`](COPYING)). Required by the engine:
Crispy/Chocolate Doom and Heretic game code (id / Raven) are GPL-2.0+.

- Crispy/Chocolate Doom, Heretic code — GPL-2.0+ (`crispy-doom/COPYING.md` after fetch).
- SDL2, SDL2_mixer, SDL2_net — zlib (GPL-compatible).
- Blasphemer — free assets (see its repository).
- Original `heretic.wad` — commercial id Software data; not included here.

Copyright (C) 2026 Aleksey Karakuts &lt;aleksey@karakuts.com&gt;
