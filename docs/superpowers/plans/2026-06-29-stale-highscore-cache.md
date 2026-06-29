# Stale High-Score Cache Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the daily high-score board from silently skipping name entry when the local cache holds a previous day's board, by stamping the cached today's-board with its UTC day and treating a stale/missing stamp as an empty board.

**Architecture:** The client gate (`goodForHighScores`) is a generous pre-filter; Supabase is the judge. We persist a `K_TODAYS_DAY` UTC-day stamp alongside the cached today's-board (`K_TODAYS`). `goodForHighScores` treats a missing or previous-day stamp as an empty board so any positive score qualifies. A proactive `fetch()` at service init keeps the in-memory board fresh online. Time is supplied through an injectable `Clock` seam (mirroring the existing `EnvLookup` seam) so the day-boundary logic is unit-testable.

**Tech Stack:** Java 17 bytecode (built on JDK 26), libGDX 1.14.2 (`Gdx.net`, `Preferences`), JUnit 4 with the libGDX headless backend. Build via the Gradle wrapper (`gradlew.bat` on Windows). Tests run with `gradlew.bat desktop:test`.

---

## File Structure

- **Create:** `core/src/com/mdinic/game/giana/service/Clock.java` — a one-method time source interface (`long now()`), the testability seam for the UTC-day comparison. Patterned exactly on `EnvLookup`.
- **Modify:** `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java` — add the `K_TODAYS_DAY` key, a `Clock` field with a system-time default, a `Clock`-injecting test constructor, thread `clock.now()` through `fetch()` into `applyTodays(fresh, now)`, write the day stamp in `applyTodays`, make `goodForHighScores` stale-day-aware, and add the proactive `fetch()` in `init()`.
- **Modify (test):** `desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java` — **new** test class in the `service` package (so it can reach package-private members like the seam constructor, `applyTodays`, `K_TODAYS`, `K_TODAYS_DAY`). Covers the day-boundary gate logic and the stamp write.

The existing `SupabaseHighScoreServiceTest` (offline/construction) and `SupabaseHighScoreOutboxTest` (outbox) keep passing unchanged; the new daily test isolates the new behavior.

---

## Task 1: Add the `Clock` seam interface

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/Clock.java`

- [ ] **Step 1: Create the interface**

Create `core/src/com/mdinic/game/giana/service/Clock.java`:

```java
package com.mdinic.game.giana.service;

/**
 * Supplies the current wall-clock time in epoch milliseconds. Production wires
 * this to {@link System#currentTimeMillis()}; tests inject a fixed value so the
 * UTC-day comparison in {@link SupabaseHighScoreService#goodForHighScores(int)}
 * is deterministic. Kept as an interface (not a lambda target by convention —
 * this codebase uses anonymous classes), mirroring {@link EnvLookup}.
 */
public interface Clock {
    /** @return the current time in epoch milliseconds. */
    long now();
}
```

- [ ] **Step 2: Compile core to verify the new file parses**

Run: `gradlew.bat core:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/Clock.java
git commit -m "Add Clock seam for testable time in high-score service"
```

---

## Task 2: Wire the `Clock` field and key into `SupabaseHighScoreService`

This task adds the new state (the day-stamp key, the `Clock` field, the system-time default, and a `Clock`-injecting test constructor) WITHOUT yet changing any behavior. It keeps the build green and gives later tasks the seam they need.

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java`

- [ ] **Step 1: Add the `K_TODAYS_DAY` constant**

In `SupabaseHighScoreService.java`, the constants block currently reads:

```java
    static final String PREFS = "giana-highscores";
    static final String K_ALLTIME = "cache.alltime";
    static final String K_TODAYS = "cache.todays";
    static final String K_OUTBOX = "outbox";
```

Change it to add the new key directly after `K_TODAYS`:

```java
    static final String PREFS = "giana-highscores";
    static final String K_ALLTIME = "cache.alltime";
    static final String K_TODAYS = "cache.todays";
    /** UTC day (yyyy-MM-ddT00:00:00Z) the cached today's-board was fetched for. */
    static final String K_TODAYS_DAY = "cache.todays.day";
    static final String K_OUTBOX = "outbox";
```

- [ ] **Step 2: Add the `Clock` field with a system-time default**

The field declarations currently read:

```java
    SupabaseConfig config;
    private Preferences prefs;
    private boolean initialized;
    private final Object lock = new Object();
```

Change to add a `Clock` field initialized to system time:

```java
    SupabaseConfig config;
    private Preferences prefs;
    private boolean initialized;
    private final Object lock = new Object();

    /**
     * Time source for the UTC-day comparison. Defaults to system time; the
     * test-seam constructor can replace it so the day-boundary logic is
     * deterministic. Mirrors the {@link EnvLookup} seam.
     */
    private Clock clock = new Clock() {
        public long now() {
            return System.currentTimeMillis();
        }
    };
```

- [ ] **Step 3: Add a `Clock`-injecting test constructor**

The existing test-seam constructor reads:

```java
    /** Constructor seam for tests: injects config + prefs and initializes eagerly. */
    SupabaseHighScoreService(SupabaseConfig config, Preferences prefs) {
        this.config = config;
        this.prefs = prefs;
        init();
    }
```

Add a second seam constructor ABOVE it that also injects the clock, and have the
two-arg one delegate so existing callers keep working. Set the clock BEFORE
`init()` so any `init()`-time logic uses the injected time:

```java
    /** Constructor seam for tests: injects config + prefs + clock and initializes eagerly. */
    SupabaseHighScoreService(SupabaseConfig config, Preferences prefs, Clock clock) {
        this.config = config;
        this.prefs = prefs;
        this.clock = clock;
        init();
    }

    /** Constructor seam for tests: injects config + prefs and initializes eagerly. */
    SupabaseHighScoreService(SupabaseConfig config, Preferences prefs) {
        this(config, prefs, new Clock() {
            public long now() {
                return System.currentTimeMillis();
            }
        });
    }
```

- [ ] **Step 4: Compile core**

Run: `gradlew.bat core:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the existing service tests to confirm no regression**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.SupabaseHighScoreServiceTest" --tests "com.mdinic.game.giana.service.SupabaseHighScoreOutboxTest"`
Expected: `BUILD SUCCESSFUL`, all tests pass. (Behavior is unchanged so far; we only added unused state and a delegating constructor.)

- [ ] **Step 6: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java
git commit -m "Add today's-day stamp key and Clock seam to high-score service"
```

---

## Task 3: Stamp the today's-board day on fetch (`applyTodays`)

Thread the clock's `now` through `fetch()` into `applyTodays`, and have
`applyTodays` persist the UTC-day stamp next to the cached list. We write the
test first (it can assert the stamp via the injected `Preferences`).

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java:404-411` (`applyTodays`) and `:196-207` (the today's `getJson` block in `fetch`)
- Test: `desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java`

- [ ] **Step 1: Write the failing test for the stamp write**

Create `desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java`:

```java
package com.mdinic.game.giana.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Day-boundary behavior of {@link SupabaseHighScoreService}: the today's-board
 * day stamp written by {@code applyTodays}, and the stale/missing-stamp logic in
 * {@code goodForHighScores}. Lives in the {@code service} package so it can reach
 * the package-private seam constructor, {@code applyTodays}, and the cache keys.
 */
public class SupabaseHighScoreServiceDailyTest {

    private HeadlessApplication app;
    private Preferences prefs;

    private static final SupabaseConfig OFFLINE = new SupabaseConfig("", "", "", "");

    // 2026-06-29T12:00:00Z and the prior day, as epoch millis (UTC).
    // Derived from ScoreCodec.utcMidnightIso so the test and prod agree on "day".
    private static final long DAY_2026_06_29_NOON = 1782734400000L;
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long DAY_2026_06_28_NOON = DAY_2026_06_29_NOON - ONE_DAY_MILLIS;

    @Before
    public void setUp() {
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
        prefs = Gdx.app.getPreferences("giana-highscores-daily-test");
        prefs.clear();
        prefs.flush();
    }

    @After
    public void tearDown() {
        prefs.clear();
        prefs.flush();
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    private static Clock fixedClock(final long millis) {
        return new Clock() {
            public long now() {
                return millis;
            }
        };
    }

    private static List<Score> fullBoard() {
        List<Score> board = new ArrayList<Score>();
        for (int i = 0; i < 5; i++) {
            board.add(new Score("P" + i, 1000 + i, 1));
        }
        return board; // five entries, lowest is 1000
    }

    /** applyTodays must persist both the list and the UTC-day stamp for the clock's day. */
    @Test
    public void applyTodays_writesDayStamp() {
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));

        svc.applyTodays(fullBoard(), DAY_2026_06_29_NOON);

        assertEquals(ScoreCodec.utcMidnightIso(DAY_2026_06_29_NOON),
                prefs.getString(SupabaseHighScoreService.K_TODAYS_DAY, ""));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.service.SupabaseHighScoreServiceDailyTest"`
Expected: FAIL — `applyTodays(List, long)` does not exist yet (compile error), or, if only the one-arg form exists, a "cannot find symbol" / method-arity compile failure. This confirms the new two-arg signature is required.

- [ ] **Step 3: Change `applyTodays` to take `now` and write the stamp**

`applyTodays` currently reads:

```java
    void applyTodays(List<Score> fresh) {
        synchronized (lock) {
            todaysScores = fresh;
            haveTodaysUpdate = true;
        }
        prefs.putString(K_TODAYS, ScoreStore.scoresToJson(fresh));
        prefs.flush();
    }
```

Change it to:

```java
    void applyTodays(List<Score> fresh, long now) {
        synchronized (lock) {
            todaysScores = fresh;
            haveTodaysUpdate = true;
        }
        prefs.putString(K_TODAYS, ScoreStore.scoresToJson(fresh));
        prefs.putString(K_TODAYS_DAY, ScoreCodec.utcMidnightIso(now));
        prefs.flush();
    }
```

- [ ] **Step 4: Update the `fetch()` caller to pass the queried `now`**

In `fetch()`, the today's request currently reads:

```java
        getJson(ScoreCodec.todaysUrl(config.url, System.currentTimeMillis()), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                List<Score> fresh = ScoreCodec.parseScores(body);
                Gdx.app.debug("HighScore", "received " + fresh.size() + " todays scores");
                applyTodays(fresh);
            }

            public void fail() {
                setLastNetworkOk(false);
            }
        });
```

Capture a single `now` from the clock so the URL day and the stamped day match,
and pass it into `applyTodays`:

```java
        final long now = clock.now();
        getJson(ScoreCodec.todaysUrl(config.url, now), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                List<Score> fresh = ScoreCodec.parseScores(body);
                Gdx.app.debug("HighScore", "received " + fresh.size() + " todays scores");
                applyTodays(fresh, now);
            }

            public void fail() {
                setLastNetworkOk(false);
            }
        });
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.service.SupabaseHighScoreServiceDailyTest"`
Expected: PASS.

- [ ] **Step 6: Run the full service + outbox tests for no regression**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.SupabaseHighScoreServiceTest" --tests "com.mdinic.game.giana.service.SupabaseHighScoreOutboxTest"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java
git commit -m "Stamp today's-board with its UTC day on fetch"
```

---

## Task 4: Make `goodForHighScores` stale-day-aware

The gate must treat a missing or previous-day stamp as an empty board (any
positive score qualifies), and otherwise apply the existing logic.

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java:372-391` (`goodForHighScores`)
- Test: `desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java`

- [ ] **Step 1: Write the failing gate tests**

Add these four tests to `SupabaseHighScoreServiceDailyTest` (the helpers
`fixedClock`, `fullBoard`, the day constants, and `OFFLINE` already exist from
Task 3):

```java
    /** Same-day full board: a score below the lowest entry does NOT qualify. */
    @Test
    public void goodForHighScores_sameDayFullBoard_lowScore_false() {
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));
        svc.applyTodays(fullBoard(), DAY_2026_06_29_NOON); // stamps today

        assertFalse(svc.goodForHighScores(10)); // 10 < lowest (1000)
    }

    /** Stale stamp (yesterday) with a full board: board treated empty, positive score qualifies. */
    @Test
    public void goodForHighScores_staleDayFullBoard_positiveScore_true() {
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));
        // Stamp the board for the PRIOR day, then advance "now" to today.
        svc.applyTodays(fullBoard(), DAY_2026_06_28_NOON);

        assertTrue(svc.goodForHighScores(10)); // stale day => empty board => any score>0 qualifies
    }

    /** Missing stamp (existing install / never fetched): positive score qualifies. */
    @Test
    public void goodForHighScores_missingStamp_positiveScore_true() {
        // Seed a full cached board directly, with NO day stamp.
        prefs.putString(SupabaseHighScoreService.K_TODAYS, ScoreStore.scoresToJson(fullBoard()));
        prefs.flush();
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));

        assertTrue(svc.goodForHighScores(10)); // no stamp => stale => empty board
    }

    /** A zero score never qualifies, regardless of stamp. */
    @Test
    public void goodForHighScores_zeroScore_false_evenWhenStale() {
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));
        // No applyTodays => no stamp => stale, but a zero score still must not qualify.
        assertFalse(svc.goodForHighScores(0));
    }
```

- [ ] **Step 2: Run the new tests to verify they fail**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.service.SupabaseHighScoreServiceDailyTest"`
Expected: FAIL — `goodForHighScores_staleDayFullBoard_positiveScore_true` and `goodForHighScores_missingStamp_positiveScore_true` fail (the current gate consults the full board and returns `false` for score 10). The same-day and zero-score tests should already pass against the old logic.

- [ ] **Step 3: Implement the stale-day gate**

`goodForHighScores` currently reads:

```java
    @Override
    public boolean goodForHighScores(int score) {
        ensureInit();
        synchronized (lock) {
            // A zero score never belongs on the board, even when the list has
            // free slots — otherwise dying immediately would still prompt for a
            // name on a near-empty leaderboard.
            if (score <= 0) {
                return false;
            }
            if (todaysScores.size() < LIMIT) {
                return true;
            }
            for (Score s : todaysScores) {
                if (s.getScore() < score) {
                    return true;
                }
            }
            return false;
        }
    }
```

Change it to treat a missing or previous-day stamp as an empty board. Read the
stamp BEFORE entering the lock (Preferences access is independent of the
`todaysScores` mutation lock):

```java
    @Override
    public boolean goodForHighScores(int score) {
        ensureInit();
        // A zero score never belongs on the board, even when the list has free
        // slots — otherwise dying immediately would still prompt for a name on a
        // near-empty leaderboard.
        if (score <= 0) {
            return false;
        }
        // If the cached today's-board is from a previous UTC day (or was never
        // stamped — e.g. an existing install, or before the first fetch), we
        // cannot trust it to represent today. Treat the board as empty so any
        // positive score prompts for a name; the server is the real judge of
        // whether the queued submit actually lands.
        String cachedDay = prefs.getString(K_TODAYS_DAY, "");
        String currentDay = ScoreCodec.utcMidnightIso(clock.now());
        if (!currentDay.equals(cachedDay)) {
            return true;
        }
        synchronized (lock) {
            if (todaysScores.size() < LIMIT) {
                return true;
            }
            for (Score s : todaysScores) {
                if (s.getScore() < score) {
                    return true;
                }
            }
            return false;
        }
    }
```

- [ ] **Step 4: Run the daily tests to verify they pass**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.service.SupabaseHighScoreServiceDailyTest"`
Expected: PASS (all stamp + gate tests green).

- [ ] **Step 5: Run the existing service test (guards the offline default path)**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.SupabaseHighScoreServiceTest"`
Expected: `BUILD SUCCESSFUL`. Note `worksAfterGdxRestored_offlineMode` asserts `goodForHighScores(10)` is `true` and `goodForHighScores(0)` is `false`. With no cache and no stamp, the new logic returns `true` for 10 (stale/missing stamp) and `false` for 0 — consistent.

- [ ] **Step 6: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java desktop/test/com/mdinic/game/giana/service/SupabaseHighScoreServiceDailyTest.java
git commit -m "Treat stale/missing today's-board stamp as empty in goodForHighScores"
```

---

## Task 5: Proactive today's-board fetch at service init

Refresh the today's board at init so, online, the in-memory board is fresh well
before a death — without duplicating the fetch that an in-flight outbox submit
already triggers.

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java:86-91` (`init`)

- [ ] **Step 1: Add the proactive fetch in `init()`**

`init()` currently reads:

```java
    /** Loads cached lists and flushes the outbox. Requires config + prefs set. */
    private void init() {
        scores = ScoreStore.scoresFromJson(prefs.getString(K_ALLTIME, ""));
        todaysScores = ScoreStore.scoresFromJson(prefs.getString(K_TODAYS, ""));
        initialized = true;
        flushOutbox();
    }
```

Change it to fetch when no submit is in flight. `flushOutbox()` returns `true`
when a submit-score POST is in flight — and that POST's success callback already
re-fetches the leaderboards after the insert lands, so fetching again here would
duplicate it. Only fetch when `flushOutbox()` returns `false` (queue empty,
offline, or only poison entries dropped):

```java
    /** Loads cached lists, flushes the outbox, and proactively refreshes the board. */
    private void init() {
        scores = ScoreStore.scoresFromJson(prefs.getString(K_ALLTIME, ""));
        todaysScores = ScoreStore.scoresFromJson(prefs.getString(K_TODAYS, ""));
        initialized = true;
        // Refresh the leaderboards at launch so the qualification gate sees a
        // current-day board well before any death. If flushOutbox() started (or
        // found) a submit in flight it returns true; that POST's callback already
        // re-fetches after the insert, so we must NOT fetch again here. Only
        // fetch when no submit will run (offline, empty queue, or drained).
        if (!flushOutbox()) {
            fetch();
        }
    }
```

- [ ] **Step 2: Compile core**

Run: `gradlew.bat core:compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run the full high-score test suite**

Run: `gradlew.bat desktop:test --tests "com.mdinic.game.giana.SupabaseHighScoreServiceTest" --tests "com.mdinic.game.giana.service.SupabaseHighScoreOutboxTest" --tests "com.mdinic.game.giana.service.SupabaseHighScoreServiceDailyTest"`
Expected: `BUILD SUCCESSFUL`. The headless tests have no real network, so `fetch()` for the `OFFLINE`/no-config services is a no-op (config not configured) and for `ONLINE` services it fires async requests that fail without affecting the synchronous assertions. The outbox tests must still see the same outbox state (a no-config or no-network `fetch()` does not touch the outbox).

- [ ] **Step 4: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java
git commit -m "Proactively refresh leaderboards at high-score service init"
```

---

## Task 6: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the entire test suite**

Run: `gradlew.bat desktop:test`
Expected: `BUILD SUCCESSFUL`. This includes `MapTest` (level smoke test) plus all high-score tests.

- [ ] **Step 2: Compile the whole project (core + desktop + ensure nothing else broke)**

Run: `gradlew.bat build -x test`
Expected: `BUILD SUCCESSFUL`. (Note: the `android` module requires the Android SDK + a JDK 17–21 toolchain per CLAUDE.md; if that toolchain is unavailable in this environment, run `gradlew.bat core:build desktop:build -x test` instead and note that android was not built.)

- [ ] **Step 3: Sanity-run the desktop game (optional manual check)**

Run: `gradlew.bat desktop:run`
Expected: the game launches. There is no easy way to force a UTC day rollover at runtime, so the day-boundary behavior is covered by the unit tests in Tasks 3–4; this step only confirms nothing regressed in normal startup and high-score display.

- [ ] **Step 4: Final commit if any verification fixups were needed**

Only if Steps 1–2 surfaced a fix:

```bash
git add -A
git commit -m "Fix issues found during verification"
```

---

## Self-Review Notes (for the implementer)

- **Spec coverage:** stale-day stamp (Tasks 2–3), stale/missing-stamp gate incl. existing-install path (Task 4), proactive refresh without duplicate fetch (Task 5), `Clock` testability seam (Tasks 1–2), no public-interface or gate-call-site changes (none touched — `goodForHighScores(int)` keeps its signature). UTC day stays via `ScoreCodec.utcMidnightIso`.
- **Constant for the test epoch:** `DAY_2026_06_29_NOON = 1782734400000L` is 2026-06-29T12:00:00Z (verified). The tests compare against `ScoreCodec.utcMidnightIso(...)` of the same millis rather than hardcoding the ISO string, so the test stays internally consistent (it checks "stamp written equals utcMidnightIso(now)" and "stale day ≠ current day"). The only requirement is that `DAY_2026_06_28_NOON` and `DAY_2026_06_29_NOON` fall on different UTC days, which subtracting a full 24h guarantees for a noon anchor.
- **Method signature consistency:** `applyTodays(List<Score>, long)` is defined in Task 3 and only ever called with `(fresh, now)`; `goodForHighScores(int)` is unchanged in signature.
