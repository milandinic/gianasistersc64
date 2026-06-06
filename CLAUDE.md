# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A libGDX clone of the C64 platformer "Giana Sisters". It is a Gradle multi-project build with three modules: `core` (all game logic, platform-agnostic), `desktop` (LWJGL3 launcher), and `android` (Android launcher + the canonical asset directory). Uses libGDX 1.14.2 on Gradle 9.5.1 and the Android Gradle Plugin 8.13.

### Toolchain (important, non-obvious)

The build **runs on JDK 26** (the only JDK on PATH) but **emits Java 17 bytecode** (`options.release = 17` for `core`/`desktop` in the root `build.gradle`). Java 17 is required because Android's dexer (D8/R8) cannot consume Java 26 bytecode. Desktop runs fine on the JDK 26 runtime with Java 17 bytecode.

The **android module is pinned to a JDK 21 toolchain** (`java { toolchain { languageVersion = 21 } }` in `android/build.gradle`), because AGP 8.13's `JdkImageTransform` runs `jlink`, and JDK 26's `jlink` rejects the Android platform `java.base`. Corretto 21 is registered via `org.gradle.java.installations.paths` in `gradle.properties`. If that JDK is removed, the Android build breaks — any JDK 17–21 registered the same way will do. `core`/`desktop` keep the JDK 26 toolchain.

## Build & Run

Use the Gradle wrapper (`./gradlew` / `gradlew.bat`).

- Run on desktop: `./gradlew desktop:run`
- Build a runnable fat jar: `./gradlew desktop:dist` → `desktop/build/libs/`
- Build Android APK: `./gradlew android:assembleDebug` → `android/build/outputs/apk/debug/android-debug.apk` (requires the Android SDK and `local.properties` with `sdk.dir=...`; also requires a JDK 17–21 registered for the android toolchain — see Toolchain above)
- Install/launch on a connected device: `./gradlew android:run`
- Compile everything: `./gradlew build`

### Tests

The only test is `desktop/test/com/mdinic/game/giana/MapTest.java` (JUnit 4). It is a smoke test that loads every level pixmap (`new GameMap(i, null)` for all levels) to verify each level parses and contains a Giana start + end door. It uses the libGDX **headless backend** (`HeadlessApplication`) for `Gdx.files`/`Pixmap`, and `desktop/build.gradle` wires the `test/` dir into the Gradle build with `workingDir = ../android/assets`. Run it with `./gradlew desktop:test`.

## Assets

There is **one** asset directory: `android/assets/data/`. The desktop module does not copy assets — `desktop/build.gradle` sets `workingDir = ../android/assets` for the `run` task and folds that directory into the `dist` jar. All asset loads use paths like `Gdx.files.internal("data/...")` relative to that working dir. When adding assets, put them under `android/assets/data/` only.

## Architecture

### Platform abstraction via services

`core` defines three service interfaces in `core/.../service/`: `HighScoreService`, `SettingsService`, `GeneralService`. `SettingsService` and `GeneralService` have per-platform implementations (`*Desktop` in `desktop/`, `*Droid`/`*Drod` in `android/`); `HighScoreService` now has a single shared `core` implementation (see below). The launcher (`DesktopLauncher`, `AndroidLauncher`) constructs the implementations and injects them into the `GianaSistersC64` game object via setters before starting libGDX. Core code reaches them through `getGame().getHighScoreService()` etc. **Any platform-specific capability (persistence, exit dialogs) must go through a service interface — never call platform APIs from `core`.**

High scores are backed by **Supabase**, accessed through a single shared `core` implementation, `SupabaseHighScoreService`, that uses libGDX's cross-platform `Gdx.net` (no platform-specific high-score classes remain; the old Parse/Bolts backend and the `*Desktop`/`*Droid` stubs were removed). Leaderboard reads hit PostgREST directly with the anon key; writes go only through a `submit-score` Edge Function that verifies an HMAC-SHA256 signature over `name|score|level|ts` (RLS forbids direct anonymous inserts). Leaderboards are cached in libGDX `Preferences` for offline play and submissions made offline are queued and retried. Connection config lives in `android/assets/highscore.properties` (gitignored; see `highscore.properties.example`); if absent, the game runs offline-only. Backend assets live under `supabase/`. The HMAC secret ships in the client, so this blocks casual REST forgery but is not tamper-proof against a determined reverse-engineer. Pure encode/sign/serialize helpers (`ScoreCodec`, `ScoreStore`, `SupabaseConfig`) are unit-tested under `desktop/test`.

### Screen flow

`GianaSistersC64 extends com.badlogic.gdx.Game` and swaps `Screen`s. All screens extend `GianaSistersScreen` (which holds the shared `Game` + `MapRenderer` and constants `SCREEN_WIDTH=480`, `SCREEN_HEIGHT=320`, `LEVEL_COUNT=30`). Flow: `IntroScreen` → `LevelStartingScreen` → `GameScreen` → (`LevelOverScreen` | `BonusGameScreen` | `EnterYourNameScreen` → `HighScoreScreen`) → `GameCompletedScreen`. Screen transitions happen by calling `game.setScreen(new SomeScreen(...))` from within `render()`.

### Levels are encoded as pixels in PNG files

This is the central, non-obvious design. Levels are **not** Tiled/TMX maps. `android/assets/data/levels.png` (and `bonuslevels.png`) is a tall image where each level occupies a 16-row band (`LEVEL_PIXELBUFFER = 20` rows of vertical stride per level, level `n` starts at row `n * 20`). `GameMap.loadBinary()` reads pixels and maps **specific RGB color constants to game entities** — e.g. `0xff0000` = Giana start, `0xffffff` = solid tile, diamond/piranha/ball/bee/spider/quicksand/treat-box each have a dedicated color. Entity types whose color sets live in enums implement a `containsColor(int)` lookup: see `SimpleImageType`, `GoundMonsterType` (note the spelling), `FixedTrapType`. The Y axis is flipped on load (`newY = pixmapHeight - 1 - y`).

To add or change a map element you typically: (1) edit the level PNG with the right color, and (2) ensure that color is recognized in `GameMap.loadBinary` or in the relevant `*Type` enum's color list.

### Game model & update loop

`GameMap` is the live world state: the tile grid (`int[][] tiles`), collections of every entity type (`Array<Diamond>`, `Array<GroundMonster>`, `bees`, `fishes`, `balls`, `fixedTraps`, the `boss` Spider, etc.), and run state (`lives`, `score`, `level`, `time`, `diamondsCollected`). `GameMap.update(delta)` drives the simulation by calling `update(delta)` on Giana first, then every entity collection. Entities (`Giana`, `Bee`, `Fish`, `Ball`, `GroundMonster`, `Spider`, `Diamond`, `Bullet`, ...) hold a back-reference to their `GameMap` and a `Rectangle bounds`; collision is rectangle/tile-color based (`GameMap.isColidable`, `isDeadly`). Giana's state machine lives in `GianaState` (e.g. `DYING`, `DEAD`) and power-ups in `GianaPower`.

### Rendering

`MapRenderer` is a single long-lived object created once in `GianaSistersC64.create()` and shared across screens via the constructor. It owns the `OrthographicCamera`, `SpriteBatch`es, all loaded `Texture`/`TextureRegion`/`Animation` assets, and FreeType-generated `BitmapFont`s. `GameScreen.render()` clears with the map's background color, then calls `renderer.render(delta)` for the world and `OnscreenControlRenderer` for the touch controls. `Sounds` is likewise created once and shared; it centralizes all `Music`/`Sound` loading and a global mute flag wired to `SettingsService.isSoundEnabled()`.

### Per-level configuration

`LevelConf` is an enum with one entry per level (`INTRO`, `LEVEL1`..`LEVEL30`) carrying background color, brick color/type, and background music (`Sounds.BgMusic`). Indexed by `map.level` as `LevelConf.values()[map.level]`. `BonusLevelConf` is the analogous config for bonus levels.

## Conventions & gotchas

- Several identifiers have entrenched typos that are part of the public API — keep using them as-is: `GoundMonsterType`, `SmallDiamoind`, `collectDiamound()`, `GeneralServiceDrod`.
- The desktop backend is **LWJGL3** (`Lwjgl3Application`/`Lwjgl3ApplicationConfiguration` in `DesktopLauncher`). On JDK 26, LWJGL 3.3.3 prints a benign `Unsupported JNI version detected` warning at startup — it runs fine; a future LWJGL bump would clear it.
- libGDX's `Animation` is generic (`Animation<TextureRegion>`); all `MapRenderer` animation fields/locals are typed accordingly. The two `groundMonsterAnimations` arrays use raw `new Animation[]` on purpose (generic array creation is illegal in Java) — this is the source of the lone unchecked-operations note during `core` compile.
- `core` emits Java 17 bytecode (not 26) so the shared jar is dexable by Android; see Toolchain above.
- `delta` is clamped to `0.06f` max per frame in `GameScreen.render()` to keep physics stable on slow frames.
- `Keys.BACK` (Android) drives pause (`Gdx.input.setCatchKey(Keys.BACK, true)`); touching the screen un-pauses.
