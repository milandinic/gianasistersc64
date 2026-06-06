# High-score config env-var override Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let environment variables (`GIANA_*`) override `highscore.properties` per-value when running the game locally (e.g. from IntelliJ IDEA), so secrets need not be written to disk; production keeps using the file unchanged.

**Architecture:** Add a tiny `EnvLookup` interface and a `SupabaseConfig.fromSources(Properties, EnvLookup)` factory in `core` that resolves each config value as `env (if non-blank) → file property → empty`. `SupabaseHighScoreService.loadConfig()` switches to `fromSources` with a production `EnvLookup` delegating to `System.getenv`, and is restructured so env vars work even when no file is present. All env reading is pure-JDK and cross-platform (`System.getenv` returns `null` on Android, preserving the file/offline path there).

**Tech Stack:** Java 17 bytecode (no lambdas — codebase uses pre-lambda anonymous-class style), libGDX 1.14.2, JUnit 4, libGDX headless backend for service tests, Gradle wrapper (`gradlew.bat` on Windows / `./gradlew`).

**Reference spec:** `docs/superpowers/specs/2026-06-06-highscore-env-var-override-design.md`

---

## File Structure

- **Create** `core/src/com/mdinic/game/giana/service/EnvLookup.java` — one-method interface (`String get(String name)`); the seam that lets tests inject a fake environment.
- **Modify** `core/src/com/mdinic/game/giana/service/SupabaseConfig.java` — add `fromSources(Properties, EnvLookup)`; reimplement `fromProperties(Properties)` on top of it.
- **Modify** `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java:86-99` — `loadConfig()` consults env vars and supports the file-absent case.
- **Modify** `desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java` — add env-overlay unit tests.
- **Modify** `android/assets/highscore.properties.example` — document the `GIANA_*` override.
- **Modify** `CLAUDE.md:39` — note the env-var override in the high-score paragraph.

Each task is self-contained and leaves the build green.

---

### Task 1: Add the `EnvLookup` seam and `SupabaseConfig.fromSources`

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/EnvLookup.java`
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseConfig.java`
- Test: `desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java`

- [ ] **Step 1: Write the failing tests**

Add these four methods inside the existing `SupabaseConfigTest` class (after the
last test, before the closing brace). They reference `SupabaseConfig.fromSources`
and `EnvLookup`, which do not exist yet.

```java
    /** Small fake environment for tests: returns a value for a known name, else null. */
    private static EnvLookup env(final String name, final String value) {
        return new EnvLookup() {
            public String get(String n) {
                return name.equals(n) ? value : null;
            }
        };
    }

    private static Properties fullProps() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://file.supabase.co");
        p.setProperty("supabase.anonKey", "fileAnon");
        p.setProperty("supabase.functionsUrl", "https://file.functions.supabase.co");
        p.setProperty("score.secret", "fileSecret");
        return p;
    }

    @Test
    public void fromSources_envOverridesFileValue() {
        SupabaseConfig c = SupabaseConfig.fromSources(fullProps(),
                env("GIANA_SUPABASE_URL", "https://env.supabase.co"));

        assertEquals("https://env.supabase.co", c.url); // env wins
        assertEquals("fileAnon", c.anonKey);            // others fall back to file
        assertTrue(c.isConfigured());
    }

    @Test
    public void fromSources_blankEnvFallsBackToFile() {
        SupabaseConfig c = SupabaseConfig.fromSources(fullProps(),
                env("GIANA_SUPABASE_URL", "   ")); // blank == absent

        assertEquals("https://file.supabase.co", c.url);
        assertTrue(c.isConfigured());
    }

    @Test
    public void fromSources_envOnlyWithNullProps_isConfigured() {
        final Properties none = null;
        EnvLookup all = new EnvLookup() {
            public String get(String n) {
                if ("GIANA_SUPABASE_URL".equals(n)) return "https://env.supabase.co";
                if ("GIANA_SUPABASE_ANON_KEY".equals(n)) return "envAnon";
                if ("GIANA_SUPABASE_FUNCTIONS_URL".equals(n)) return "https://env.functions.supabase.co";
                if ("GIANA_SCORE_SECRET".equals(n)) return "envSecret";
                return null;
            }
        };

        SupabaseConfig c = SupabaseConfig.fromSources(none, all);

        assertTrue(c.isConfigured());
        assertEquals("https://env.supabase.co", c.url);
        assertEquals("envSecret", c.secret);
    }

    @Test
    public void fromSources_neitherSource_notConfigured() {
        EnvLookup empty = new EnvLookup() {
            public String get(String n) {
                return null;
            }
        };

        assertFalse(SupabaseConfig.fromSources(null, empty).isConfigured());
    }
```

Also add the import at the top of the test file (with the other `com.mdinic`
import):

```java
import com.mdinic.game.giana.service.EnvLookup;
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew desktop:test --tests com.mdinic.game.giana.SupabaseConfigTest`
(Windows: `gradlew.bat desktop:test --tests com.mdinic.game.giana.SupabaseConfigTest`)
Expected: COMPILE FAILURE — `cannot find symbol: class EnvLookup` and
`method fromSources`.

- [ ] **Step 3: Create `EnvLookup`**

Create `core/src/com/mdinic/game/giana/service/EnvLookup.java`:

```java
package com.mdinic.game.giana.service;

/**
 * Looks up a configuration value by environment-variable name. Production wires
 * this to {@link System#getenv(String)}; tests inject a fake. Kept as an
 * interface (not a lambda target by convention — this codebase uses anonymous
 * classes) so {@link SupabaseConfig#fromSources} stays unit-testable without
 * real process environment variables.
 */
public interface EnvLookup {
    /** @return the value for {@code name}, or {@code null} if unset. */
    String get(String name);
}
```

- [ ] **Step 4: Implement `fromSources` and reimplement `fromProperties`**

In `core/src/com/mdinic/game/giana/service/SupabaseConfig.java`, replace the
existing `fromProperties` method with the following two methods (keep everything
else in the file unchanged):

```java
    public static SupabaseConfig fromProperties(Properties p) {
        return fromSources(p, new EnvLookup() {
            public String get(String name) {
                return null;
            }
        });
    }

    /**
     * Resolves each value as: environment variable (if non-blank) wins over the
     * file property. {@code fileProps} may be {@code null} (no file present), in
     * which case only the environment is consulted.
     */
    public static SupabaseConfig fromSources(Properties fileProps, EnvLookup env) {
        return new SupabaseConfig(
                pick(env.get("GIANA_SUPABASE_URL"), prop(fileProps, "supabase.url")),
                pick(env.get("GIANA_SUPABASE_ANON_KEY"), prop(fileProps, "supabase.anonKey")),
                pick(env.get("GIANA_SUPABASE_FUNCTIONS_URL"), prop(fileProps, "supabase.functionsUrl")),
                pick(env.get("GIANA_SCORE_SECRET"), prop(fileProps, "score.secret")));
    }

    private static String prop(Properties p, String key) {
        return p == null ? null : p.getProperty(key);
    }

    /** Env value if present and non-blank, otherwise the file value. */
    private static String pick(String envValue, String fileValue) {
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        return fileValue;
    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew desktop:test --tests com.mdinic.game.giana.SupabaseConfigTest`
Expected: PASS — all 8 tests (4 existing `fromProperties_*` + 4 new
`fromSources_*`).

- [ ] **Step 6: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/EnvLookup.java \
        core/src/com/mdinic/game/giana/service/SupabaseConfig.java \
        desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java
git commit -m "feat: env-var override seam for Supabase config

Add EnvLookup interface and SupabaseConfig.fromSources(Properties, EnvLookup):
env value (if non-blank) wins per-field over the file property. fromProperties
is reimplemented on top with a null-returning env, so its behavior is unchanged.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Wire `loadConfig()` to env vars and support the file-absent case

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java:86-99`
- Test: `desktop/test/com/mdinic/game/giana/SupabaseHighScoreServiceTest.java` (existing offline test guards the no-config path)

Note: `loadConfig()` reads `Gdx.files`, so it can't be exercised by a pure unit
test without env vars set in the JVM — we rely on the existing
`worksAfterGdxRestored_offlineMode` test (no file, no env → offline) as the
regression guard, and on Task 1's `fromSources` tests for the resolution logic.

- [ ] **Step 1: Run the existing service test to confirm the baseline is green**

Run: `./gradlew desktop:test --tests com.mdinic.game.giana.SupabaseHighScoreServiceTest`
Expected: PASS (2 tests). This is the behavior we must preserve.

- [ ] **Step 2: Rewrite `loadConfig()`**

In `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java`,
replace the entire existing `loadConfig()` method (currently lines 86-99):

```java
    static SupabaseConfig loadConfig() {
        try {
            FileHandle fh = Gdx.files.internal("highscore.properties");
            if (!fh.exists()) {
                return SupabaseConfig.fromProperties(null);
            }
            Properties p = new Properties();
            p.load(fh.read());
            return SupabaseConfig.fromProperties(p);
        } catch (Exception e) {
            Gdx.app.error("HighScore", "config load failed, offline mode", e);
            return SupabaseConfig.fromProperties(null);
        }
    }
```

with:

```java
    static SupabaseConfig loadConfig() {
        try {
            FileHandle fh = Gdx.files.internal("highscore.properties");
            Properties p = null;
            if (fh.exists()) {
                p = new Properties();
                p.load(fh.read());
            }
            // Env vars override the file per-value; with no file, env-only still
            // works. System.getenv is plain JDK and returns null on Android, so
            // the Android path stays file -> offline.
            return SupabaseConfig.fromSources(p, new EnvLookup() {
                public String get(String name) {
                    return System.getenv(name);
                }
            });
        } catch (Exception e) {
            Gdx.app.error("HighScore", "config load failed, offline mode", e);
            return SupabaseConfig.fromProperties(null);
        }
    }
```

`EnvLookup` is in the same `service` package as this class, so no import is
needed.

- [ ] **Step 3: Run the full desktop test suite to verify nothing regressed**

Run: `./gradlew desktop:test`
Expected: PASS — `MapTest`, `ScoreCodecTest`, `ScoreStoreTest`,
`SupabaseConfigTest`, `SupabaseHighScoreServiceTest`. In particular
`worksAfterGdxRestored_offlineMode` still passes: no file + no `GIANA_*` env
vars in the test JVM → `isConfigured()` false → offline.

- [ ] **Step 4: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java
git commit -m "feat: loadConfig consults GIANA_* env vars, allows file-absent

Env vars now override highscore.properties per-value, and config resolves from
env alone when the file is missing (previously a missing file short-circuited
to offline, ignoring env vars). Production runs unchanged; IDEA can set the
four GIANA_* env vars instead of writing secrets to disk.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Document the env-var override

**Files:**
- Modify: `android/assets/highscore.properties.example`
- Modify: `CLAUDE.md` (high-score paragraph, line 39)

- [ ] **Step 1: Update the example file header**

Replace the entire contents of `android/assets/highscore.properties.example`
with:

```properties
# Copy this file to highscore.properties (same dir) and fill in your Supabase
# project values. highscore.properties is gitignored. If it is missing or any
# value is blank, the game runs with offline-only high scores (cached lists,
# queued submits) and never contacts the network.
#
# For local development (e.g. IntelliJ IDEA), you can instead set these as
# environment variables, which OVERRIDE the file per-value (handy to keep
# secrets off disk). In an IDEA run configuration use the "Environment
# variables" field:
#   GIANA_SUPABASE_URL            -> supabase.url
#   GIANA_SUPABASE_ANON_KEY       -> supabase.anonKey
#   GIANA_SUPABASE_FUNCTIONS_URL  -> supabase.functionsUrl
#   GIANA_SCORE_SECRET            -> score.secret
# Env vars alone are sufficient even with no highscore.properties present.
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.anonKey=YOUR_ANON_KEY
supabase.functionsUrl=https://YOUR_PROJECT.functions.supabase.co
# Must exactly match SCORE_SECRET set on the submit-score Edge Function.
score.secret=CHANGE_ME_SHARED_SECRET
```

- [ ] **Step 2: Update the `CLAUDE.md` high-score paragraph**

In `CLAUDE.md`, find this sentence in the high-score paragraph (line 39):

```
Connection config lives in `android/assets/highscore.properties` (gitignored; see `highscore.properties.example`); if absent, the game runs offline-only.
```

Replace it with:

```
Connection config lives in `android/assets/highscore.properties` (gitignored; see `highscore.properties.example`); if absent, the game runs offline-only. The four values can be overridden per-value by environment variables (`GIANA_SUPABASE_URL`, `GIANA_SUPABASE_ANON_KEY`, `GIANA_SUPABASE_FUNCTIONS_URL`, `GIANA_SCORE_SECRET`) — env wins over the file, and env-only works with no file present (used for local/IDEA runs to keep secrets off disk). Env reading lives in `SupabaseHighScoreService.loadConfig` via the testable `EnvLookup` seam; `System.getenv` returns `null` on Android, so that path stays file → offline.
```

- [ ] **Step 3: Verify the docs build nothing / just sanity-check**

These are docs only — no compile step. Confirm the example file still has all
four keys and the `CLAUDE.md` sentence reads correctly.

Run: `git diff --stat android/assets/highscore.properties.example CLAUDE.md`
Expected: both files show as modified.

- [ ] **Step 4: Commit**

```bash
git add android/assets/highscore.properties.example CLAUDE.md
git commit -m "docs: document GIANA_* env-var override for high-score config

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review notes

- **Spec coverage:** precedence (Task 1 `pick`), four `GIANA_*` names (Task 1 `fromSources` + Task 3 docs), env reading in `core` `loadConfig` via `EnvLookup` (Task 2), file-absent → env-only (Task 2 rewrite), retained `fromProperties` (Task 1), 4 unit tests (Task 1), docs (Task 3). All covered.
- **Type consistency:** `EnvLookup.get(String)`, `SupabaseConfig.fromSources(Properties, EnvLookup)`, and the `GIANA_*` names match across Tasks 1, 2, and the tests.
- **No placeholders:** every code/command step shows full content.
- **Convention:** anonymous `EnvLookup` instances (no lambdas), matching existing `core` style.