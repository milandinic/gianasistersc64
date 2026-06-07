# Toolchain Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade Gradle (2.2→9.5.1), libGDX (1.5.4→1.14.2), Android Gradle Plugin (1.0.0→8.13), and Java (1.6→26) while keeping the desktop module (testing) and android module (distribution) buildable; stub out the dead Parse high-score backend.

**Architecture:** A Gradle multi-project build (`core`, `desktop`, `android`). Migrate to modern `pluginManagement`/`dependencyResolutionManagement` in `settings.gradle`, the Gradle Java toolchain for JDK 26, the LWJGL3 desktop backend, and the modern AGP `com.android.application` DSL. Parse is removed (maven dep + bundled jar) and `HighScoreServiceDroid` becomes a local-only stub mirroring `HighScoreServiceDesktop`.

**Tech Stack:** Gradle 9.5.1, libGDX 1.14.2, AGP 8.13, JDK 26, Android compileSdk/targetSdk 35 + minSdk 19, LWJGL3 + headless test backend.

**Environment (verified):** Only JDK 26 installed (`C:\Program Files\Java\jdk-26`, on PATH, `JAVA_HOME` set). Android SDK at `%LOCALAPPDATA%\Android\Sdk` with build-tools 35.0.0 and platform android-35. No `local.properties` yet.

**KNOWN RISK:** No released AGP officially supports JDK 26 (AGP 8.13 min/default JDK is 17). Per the approved spec, if `android:assembleDebug` fails because AGP rejects JDK 26, **finish desktop fully, then STOP and report** the exact error plus options (e.g. installing JDK 21 for the Android build only). Do NOT auto-install another JDK. Desktop is not at risk from this.

**Commands use PowerShell (Windows). Use `.\gradlew.bat` (or `gradlew` once the wrapper is regenerated).**

---

## File Structure

**Modify:**
- `settings.gradle` — add `pluginManagement` + `dependencyResolutionManagement`
- `build.gradle` (root) — remove buildscript/ext-cruft, convert `compile`→`implementation`, set toolchains
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.5.1
- `gradle.properties` — JVM args + `android.useAndroidX=true`
- `desktop/build.gradle` — LWJGL3 deps, headless test dep, wire `test/` sourceSet, fix `mainClassName`
- `desktop/src/com/mdinic/game/giana/DesktopLauncher.java` — LWJGL3 API
- `desktop/test/com/mdinic/game/giana/MapTest.java` — headless backend
- `android/build.gradle` — full AGP 8.13 DSL rewrite
- `android/AndroidManifest.xml` — drop `package=`, move version to defaultConfig
- `android/src/com/mdinic/game/giana/AndroidLauncher.java` — remove Parse.initialize
- `android/src/com/mdinic/game/giana/HighScoreServiceDroid.java` — remove Parse, local-only stub

**Create:**
- `local.properties` — `sdk.dir` (gitignored; not committed)

**Delete:**
- `android/libs/Parse-1.7.1.jar` + `android/libs/Parse-1.7.1.jar.properties`

---

## Task 1: Regenerate the Gradle wrapper to 9.5.1

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`

The current wrapper (2.2) cannot run on JDK 26. We set the distribution URL directly, then let Gradle self-update the wrapper jar/scripts on first run. We cannot run `gradlew wrapper` first because the old wrapper won't start under JDK 26.

- [ ] **Step 1: Point the wrapper at Gradle 9.5.1**

Edit `gradle/wrapper/gradle-wrapper.properties` to exactly:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
networkTimeout=10000
validateDistributionUrl=true
```

- [ ] **Step 2: Verify the new wrapper downloads and runs on JDK 26**

Run: `.\gradlew.bat --version`
Expected: Gradle **9.5.1**, JVM **26** (downloads the distribution on first run; this may take a minute).
If this fails because the root `build.gradle` still references the dead AGP classpath, that's fixed in Task 2 — re-run `--version` after Task 2 if needed. `--version` does not configure projects, so it should succeed here regardless.

- [ ] **Step 3: Regenerate wrapper jar + scripts**

Run: `.\gradlew.bat wrapper --gradle-version 9.5.1 --distribution-type bin`
Expected: BUILD SUCCESSFUL; `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat` updated.

- [ ] **Step 4: Commit**

```powershell
git add gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat
git commit -m @'
Upgrade Gradle wrapper to 9.5.1

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Task 2: Modernize settings.gradle and root build.gradle (core + desktop only)

This task makes `core` and `desktop` build on the new stack. The `android` project is temporarily excluded from `settings.gradle` so we can verify core/desktop in isolation before the AGP migration (Task 5). Android is re-added in Task 5.

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle` (root)
- Modify: `gradle.properties`

- [ ] **Step 1: Rewrite `settings.gradle`**

Replace entire contents with:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        maven { url "https://oss.sonatype.org/content/repositories/releases/" }
    }
}

rootProject.name = 'gianasisters64'
// android re-added in Task 5 after core/desktop verified
include 'desktop', 'core'
```

- [ ] **Step 2: Rewrite root `build.gradle`**

Replace entire contents with (note: NO `buildscript` block, NO `apply plugin: android`, dead `ext` vars removed, `compile`→`implementation`):

```groovy
allprojects {
    version = '1.0'
    ext {
        appName = 'gianasisters64'
        gdxVersion = '1.14.2'
    }

    repositories {
        google()
        mavenCentral()
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        maven { url "https://oss.sonatype.org/content/repositories/releases/" }
    }
}

project(":desktop") {
    apply plugin: "java"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(26)
        }
    }

    dependencies {
        implementation project(":core")
        implementation "com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion"
        implementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
        implementation "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop"
        testImplementation "junit:junit:4.13.2"
        testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
        testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
    }
}

project(":core") {
    apply plugin: "java"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(26)
        }
    }

    dependencies {
        implementation "com.badlogicgames.gdx:gdx:$gdxVersion"
        implementation "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    }
}
```

- [ ] **Step 3: Update `gradle.properties`**

Replace entire contents with:

```properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xms256m -Xmx1024m
org.gradle.configureondemand=false
android.useAndroidX=true
```

(`configureondemand=false`: it's deprecated/incompatible with modern AGP. `android.useAndroidX` is harmless now and needed once android returns in Task 5.)

- [ ] **Step 4: Verify core compiles**

Run: `.\gradlew.bat core:compileJava`
Expected: BUILD SUCCESSFUL. If libGDX 1.5→1.14.2 API drift causes compile errors in `core`, fix them minimally (see Task 3 note) — but core game logic is expected to compile unchanged.

- [ ] **Step 5: Commit**

```powershell
git add settings.gradle build.gradle gradle.properties
git commit -m @'
Modernize Gradle config: plugin management, libGDX 1.14.2, JDK 26 toolchain

Convert compile->implementation, remove dead ext vars (roboVM/ashley/
ai/box2dlights), drop legacy eclipse/idea/buildscript blocks. Android
temporarily excluded; re-added after AGP migration.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Task 3: Migrate DesktopLauncher to the LWJGL3 backend

**Files:**
- Modify: `desktop/src/com/mdinic/game/giana/DesktopLauncher.java`
- Modify: `desktop/build.gradle`

In libGDX 1.14.2 the desktop backend is LWJGL3: `LwjglApplication`→`Lwjgl3Application`, `LwjglApplicationConfiguration`→`Lwjgl3ApplicationConfiguration`.

- [ ] **Step 1: Rewrite `DesktopLauncher.java`**

Replace entire contents with:

```java
package com.mdinic.game.giana;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Giana Byte");
        config.setWindowedMode(480, 320);
        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new HighScoreServiceDesktop());
        gianaSistersC64.setSettingsService(new SettingsServiceDesktop());
        gianaSistersC64.setGeneralService(new GeneralServiceDesktop());
        new Lwjgl3Application(gianaSistersC64, config);
    }
}
```

(480x320 matches `GianaSistersScreen.SCREEN_WIDTH/HEIGHT`.)

- [ ] **Step 2: Fix `desktop/build.gradle` — `mainClassName` bug + wire test sourceSet**

The `java` plugin does not provide a `run` task (only the `application` plugin does, which we do not apply), so register `run` ourselves with `tasks.register`. Replace the entire contents of `desktop/build.gradle` with exactly:

```groovy
apply plugin: "java"

sourceSets.main.java.srcDirs = [ "src/" ]
sourceSets.test.java.srcDirs = [ "test/" ]

project.ext.mainClassName = "com.mdinic.game.giana.DesktopLauncher"
project.ext.assetsDir = new File("../android/assets")

tasks.register("run", JavaExec) {
    dependsOn classes
    mainClass = project.mainClassName
    classpath = sourceSets.main.runtimeClasspath
    standardInput = System.in
    workingDir = project.assetsDir
    ignoreExitValue = true
}

test {
    workingDir = project.assetsDir
}

tasks.register("dist", Jar) {
    dependsOn classes
    from files(sourceSets.main.output.classesDirs)
    from files(sourceSets.main.output.resourcesDir)
    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
    from files(project.assetsDir)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes 'Main-Class': project.mainClassName
    }
}
```

- [ ] **Step 3: Verify desktop compiles**

Run: `.\gradlew.bat desktop:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Verify the game launches**

Run: `.\gradlew.bat desktop:run`
Expected: A 480x320 game window opens showing the intro screen. Close it; task `run` ends (ignoreExitValue handles the exit code).

- [ ] **Step 5: Commit**

```powershell
git add desktop/src/com/mdinic/game/giana/DesktopLauncher.java desktop/build.gradle
git commit -m @'
Migrate desktop launcher to LWJGL3 backend; fix mainClassName

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Task 4: Migrate MapTest to the headless backend

**Files:**
- Modify: `desktop/test/com/mdinic/game/giana/MapTest.java`

`MapTest` is the level-loading smoke test. It used `LwjglFiles` + `LwjglNativesLoader` (LWJGL2, gone). The headless backend initializes `Gdx.files`/`Gdx.gl`/`Gdx.app` without a window. `GameMap.loadBinary` uses `Pixmap` (CPU-side, no GL context needed) so headless is sufficient.

- [ ] **Step 1: Rewrite `MapTest.java`**

Replace entire contents with:

```java
package com.mdinic.game.giana;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.mdinic.game.giana.screens.GianaSistersScreen;

public class MapTest {

    private HeadlessApplication app;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new com.badlogic.gdx.ApplicationAdapter() {
        }, config);
    }

    @After
    public void tearDown() {
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    @Test
    public void testRenderLevels() {
        for (int i = 0; i <= GianaSistersScreen.LEVEL_COUNT; i++) {
            new GameMap(i, null);
        }
    }
}
```

(`new GameMap(i, null)` parses each level pixmap and throws `IllegalStateException` if Giana start or end door is missing — that's the assertion.)

- [ ] **Step 2: Run the test, expect PASS**

Run: `.\gradlew.bat desktop:test --tests com.mdinic.game.giana.MapTest`
Expected: BUILD SUCCESSFUL, 1 test passed. The `test { workingDir = project.assetsDir }` from Task 3 ensures `data/levels.png` resolves.
If it fails with a native-loading error on `Pixmap`, add `com.badlogic.gdx.utils.GdxNativesLoader.load();` as the first line of `setUp()` and re-run.

- [ ] **Step 3: Commit**

```powershell
git add desktop/test/com/mdinic/game/giana/MapTest.java
git commit -m @'
Migrate MapTest to libGDX headless backend

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Task 5: Migrate the Android module to AGP 8.13

This is the largest task. AGP 8.13 needs Gradle ≥8.13 (have 9.5.1 ✓), Build Tools 35.0.0 (have ✓), compileSdk 35 (android-35 ✓). The legacy Eclipse `android` plugin, manual native-copy tasks, and Ant-era eclipse/idea blocks are all removed.

**Files:**
- Modify: `settings.gradle` (re-add android + AGP plugin version)
- Modify: `android/build.gradle` (full rewrite)
- Modify: `android/AndroidManifest.xml`
- Modify: `android/src/com/mdinic/game/giana/AndroidLauncher.java`
- Modify: `android/src/com/mdinic/game/giana/HighScoreServiceDroid.java`
- Create: `local.properties`
- Delete: `android/libs/Parse-1.7.1.jar`, `android/libs/Parse-1.7.1.jar.properties`

- [ ] **Step 1: Create `local.properties` (NOT committed — already in .gitignore)**

Write `local.properties` with exactly (note doubled backslashes):

```properties
sdk.dir=C\:\\Users\\midi\\AppData\\Local\\Android\\Sdk
```

- [ ] **Step 2: Re-add android to `settings.gradle` with the AGP plugin version**

Edit `settings.gradle`: add the AGP plugin to `pluginManagement` and re-add `android` to includes. The `pluginManagement` block becomes:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id 'com.android.application' version '8.13.0'
    }
}
```

And the include line becomes:

```groovy
include 'desktop', 'core', 'android'
```

- [ ] **Step 3: Remove the Parse maven dependency reference and stub `AndroidLauncher.java`**

Replace entire contents of `android/src/com/mdinic/game/giana/AndroidLauncher.java` with (Parse import + initialize removed):

```java
package com.mdinic.game.giana;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mdinic.game.giana.service.InternetConnectionChecker;

public class AndroidLauncher extends AndroidApplication implements InternetConnectionChecker {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setSettingsService(new SettingsServiceDroid(this));
        gianaSistersC64.setHighScoreService(new HighScoreServiceDroid(this, this));
        gianaSistersC64.setGeneralService(new GeneralServiceDrod(this));

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;
        initialize(gianaSistersC64, config);
    }

    @Override
    public boolean isAvailableConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
```

- [ ] **Step 4: Stub `HighScoreServiceDroid.java` — remove all Parse, keep offline SharedPreferences behavior**

Replace entire contents with (Parse imports/queries removed; offline-only; still implements `HighScoreService`; `goodForHighScores` mirrors original local logic):

```java
package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.InternetConnectionChecker;
import com.mdinic.game.giana.service.Score;

/**
 * Local-only high score service. The Parse backend was removed (servers
 * shut down in 2017); online high scores are deferred to a later iteration.
 * Scores persist in SharedPreferences on-device only.
 */
public class HighScoreServiceDroid implements HighScoreService {

    private static final int LIMIT_SCORES = 5;
    private static final String SCORE = "score";
    private static final String LEVEL = "level";
    private static final String USERNAME = "username";

    private static final String NAMEMY = "usernamemy";
    private static final String LEVELMY = "levelmy";
    private static final String SCOREMY = "scoremy";
    private static final String TOTALSCORES = "totalscores";

    private List<Score> scores = new ArrayList<Score>();
    private List<Score> todaysScores = new ArrayList<Score>();

    private final Activity activity;
    final Object object = new Object();

    boolean haveUpdate = true;
    boolean haveTodaysUpdate = true;

    public HighScoreServiceDroid(Activity activity, InternetConnectionChecker checker) {
        super();
        this.activity = activity;
        fetchHighScores();
        fetchTodaysHighScores(true);
    }

    @Override
    public void fetchHighScores() {
        synchronized (object) {
            scores = getOfflineScores("");
            haveUpdate = true;
        }
    }

    @Override
    public void fetchTodaysHighScores(boolean saveLocalScoreToWeb) {
        synchronized (object) {
            todaysScores = getOfflineScores("todays");
            haveTodaysUpdate = true;
        }
    }

    @Override
    public void saveHighScore(Score score) {
        Score myBestScore = getMyBest();
        if (myBestScore == null || myBestScore.getScore() < score.getScore()) {
            saveMyOfflineHighScore(score);
        }
        fetchHighScores();
        fetchTodaysHighScores(false);
    }

    private void saveMyOfflineHighScore(Score score) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SCOREMY, score.getScore());
        editor.putInt(LEVELMY, score.getLevel());
        editor.putString(NAMEMY, score.getName());
        editor.commit();
    }

    @Override
    public Score getMyBest() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int score = sharedPref.getInt(SCOREMY, 0);
        if (score != 0) {
            return new Score(sharedPref.getString(NAMEMY, ""), score, sharedPref.getInt(LEVELMY, 0));
        }
        return null;
    }

    @Override
    public boolean goodForHighScores(int score) {
        if (todaysScores.isEmpty() || todaysScores.size() < LIMIT_SCORES) {
            return true;
        }
        for (Score topScore : todaysScores) {
            if (topScore.getScore() < score) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Score> getScoreUpdate() {
        List<Score> result = new ArrayList<Score>(scores.size() + 1);
        synchronized (object) {
            haveUpdate = false;
            result.addAll(scores);
        }
        return result;
    }

    @Override
    public List<Score> getTodaysScoreUpdate() {
        List<Score> result = new ArrayList<Score>(todaysScores.size() + 1);
        synchronized (object) {
            haveTodaysUpdate = false;
            result.addAll(todaysScores);
        }
        return result;
    }

    @Override
    public boolean haveScoreUpdate() {
        synchronized (object) {
            return haveUpdate;
        }
    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        synchronized (object) {
            return haveTodaysUpdate;
        }
    }

    @Override
    public boolean internetAvailable() {
        return false;
    }

    private List<Score> getOfflineScores(String prefix) {
        List<Score> localScores = new ArrayList<Score>();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int highScores = sharedPref.getInt(prefix + TOTALSCORES, 0);
        for (int i = 0; i < highScores; i++) {
            localScores.add(new Score(sharedPref.getString(prefix + USERNAME + i, ""),
                    sharedPref.getInt(prefix + SCORE + i, 0), sharedPref.getInt(prefix + LEVEL + i, 0)));
        }
        return localScores;
    }
}
```

- [ ] **Step 5: Delete the bundled Parse jar**

Run:
```powershell
Remove-Item "android\libs\Parse-1.7.1.jar","android\libs\Parse-1.7.1.jar.properties" -Force
```

- [ ] **Step 6: Rewrite `android/build.gradle` for AGP 8.13**

Replace entire contents with:

```groovy
plugins {
    id "com.android.application"
}

android {
    namespace "com.mdinic.game.giana"
    compileSdk 35

    defaultConfig {
        applicationId "com.mdinic.game.giana"
        minSdk 19
        targetSdk 35
        versionCode 18
        versionName "1.17"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    sourceSets {
        main {
            manifest.srcFile 'AndroidManifest.xml'
            java.srcDirs = ['src']
            res.srcDirs = ['res']
            assets.srcDirs = ['assets']
            jniLibs.srcDirs = ['libs']
        }
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }
}

configurations { natives }

dependencies {
    implementation project(":core")
    implementation "com.badlogicgames.gdx:gdx-backend-android:$gdxVersion"
    implementation "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"

    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64"
    natives "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86"
    natives "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64"
}

// Extract libGDX native .so files into jniLibs by ABI so AGP packages them.
tasks.register("copyAndroidNatives") {
    doFirst {
        configurations.natives.files.each { jar ->
            def outputDir = null
            if (jar.name.endsWith("natives-arm64-v8a.jar")) outputDir = file("libs/arm64-v8a")
            if (jar.name.endsWith("natives-armeabi-v7a.jar")) outputDir = file("libs/armeabi-v7a")
            if (jar.name.endsWith("natives-x86_64.jar")) outputDir = file("libs/x86_64")
            if (jar.name.endsWith("natives-x86.jar")) outputDir = file("libs/x86")
            if (outputDir != null) {
                outputDir.mkdirs()
                copy {
                    from zipTree(jar)
                    into outputDir
                    include "*.so"
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn "copyAndroidNatives"
}
tasks.named("preBuild").configure {
    dependsOn "copyAndroidNatives"
}
```

(libGDX still ships native `.so` inside `natives-*.jar`, so we extract them into ABI-named folders under `libs/` and point `jniLibs.srcDirs` there. The bare `armeabi` and the old manual `PackageApplication` hook are gone.)

- [ ] **Step 7: Update `AndroidManifest.xml` — remove `package=`**

Replace entire contents with (package attr removed → now provided by `namespace`; version moved to defaultConfig; everything else preserved):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    android:launchMode="singleInstance">

    <uses-sdk android:minSdkVersion="19" android:targetSdkVersion="35" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        <activity
            android:name="com.mdinic.game.giana.AndroidLauncher"
            android:label="@string/app_name"
            android:screenOrientation="landscape"
            android:exported="true"
            android:configChanges="keyboard|keyboardHidden|orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

(`android:exported="true"` is REQUIRED on the launcher activity for targetSdk ≥31. Added `screenSize` to configChanges — standard for modern games. `versionCode`/`versionName` now live in `defaultConfig`.)

- [ ] **Step 8: Attempt the Android build**

Run: `.\gradlew.bat android:assembleDebug`
Expected (success): BUILD SUCCESSFUL; APK at `android/build/outputs/apk/debug/android-debug.apk`.

**IF THIS FAILS because AGP rejects JDK 26** (error mentioning unsupported Java/class file version, or AGP refusing the JDK): STOP. Do not attempt workarounds or install another JDK. Record the exact error text. Desktop work (Tasks 1–4) is already complete and committed. Report findings to the user per the plan's KNOWN RISK section.

For any OTHER failure (missing resource, manifest merge, native packaging), debug and fix it — those are not the JDK-compatibility wall.

- [ ] **Step 9: Commit (only if build succeeds OR if android config is correct-by-inspection and the only failure is the documented JDK-26 wall)**

```powershell
git add settings.gradle android/build.gradle android/AndroidManifest.xml android/src/com/mdinic/game/giana/AndroidLauncher.java android/src/com/mdinic/game/giana/HighScoreServiceDroid.java
git commit -m @'
Migrate Android module to AGP 8.13; stub out Parse high scores

Modern com.android.application DSL (namespace, compileSdk 35, minSdk 19),
AndroidX, AGP-managed native packaging. Parse SDK removed (maven dep +
bundled jar); HighScoreServiceDroid is now local-only. Online high
scores deferred.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Task 6: Full verification + update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Clean build of desktop + tests**

Run: `.\gradlew.bat clean desktop:build`
Expected: BUILD SUCCESSFUL (compiles + runs `MapTest`).

- [ ] **Step 2: Confirm versions**

Run: `.\gradlew.bat --version`
Expected: Gradle 9.5.1 on JVM 26.

- [ ] **Step 3: Update `CLAUDE.md` version references**

In `CLAUDE.md`, update the stated versions to reality. Specifically:
- libGDX 1.5.4 → **1.14.2**
- Gradle wrapper → **9.5.1**
- AGP → **8.13** (`com.android.application`)
- Java → **26** (Gradle Java toolchain)
- Android compileSdk/targetSdk 21 → **35**, minSdk 8 → **19**, buildTools → **35.0.0**
- Desktop backend note: now **LWJGL3** (`Lwjgl3Application`); `DesktopLauncher` uses `Lwjgl3ApplicationConfiguration`.
- `MapTest` now uses the **headless backend** and IS wired into Gradle (`gradlew desktop:test`).
- The `mainClassName` bug is **fixed** (remove that gotcha bullet).
- Parse high scores are **stubbed**; `HighScoreServiceDroid` is local-only; online high scores deferred.
- RoboVM/Ashley/ai/box2dlights `ext` vars **removed**.

- [ ] **Step 4: Commit**

```powershell
git add CLAUDE.md
git commit -m @'
Update CLAUDE.md for modernized toolchain

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
'@
```

---

## Final notes for the implementer

- **Verify, don't assume.** Each task's verification step must actually pass (showing real command output) before moving on. The headless test and `desktop:run` are the desktop gates.
- **The JDK-26/AGP wall is the one expected hard stop.** If you hit it in Task 5 Step 8, desktop is already done — report and hand back to the user. Everything else should be debugged through.
- **`local.properties` is never committed** (it's in `.gitignore`).
