# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A libGDX clone of the C64 platformer "Giana Sisters". It is a Gradle multi-project build with three modules: `core` (all game logic, platform-agnostic), `desktop` (LWJGL launcher), and `android` (Android launcher + the canonical asset directory). Pins libGDX 1.5.4 and targets Java 1.6 source compatibility.

## Build & Run

Use the Gradle wrapper (`./gradlew` / `gradlew.bat`).

- Run on desktop: `./gradlew desktop:run`
- Build a runnable fat jar: `./gradlew desktop:dist` → `desktop/build/libs/`
- Build Android APK: `./gradlew android:assembleDebug` (requires Android SDK; create `local.properties` with `sdk.dir=...`)
- Install/launch on a connected device: `./gradlew android:run`
- Compile everything: `./gradlew build`

### Tests

The only test is `desktop/test/com/mdinic/game/giana/MapTest.java` (JUnit 4). It is a smoke test that loads every level pixmap (`new GameMap(i, null)` for all levels) to verify each level parses and contains a Giana start + end door. It needs the LWJGL natives and the assets working directory. Note: `desktop/build.gradle` declares only `src/` as a source dir, so the `test/` dir is not wired into the Gradle build — run it from the IDE (Eclipse/IntelliJ), or add the test sourceSet to Gradle first.

## Assets

There is **one** asset directory: `android/assets/data/`. The desktop module does not copy assets — `desktop/build.gradle` sets `workingDir = ../android/assets` for the `run` task and folds that directory into the `dist` jar. All asset loads use paths like `Gdx.files.internal("data/...")` relative to that working dir. When adding assets, put them under `android/assets/data/` only.

## Architecture

### Platform abstraction via services

`core` defines three service interfaces in `core/.../service/`: `HighScoreService`, `SettingsService`, `GeneralService`. Each platform provides its own implementations (`*Desktop` in `desktop/`, `*Droid`/`*Drod` in `android/`). The launcher (`DesktopLauncher`, `AndroidLauncher`) constructs the platform implementations and injects them into the `GianaSistersC64` game object via setters before starting libGDX. Core code reaches them through `getGame().getHighScoreService()` etc. **Any platform-specific capability (persistence, network high scores, exit dialogs) must go through a service interface — never call platform APIs from `core`.** Android high scores use Parse/Bolts.

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
- `desktop/build.gradle` sets `mainClassName = "com.mdinic.game.giana.desktop.DesktopLauncher"`, but the actual class is `com.mdinic.game.giana.DesktopLauncher` (no `.desktop` package). The `desktop:run` task works because it uses the runtime classpath/JavaExec; the `dist` jar manifest main-class may be wrong. Launch from the IDE against `DesktopLauncher` if `dist` misbehaves.
- `delta` is clamped to `0.06f` max per frame in `GameScreen.render()` to keep physics stable on slow frames.
- `Keys.BACK` (Android) drives pause; touching the screen un-pauses.
