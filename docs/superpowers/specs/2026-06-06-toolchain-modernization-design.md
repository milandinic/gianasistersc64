# Toolchain & Dependency Modernization — Design

**Date:** 2026-06-06
**Status:** Approved (pending spec review)

## Goal

Upgrade `gianasistersc64` from its 2013–2015 build toolchain to current versions while keeping **both** the desktop module (used for testing) and the android module (the distribution target) buildable. Target **JDK 26** everywhere. Defer all high-score / Parse backend work to a later iteration — in this iteration Parse is only stubbed so the Android module compiles.

## Decisions (from brainstorming)

- **Both modules stay.** Desktop = testing, Android = distribution.
- **libGDX → latest 1.13.x.**
- **JDK → 26 everywhere** (the only JDK installed: `C:\Program Files\Java\jdk-26`, on PATH, `JAVA_HOME` set). No JDK 21 will be installed.
- **Android minSdk → 19** (Android 4.4, maximum device reach; AndroidX minimum).
- **Parse → stubbed** so Android compiles; online high scores stay broken until a later iteration.
- **If AGP refuses to run under JDK 26:** get desktop fully working, then **stop and report** the exact error + recommendations. No auto-install of JDK 21.

## Environment (verified)

- JDK: only **26** installed (`C:\Program Files\Java\jdk-26`), `JAVA_HOME` and PATH point to it.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (no `ANDROID_HOME`/`ANDROID_SDK_ROOT` env var, no `local.properties` yet).
  - build-tools: 29.0.2, 30.0.2, 30.0.3, 33.0.2, 34.0.0, **35.0.0**
  - platforms: android-28, 31, 32, 33, 34, **35**, 36
- Current Gradle wrapper: **2.2**. AGP: **1.0.0** (legacy Eclipse `android` plugin). libGDX: **1.5.4**. Java source: **1.6**.

## Version target table

| Component | From | To |
|---|---|---|
| Gradle wrapper | 2.2 | latest **9.x** (only line that runs on JDK 26) |
| libGDX (`gdxVersion`) | 1.5.4 | latest **1.13.x** |
| Android Gradle Plugin | 1.0.0 (`apply plugin: "android"`) | latest **AGP 8.x** (`com.android.application`) |
| Java | source/target 1.6 | **26** (via Gradle Java toolchain) |
| Android `compileSdk` / `targetSdk` | 21 | **35** |
| Android `buildToolsVersion` | 20.0.0 | **35.0.0** (or AGP-managed default) |
| Android `minSdk` | 8 | **19** |
| Desktop backend | `gdx-backend-lwjgl` (LWJGL2) | **`gdx-backend-lwjgl3`** (LWJGL3) |
| RoboVM / Ashley / ai / box2dlights `ext` vars | declared, unused | **removed** |
| Parse / Bolts | live deps + live calls | **stubbed** (no SDK dependency) |
| Gradle config names | `compile` / `testCompile` | `implementation` / `testImplementation` |

## Architecture of the changes

Build-configuration migration, decomposed into isolated, independently-verifiable units.

### 1. `settings.gradle`
Add `pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }` and `dependencyResolutionManagement { repositories { google(); mavenCentral() } }`. AGP is resolved from `google()`. Keeps `include 'desktop', 'android', 'core'`.

### 2. Root `build.gradle`
- Remove `buildscript { classpath 'com.android.tools.build:gradle:1.0.0' }`.
- Remove dead `ext` vars: `roboVMVersion`, `box2DLightsVersion`, `ashleyVersion`, `aiVersion`. Keep `appName`, `gdxVersion` (→ 1.13.x).
- Remove the project-wide `apply plugin: "eclipse"` / `apply plugin: "idea"` and the `tasks.eclipse.doLast { delete ".project" }` hook (ADT-era cruft).
- Convert all `compile` → `implementation`, `testCompile` → `testImplementation`.
- `core`: `gdx` + `gdx-freetype`, Java toolchain 26.
- `desktop`: `gdx-backend-lwjgl3` + `gdx-platform:natives-desktop` + `gdx-freetype-platform:natives-desktop`; `testImplementation` JUnit **and** `gdx-backend-headless` for `MapTest`.
- `android`: moves to its own `android/build.gradle` using the modern AGP DSL (see §6). The root file no longer `apply plugin: "android"`.

### 3. Gradle wrapper
Regenerate `gradle/wrapper/gradle-wrapper.properties` (+ jar + scripts) to the latest **9.x** distribution. This is the only Gradle line known to run on JDK 26.

### 4. `core` module
- libGDX 1.13.x (`gdx`, `gdx-freetype`).
- Java toolchain 26.
- Fix any 1.5→1.13 API drift surfaced by compilation (expected to be minimal for core game logic).

### 5. `desktop` module — LWJGL3 migration (code change)
- Dependency: `gdx-backend-lwjgl` → `gdx-backend-lwjgl3`.
- `DesktopLauncher.java`:
  - `LwjglApplicationConfiguration` → `Lwjgl3ApplicationConfiguration`
  - `LwjglApplication` → `Lwjgl3Application`
  - Config setters as needed (current config sets nothing, so minimal).
- `MapTest.java`: switch from `LwjglFiles` + `LwjglNativesLoader` to the **headless backend** (`HeadlessApplication` + `HeadlessFiles`, or set `Gdx.files` via the headless backend) so the level-loading smoke test runs without a window.
- Wire `test/` into the Gradle build: add the test source dir to the desktop sourceSet (currently only `src/` is declared, so the test isn't built by Gradle today) — OR keep it IDE-only. **Decision: wire it into Gradle** so `gradlew desktop:test` runs the level smoke test.
- Fix `mainClassName`: `com.mdinic.game.giana.desktop.DesktopLauncher` → `com.mdinic.game.giana.DesktopLauncher` (the `.desktop` package does not exist; this affects the `dist` jar manifest).

### 6. `android` module — AGP 8.x migration (the big one)
- Replace legacy `android { ... }` (Eclipse-plugin DSL) with modern `com.android.application` plugin via the `plugins {}` block.
- `android {}` block: `namespace 'com.mdinic.game.giana'`, `compileSdk 35`, `defaultConfig { applicationId 'com.mdinic.game.giana'; minSdk 19; targetSdk 35; versionCode/versionName preserved from manifest }`, `compileOptions` / Java toolchain 26.
- `AndroidManifest.xml`: remove `package="com.mdinic.game.giana"` (now `namespace`). Keep `versionCode=18` / `versionName="1.17"` (or move into `defaultConfig`). Keep permissions, activity, intent-filter.
- Dependencies: `implementation project(':core')`, `gdx-backend-android`, `gdx-freetype`, and the `natives` configuration for `gdx-platform` / `gdx-freetype-platform` — but **drop x86/armeabi** that no longer ship; use `natives-armeabi-v7a`, `natives-arm64-v8a`, `natives-x86`, `natives-x86_64` per current libGDX. AGP packages these `.so` files automatically.
- **Delete** `copyAndroidNatives` task and the `PackageApplication` JNI hook — obsolete under AGP 8.x.
- **Delete** the Ant-era `eclipse` / `idea` blocks and `instrumentTest` source set.
- **Stub Parse:** remove `com.parse.bolts:bolts-android` dependency; in `AndroidLauncher.java` remove `import com.parse.Parse` and the `Parse.initialize(...)` call. `HighScoreServiceDroid` (which uses Parse) is reduced to a compiling no-op/local-only stub for this iteration — it must still satisfy the `HighScoreService` interface. Behavior change is acceptable and documented; full fix deferred.
- `gradle.properties`: add `android.useAndroidX=true` (and `android.enableJetifier=true` only if a transitive lib needs it). Keep/adjust existing JVM args.

### 7. `local.properties`
Create (gitignored) with `sdk.dir=C\:\\Users\\midi\\AppData\\Local\\Android\\Sdk` so the Android build finds the SDK. Not committed.

## Risks & unknowns

1. **AGP on JDK 26 is untested upstream.** No released AGP is validated against JDK 26. We attempt latest AGP + latest Gradle 9.x and verify by running `android:assembleDebug`. Per decision: if AGP hard-rejects JDK 26, finish desktop, then stop and report the exact error + options (e.g. install JDK 21 only for Android). Desktop is **not** at risk from this.
2. **libGDX 1.5→1.13 API drift.** ~9 years of changes. Most game APIs are stable, but some classes/signatures moved. Surfaced and fixed at compile time. The LWJGL2→LWJGL3 backend swap is the known, deliberate code change.
3. **Native artifact names changed** in modern libGDX (arm64-v8a/x86_64 added; bare `armeabi` dropped). Android `natives` config must use the current set.
4. **Headless test backend** must correctly initialize `Gdx.files`/`Gdx.gl` enough for `Pixmap`-based level loading in `MapTest`; if `Pixmap` needs GL, may fall back to `LwjglNativesLoader`-style native load without a window. Verified by running the test.

## Verification plan (evidence before "done")

| Unit | Verification |
|---|---|
| Gradle wrapper | `gradlew --version` shows 9.x running on JDK 26 |
| core + desktop compile | `gradlew desktop:build` succeeds |
| desktop runs | `gradlew desktop:run` launches the game window (LWJGL3) |
| level smoke test | `gradlew desktop:test` — `MapTest` loads all levels green |
| android compiles | `gradlew android:assembleDebug` produces an APK **or** yields the documented AGP/JDK 26 incompatibility error |

Desktop is fully verifiable. Android is best-effort-but-attempted: a real APK if AGP tolerates JDK 26, otherwise a precise findings report.

## Out of scope (explicitly deferred)

- Restoring online high scores / replacing the dead Parse backend (next iteration).
- Any gameplay, asset, or rendering changes beyond what the LWJGL3/libGDX upgrade strictly requires.
- Installing JDK 21 or any alternative JDK.
- Unrelated refactoring.
