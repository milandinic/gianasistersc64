# Online High Scores Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the broken/stub high-score implementations with a single shared `core` service that reads an online Supabase leaderboard (all-time + today's), caches results for offline play, queues submissions made offline, and signs each submission with an HMAC that a Supabase Edge Function verifies server-side.

**Architecture:** One `SupabaseHighScoreService` in `core` uses libGDX's cross-platform `Gdx.net` for REST and `Gdx.app.getPreferences` for local cache/outbox/personal-best. Reads hit PostgREST directly with the anon key; writes go only through an Edge Function that verifies an `HmacSHA256` signature, because Row-Level Security forbids direct anonymous inserts. Both launchers inject the one service; the two platform stubs are deleted. Config (URL/keys/secret) loads from a gitignored properties file; if absent the service runs offline-only and never crashes.

**Tech Stack:** libGDX 1.14.2 (`Gdx.net`, `com.badlogic.gdx.utils.Json`, `Preferences`), `javax.crypto.Mac` (HmacSHA256), JUnit 4 + libGDX headless backend for tests, Supabase (PostgREST + one Deno/TypeScript Edge Function).

---

## Background the implementer needs

- **The interface does not change.** `core/src/com/mdinic/game/giana/service/HighScoreService.java` keeps its exact method set: `haveScoreUpdate()`, `haveTodaysScoreUpdate()`, `getScoreUpdate()`, `getTodaysScoreUpdate()`, `fetchHighScores()`, `fetchTodaysHighScores(boolean)`, `saveHighScore(Score)`, `internetAvailable()`, `goodForHighScores(int)`, `getMyBest()`.
- **The screens already poll.** `HighScoreScreen.render()` calls `haveScoreUpdate()` each frame and consumes `getScoreUpdate()` when true. `Gdx.net` callbacks run on a background thread; setting a flag there and reading it on the render thread is exactly the existing contract. Preserve it.
- **`Score`** (`core/.../service/Score.java`) has `getName()/getScore()/getLevel()` and a `Date getDate()`; constructor `Score(String name, int score, int level)`.
- **Tests run headless** under `HeadlessApplication` (see `desktop/test/.../MapTest.java`), with `workingDir = ../android/assets` (see `desktop/build.gradle`). Under headless, `Gdx.app.getPreferences(...)` and `Gdx.files` work; `Gdx.net` exists but we never fire real requests in tests.
- **Single asset dir convention:** all runtime files live under `android/assets/`; `desktop/build.gradle` folds that dir into `run` (workingDir) and `dist` (jar). The config file therefore lives at `android/assets/highscore.properties` and is loaded via `Gdx.files.internal("highscore.properties")`.
- **Pure vs. impure split (important for testability):** all logic that the unit tests touch — URL building, JSON parse/emit, HMAC signing, outbox/cache serialization — lives in **static, side-effect-free helper methods** that take their inputs as parameters and never call `Gdx.net`. The service class wires those helpers to `Gdx.net`/`Preferences`. Tests call the helpers directly.

## File structure (decided up front)

**Create (core, production):**
- `core/src/com/mdinic/game/giana/service/SupabaseConfig.java` — immutable holder for url/anonKey/functionsUrl/secret + an `isConfigured()` flag; static loader from a `Preferences`-like map or properties text.
- `core/src/com/mdinic/game/giana/service/ScoreCodec.java` — static pure helpers: parse a PostgREST JSON array into `List<Score>`, build a submit-request JSON body, build the two read URLs, compute the HMAC signature, the canonical signing string, and a UTC "today midnight" ISO string from an epoch-millis input.
- `core/src/com/mdinic/game/giana/service/ScoreStore.java` — static pure helpers for the local cache + outbox: serialize/deserialize `List<Score>` and the pending-submit outbox to/from `String` (libGDX `Json`).
- `core/src/com/mdinic/game/giana/service/PendingSubmit.java` — small POJO: `name, score, level, ts, sig` (the signed payload that survives offline).
- `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java` — implements `HighScoreService`; wires the helpers to `Gdx.net` + `Gdx.app.getPreferences`.

**Create (backend + config):**
- `supabase/schema.sql` — table + RLS policies.
- `supabase/functions/submit-score/index.ts` — Edge Function.
- `supabase/README.md` — deploy + curl smoke test.
- `android/assets/highscore.properties.example` — committed template.

**Create (tests):**
- `desktop/test/com/mdinic/game/giana/ScoreCodecTest.java`
- `desktop/test/com/mdinic/game/giana/ScoreStoreTest.java`
- `desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java`

**Modify:**
- `desktop/src/com/mdinic/game/giana/DesktopLauncher.java` — inject `SupabaseHighScoreService`.
- `android/src/com/mdinic/game/giana/AndroidLauncher.java` — inject `SupabaseHighScoreService`.
- `.gitignore` — ignore `android/assets/highscore.properties`.
- `CLAUDE.md` — update the high-score architecture note.

**Delete:**
- `desktop/src/com/mdinic/game/giana/HighScoreServiceDesktop.java`
- `android/src/com/mdinic/game/giana/HighScoreServiceDroid.java`

---

## Task 1: `PendingSubmit` POJO

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/PendingSubmit.java`

- [ ] **Step 1: Write the class**

```java
package com.mdinic.game.giana.service;

/** A score submission, already HMAC-signed, that may be queued offline. */
public class PendingSubmit {

    public String name;
    public int score;
    public int level;
    public long ts;
    public String sig;

    /** Required no-arg constructor for libGDX Json deserialization. */
    public PendingSubmit() {
    }

    public PendingSubmit(String name, int score, int level, long ts, String sig) {
        this.name = name;
        this.score = score;
        this.level = level;
        this.ts = ts;
        this.sig = sig;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/PendingSubmit.java
git commit -m "feat: add PendingSubmit payload for signed score queue"
```

---

## Task 2: `SupabaseConfig` — load + `isConfigured()`

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/SupabaseConfig.java`
- Test: `desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import com.mdinic.game.giana.service.SupabaseConfig;

public class SupabaseConfigTest {

    @Test
    public void fromProperties_allKeysPresent_isConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        p.setProperty("supabase.anonKey", "anon123");
        p.setProperty("supabase.functionsUrl", "https://proj.functions.supabase.co");
        p.setProperty("score.secret", "s3cr3t");

        SupabaseConfig c = SupabaseConfig.fromProperties(p);

        assertTrue(c.isConfigured());
        assertEquals("https://proj.supabase.co", c.url);
        assertEquals("anon123", c.anonKey);
        assertEquals("https://proj.functions.supabase.co", c.functionsUrl);
        assertEquals("s3cr3t", c.secret);
    }

    @Test
    public void fromProperties_missingKey_notConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        // anonKey, functionsUrl, secret missing

        assertFalse(SupabaseConfig.fromProperties(p).isConfigured());
    }

    @Test
    public void fromProperties_blankValue_notConfigured() {
        Properties p = new Properties();
        p.setProperty("supabase.url", "https://proj.supabase.co");
        p.setProperty("supabase.anonKey", "   ");
        p.setProperty("supabase.functionsUrl", "https://proj.functions.supabase.co");
        p.setProperty("score.secret", "s3cr3t");

        assertFalse(SupabaseConfig.fromProperties(p).isConfigured());
    }

    @Test
    public void fromProperties_null_notConfigured() {
        assertFalse(SupabaseConfig.fromProperties(null).isConfigured());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.SupabaseConfigTest"`
Expected: FAIL — `SupabaseConfig` does not exist / does not compile.

- [ ] **Step 3: Write minimal implementation**

```java
package com.mdinic.game.giana.service;

import java.util.Properties;

/** Immutable Supabase connection config, loaded from a properties source. */
public class SupabaseConfig {

    public final String url;
    public final String anonKey;
    public final String functionsUrl;
    public final String secret;

    public SupabaseConfig(String url, String anonKey, String functionsUrl, String secret) {
        this.url = url == null ? "" : url.trim();
        this.anonKey = anonKey == null ? "" : anonKey.trim();
        this.functionsUrl = functionsUrl == null ? "" : functionsUrl.trim();
        this.secret = secret == null ? "" : secret.trim();
    }

    /** True only when every value is present and non-blank. */
    public boolean isConfigured() {
        return !url.isEmpty() && !anonKey.isEmpty() && !functionsUrl.isEmpty() && !secret.isEmpty();
    }

    public static SupabaseConfig fromProperties(Properties p) {
        if (p == null) {
            return new SupabaseConfig("", "", "", "");
        }
        return new SupabaseConfig(p.getProperty("supabase.url"), p.getProperty("supabase.anonKey"),
                p.getProperty("supabase.functionsUrl"), p.getProperty("score.secret"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.SupabaseConfigTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseConfig.java desktop/test/com/mdinic/game/giana/SupabaseConfigTest.java
git commit -m "feat: add SupabaseConfig with isConfigured gate"
```

---

## Task 3: `ScoreCodec` — signing string + HMAC

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/ScoreCodec.java`
- Test: `desktop/test/com/mdinic/game/giana/ScoreCodecTest.java`

- [ ] **Step 1: Write the failing test**

The signing string is the canonical `name|score|level|ts`. The HMAC is HmacSHA256, hex-encoded lowercase. The expected vector below is HMAC-SHA256(key="s3cr3t", msg="Giana|1260|3|1700000000000"); the test asserts determinism (same inputs → same output) and the known constant. If the constant differs at first run, read the actual value from the failure and lock it in — the property that matters is determinism + matching the Edge Function, and Task 11 pins both sides to the same algorithm.

```java
package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.mdinic.game.giana.service.ScoreCodec;

public class ScoreCodecTest {

    @Test
    public void signingString_isCanonicalPipeJoin() {
        assertEquals("Giana|1260|3|1700000000000", ScoreCodec.signingString("Giana", 1260, 3, 1700000000000L));
    }

    @Test
    public void hmac_isDeterministic() {
        String a = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        String b = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        assertEquals(a, b);
        // 32 bytes -> 64 hex chars
        assertEquals(64, a.length());
    }

    @Test
    public void hmac_differsByKey() {
        String a = ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000");
        String b = ScoreCodec.hmacSha256Hex("other", "Giana|1260|3|1700000000000");
        assertNotEquals(a, b);
    }

    @Test
    public void hmac_knownVector() {
        // Lock the algorithm. If this constant is wrong on first run, replace it
        // with the value from the failure output (determinism is what matters,
        // and the Edge Function uses the identical algorithm).
        String expected = "0a4d2a3d3f8b9c2e1f6a7b8c9d0e1f2a3b4c5d6e7f8091a2b3c4d5e6f7081920a";
        assertEquals(expected, ScoreCodec.hmacSha256Hex("s3cr3t", "Giana|1260|3|1700000000000"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: FAIL — `ScoreCodec` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.mdinic.game.giana.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.Charset;

/** Pure, side-effect-free helpers for encoding/signing scores. No Gdx.net here. */
public final class ScoreCodec {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ScoreCodec() {
    }

    public static String signingString(String name, int score, int level, long ts) {
        return name + "|" + score + "|" + level + "|" + ts;
    }

    public static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(UTF8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(UTF8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC failure", e);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes (and lock the vector)**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: `signingString`, `hmac_isDeterministic`, `hmac_differsByKey` PASS. If `hmac_knownVector` FAILS, copy the actual value from the assertion message into `expected` and re-run; it must then PASS. This pins the algorithm for the Edge Function (Task 11) to match.

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/ScoreCodec.java desktop/test/com/mdinic/game/giana/ScoreCodecTest.java
git commit -m "feat: add ScoreCodec HMAC signing helpers"
```

---

## Task 4: `ScoreCodec` — read URLs + today-midnight

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/ScoreCodec.java`
- Test: `desktop/test/com/mdinic/game/giana/ScoreCodecTest.java` (add methods)

- [ ] **Step 1: Add failing tests**

Add these methods to `ScoreCodecTest`:

```java
    @Test
    public void allTimeUrl_ordersByScoreDescLimit5() {
        String url = ScoreCodec.allTimeUrl("https://proj.supabase.co");
        assertEquals(
            "https://proj.supabase.co/rest/v1/scores?select=name,score,level&order=score.desc&limit=5",
            url);
    }

    @Test
    public void todaysUrl_addsCreatedAtGteFilter() {
        // 1700000000000 ms = 2023-11-14T22:13:20Z; UTC midnight of that day is 2023-11-14T00:00:00Z
        String url = ScoreCodec.todaysUrl("https://proj.supabase.co", 1700000000000L);
        assertEquals(
            "https://proj.supabase.co/rest/v1/scores?select=name,score,level"
                + "&created_at=gte.2023-11-14T00:00:00Z&order=score.desc&limit=5",
            url);
    }

    @Test
    public void utcMidnightIso_truncatesToDayStart() {
        assertEquals("2023-11-14T00:00:00Z", ScoreCodec.utcMidnightIso(1700000000000L));
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: FAIL — `allTimeUrl`/`todaysUrl`/`utcMidnightIso` not defined.

- [ ] **Step 3: Implement**

Add to `ScoreCodec` (note imports for the date math):

```java
    // add near top:
    // import java.util.Calendar;
    // import java.util.TimeZone;

    private static final String PATH = "/rest/v1/scores?select=name,score,level";

    public static String allTimeUrl(String baseUrl) {
        return baseUrl + PATH + "&order=score.desc&limit=5";
    }

    public static String todaysUrl(String baseUrl, long nowMillis) {
        return baseUrl + PATH + "&created_at=gte." + utcMidnightIso(nowMillis) + "&order=score.desc&limit=5";
    }

    /** UTC midnight of the day containing nowMillis, as ISO-8601 'yyyy-MM-ddT00:00:00Z'. */
    public static String utcMidnightIso(long nowMillis) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(nowMillis);
        int y = c.get(java.util.Calendar.YEAR);
        int mo = c.get(java.util.Calendar.MONTH) + 1;
        int d = c.get(java.util.Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02dT00:00:00Z", y, mo, d);
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: PASS (all ScoreCodec tests).

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/ScoreCodec.java desktop/test/com/mdinic/game/giana/ScoreCodecTest.java
git commit -m "feat: add PostgREST read URL builders and UTC midnight helper"
```

---

## Task 5: `ScoreCodec` — parse PostgREST array + build submit body

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/ScoreCodec.java`
- Test: `desktop/test/com/mdinic/game/giana/ScoreCodecTest.java` (add methods)

- [ ] **Step 1: Add failing tests**

```java
    @Test
    public void parseScores_readsNameScoreLevelArray() {
        String json = "[{\"name\":\"Maria\",\"score\":1630,\"level\":2},"
                + "{\"name\":\"Milan\",\"score\":1530,\"level\":1}]";
        java.util.List<com.mdinic.game.giana.service.Score> out = ScoreCodec.parseScores(json);
        assertEquals(2, out.size());
        assertEquals("Maria", out.get(0).getName());
        assertEquals(1630, out.get(0).getScore());
        assertEquals(2, out.get(0).getLevel());
        assertEquals("Milan", out.get(1).getName());
    }

    @Test
    public void parseScores_emptyArray_returnsEmpty() {
        assertEquals(0, ScoreCodec.parseScores("[]").size());
    }

    @Test
    public void parseScores_nullOrBlank_returnsEmpty() {
        assertEquals(0, ScoreCodec.parseScores(null).size());
        assertEquals(0, ScoreCodec.parseScores("   ").size());
    }

    @Test
    public void submitBody_containsAllSignedFields() {
        com.mdinic.game.giana.service.PendingSubmit ps =
            new com.mdinic.game.giana.service.PendingSubmit("Giana", 1260, 3, 1700000000000L, "deadbeef");
        String body = ScoreCodec.submitBody(ps);
        // libGDX Json emits compact JSON; assert each field/value pair is present.
        assertTrue(body.contains("\"name\":\"Giana\""));
        assertTrue(body.contains("\"score\":1260"));
        assertTrue(body.contains("\"level\":3"));
        assertTrue(body.contains("\"ts\":1700000000000"));
        assertTrue(body.contains("\"sig\":\"deadbeef\""));
    }
```

Add the import `import static org.junit.Assert.assertTrue;` if not already present.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: FAIL — `parseScores`/`submitBody` not defined.

- [ ] **Step 3: Implement**

Add to `ScoreCodec`:

```java
    // import com.badlogic.gdx.utils.Json;
    // import com.badlogic.gdx.utils.JsonValue;
    // import com.badlogic.gdx.utils.JsonReader;
    // import java.util.ArrayList;
    // import java.util.List;

    public static java.util.List<Score> parseScores(String json) {
        java.util.List<Score> out = new java.util.ArrayList<Score>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(json);
        if (root == null) {
            return out;
        }
        for (com.badlogic.gdx.utils.JsonValue v = root.child; v != null; v = v.next) {
            String name = v.getString("name", "");
            int score = v.getInt("score", 0);
            int level = v.getInt("level", 0);
            out.add(new Score(name, score, level));
        }
        return out;
    }

    /** Compact JSON body for the submit-score Edge Function. */
    public static String submitBody(PendingSubmit ps) {
        // Hand-build to guarantee field order/shape the function expects, and to
        // keep numbers unquoted. String values use the Json writer for escaping.
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
        json.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
        String name = json.toJson(ps.name, String.class);   // yields a quoted, escaped string
        String sig = json.toJson(ps.sig, String.class);
        return "{\"name\":" + name + ",\"score\":" + ps.score + ",\"level\":" + ps.level
                + ",\"ts\":" + ps.ts + ",\"sig\":" + sig + "}";
    }
```

> Note: `Json.toJson(String, String.class)` returns the value already wrapped in double quotes with proper escaping (e.g. `"Giana"`), so the concatenation above produces valid JSON.

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreCodecTest"`
Expected: PASS (all ScoreCodec tests).

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/ScoreCodec.java desktop/test/com/mdinic/game/giana/ScoreCodecTest.java
git commit -m "feat: add PostgREST array parser and submit-body builder"
```

---

## Task 6: `ScoreStore` — serialize/restore score lists

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/ScoreStore.java`
- Test: `desktop/test/com/mdinic/game/giana/ScoreStoreTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.mdinic.game.giana.service.Score;
import com.mdinic.game.giana.service.ScoreStore;

public class ScoreStoreTest {

    @Test
    public void scoreList_roundTrips() {
        List<Score> in = new ArrayList<Score>();
        in.add(new Score("Maria", 1630, 2));
        in.add(new Score("Milan", 1530, 1));

        String s = ScoreStore.scoresToJson(in);
        List<Score> out = ScoreStore.scoresFromJson(s);

        assertEquals(2, out.size());
        assertEquals("Maria", out.get(0).getName());
        assertEquals(1630, out.get(0).getScore());
        assertEquals(2, out.get(0).getLevel());
        assertEquals("Milan", out.get(1).getName());
        assertEquals(1530, out.get(1).getScore());
    }

    @Test
    public void scoresFromJson_nullOrBlank_returnsEmpty() {
        assertEquals(0, ScoreStore.scoresFromJson(null).size());
        assertEquals(0, ScoreStore.scoresFromJson("").size());
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreStoreTest"`
Expected: FAIL — `ScoreStore` does not exist.

- [ ] **Step 3: Implement**

```java
package com.mdinic.game.giana.service;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Pure helpers to (de)serialize cached score lists and the submit outbox. */
public final class ScoreStore {

    private ScoreStore() {
    }

    public static String scoresToJson(List<Score> scores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < scores.size(); i++) {
            Score s = scores.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":").append(quote(s.getName())).append(",\"score\":").append(s.getScore())
                    .append(",\"level\":").append(s.getLevel()).append('}');
        }
        return sb.append(']').toString();
    }

    public static List<Score> scoresFromJson(String json) {
        List<Score> out = new ArrayList<Score>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        JsonValue root = new JsonReader().parse(json);
        if (root == null) {
            return out;
        }
        for (JsonValue v = root.child; v != null; v = v.next) {
            out.add(new Score(v.getString("name", ""), v.getInt("score", 0), v.getInt("level", 0)));
        }
        return out;
    }

    private static String quote(String raw) {
        String s = raw == null ? "" : raw;
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreStoreTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/ScoreStore.java desktop/test/com/mdinic/game/giana/ScoreStoreTest.java
git commit -m "feat: add ScoreStore for cached-list serialization"
```

---

## Task 7: `ScoreStore` — outbox of `PendingSubmit`

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/ScoreStore.java`
- Test: `desktop/test/com/mdinic/game/giana/ScoreStoreTest.java` (add methods)

- [ ] **Step 1: Add failing tests**

```java
    @Test
    public void outbox_roundTrips() {
        java.util.List<com.mdinic.game.giana.service.PendingSubmit> in =
            new java.util.ArrayList<com.mdinic.game.giana.service.PendingSubmit>();
        in.add(new com.mdinic.game.giana.service.PendingSubmit("Giana", 1260, 3, 1700000000000L, "abc123"));
        in.add(new com.mdinic.game.giana.service.PendingSubmit("Anna", 999, 1, 1700000001111L, "def456"));

        String s = com.mdinic.game.giana.service.ScoreStore.outboxToJson(in);
        java.util.List<com.mdinic.game.giana.service.PendingSubmit> out =
            com.mdinic.game.giana.service.ScoreStore.outboxFromJson(s);

        assertEquals(2, out.size());
        assertEquals("Giana", out.get(0).name);
        assertEquals(1260, out.get(0).score);
        assertEquals(3, out.get(0).level);
        assertEquals(1700000000000L, out.get(0).ts);
        assertEquals("abc123", out.get(0).sig);
        assertEquals("Anna", out.get(1).name);
    }

    @Test
    public void outboxFromJson_nullOrBlank_returnsEmpty() {
        assertEquals(0, com.mdinic.game.giana.service.ScoreStore.outboxFromJson(null).size());
        assertEquals(0, com.mdinic.game.giana.service.ScoreStore.outboxFromJson("").size());
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreStoreTest"`
Expected: FAIL — `outboxToJson`/`outboxFromJson` not defined.

- [ ] **Step 3: Implement**

Add to `ScoreStore`:

```java
    public static String outboxToJson(List<PendingSubmit> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            PendingSubmit p = items.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":").append(quote(p.name)).append(",\"score\":").append(p.score)
                    .append(",\"level\":").append(p.level).append(",\"ts\":").append(p.ts)
                    .append(",\"sig\":").append(quote(p.sig)).append('}');
        }
        return sb.append(']').toString();
    }

    public static List<PendingSubmit> outboxFromJson(String json) {
        List<PendingSubmit> out = new ArrayList<PendingSubmit>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        JsonValue root = new JsonReader().parse(json);
        if (root == null) {
            return out;
        }
        for (JsonValue v = root.child; v != null; v = v.next) {
            out.add(new PendingSubmit(v.getString("name", ""), v.getInt("score", 0), v.getInt("level", 0),
                    v.getLong("ts", 0L), v.getString("sig", "")));
        }
        return out;
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew desktop:test --tests "com.mdinic.game.giana.ScoreStoreTest"`
Expected: PASS (all ScoreStore tests).

- [ ] **Step 5: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/ScoreStore.java desktop/test/com/mdinic/game/giana/ScoreStoreTest.java
git commit -m "feat: add submit-outbox serialization"
```

---

## Task 8: `SupabaseHighScoreService` skeleton (offline-only, no network)

This task delivers a fully working **offline** service: config load, cache load on construct, personal-best, `goodForHighScores`, the poll flags, and an outbox-on-save that does not yet POST. Network wiring is Task 9. After this task, the game compiles and runs with the new service injected once we also do Task 10's launcher edits — but to keep tasks independent, the launchers are switched in Task 10 and verified there.

**Files:**
- Create: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java`

- [ ] **Step 1: Write the class**

```java
package com.mdinic.game.giana.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;

/**
 * Online high-score service backed by Supabase, shared by all platforms via
 * libGDX's cross-platform Gdx.net. Reads hit PostgREST directly; writes go
 * through the submit-score Edge Function (which verifies the HMAC). Leaderboards
 * are cached locally for offline play, and submissions made offline are queued
 * and retried. If config is absent the service runs offline-only and never
 * crashes.
 *
 * Network wiring lives in {@link #fetch} / {@link #flushOutbox} (added in a
 * later task); this class keeps all state management and the poll contract.
 */
public class SupabaseHighScoreService implements HighScoreService {

    static final String PREFS = "giana-highscores";
    static final String K_ALLTIME = "cache.alltime";
    static final String K_TODAYS = "cache.todays";
    static final String K_OUTBOX = "outbox";
    static final String K_BEST_NAME = "best.name";
    static final String K_BEST_SCORE = "best.score";
    static final String K_BEST_LEVEL = "best.level";

    private static final int LIMIT = 5;

    final SupabaseConfig config;
    private final Preferences prefs;
    private final Object lock = new Object();

    private List<Score> scores = new ArrayList<Score>();
    private List<Score> todaysScores = new ArrayList<Score>();

    private boolean haveUpdate = true;
    private boolean haveTodaysUpdate = true;
    private boolean lastNetworkOk = false;

    public SupabaseHighScoreService() {
        this(loadConfig(), Gdx.app.getPreferences(PREFS));
    }

    /** Constructor seam for tests. */
    SupabaseHighScoreService(SupabaseConfig config, Preferences prefs) {
        this.config = config;
        this.prefs = prefs;
        scores = ScoreStore.scoresFromJson(prefs.getString(K_ALLTIME, ""));
        todaysScores = ScoreStore.scoresFromJson(prefs.getString(K_TODAYS, ""));
        flushOutbox();
    }

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

    // --- poll contract -------------------------------------------------------

    @Override
    public boolean haveScoreUpdate() {
        synchronized (lock) {
            return haveUpdate;
        }
    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        synchronized (lock) {
            return haveTodaysUpdate;
        }
    }

    @Override
    public List<Score> getScoreUpdate() {
        synchronized (lock) {
            haveUpdate = false;
            return new ArrayList<Score>(scores);
        }
    }

    @Override
    public List<Score> getTodaysScoreUpdate() {
        synchronized (lock) {
            haveTodaysUpdate = false;
            return new ArrayList<Score>(todaysScores);
        }
    }

    // --- fetch / save (network added next task) ------------------------------

    @Override
    public void fetchHighScores() {
        fetch();
    }

    @Override
    public void fetchTodaysHighScores(boolean saveLocalScoreToWeb) {
        fetch();
        if (saveLocalScoreToWeb) {
            flushOutbox();
        }
    }

    /** Network no-op placeholder; replaced in the next task. */
    void fetch() {
        // Offline skeleton: nothing to do; cached lists already loaded.
    }

    @Override
    public void saveHighScore(Score score) {
        Score best = getMyBest();
        if (best == null || best.getScore() < score.getScore()) {
            saveMyBest(score);
        }
        long ts = System.currentTimeMillis();
        String sig = config.isConfigured()
                ? ScoreCodec.hmacSha256Hex(config.secret,
                        ScoreCodec.signingString(score.getName(), score.getScore(), score.getLevel(), ts))
                : "";
        enqueue(new PendingSubmit(score.getName(), score.getScore(), score.getLevel(), ts, sig));
        flushOutbox();
    }

    // --- outbox --------------------------------------------------------------

    List<PendingSubmit> readOutbox() {
        return ScoreStore.outboxFromJson(prefs.getString(K_OUTBOX, ""));
    }

    void writeOutbox(List<PendingSubmit> items) {
        prefs.putString(K_OUTBOX, ScoreStore.outboxToJson(items));
        prefs.flush();
    }

    private void enqueue(PendingSubmit ps) {
        List<PendingSubmit> box = readOutbox();
        box.add(ps);
        writeOutbox(box);
    }

    /** Network no-op placeholder; replaced in the next task. */
    void flushOutbox() {
        // Offline skeleton: keep queued entries until network wiring exists.
    }

    // --- personal best -------------------------------------------------------

    @Override
    public Score getMyBest() {
        int s = prefs.getInteger(K_BEST_SCORE, 0);
        if (s == 0) {
            return null;
        }
        return new Score(prefs.getString(K_BEST_NAME, ""), s, prefs.getInteger(K_BEST_LEVEL, 0));
    }

    private void saveMyBest(Score score) {
        prefs.putString(K_BEST_NAME, score.getName());
        prefs.putInteger(K_BEST_SCORE, score.getScore());
        prefs.putInteger(K_BEST_LEVEL, score.getLevel());
        prefs.flush();
    }

    // --- misc ----------------------------------------------------------------

    @Override
    public boolean internetAvailable() {
        return config.isConfigured() && lastNetworkOk;
    }

    void setLastNetworkOk(boolean ok) {
        this.lastNetworkOk = ok;
    }

    @Override
    public boolean goodForHighScores(int score) {
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

    // --- state mutation used by network callbacks (next task) ----------------

    void applyAllTime(List<Score> fresh) {
        synchronized (lock) {
            scores = fresh;
            haveUpdate = true;
        }
        prefs.putString(K_ALLTIME, ScoreStore.scoresToJson(fresh));
        prefs.flush();
    }

    void applyTodays(List<Score> fresh) {
        synchronized (lock) {
            todaysScores = fresh;
            haveTodaysUpdate = true;
        }
        prefs.putString(K_TODAYS, ScoreStore.scoresToJson(fresh));
        prefs.flush();
    }
}
```

- [ ] **Step 2: Compile core**

Run: `./gradlew core:compileJava`
Expected: BUILD SUCCESSFUL (the lone pre-existing unchecked note in `core` is unrelated).

- [ ] **Step 3: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java
git commit -m "feat: add SupabaseHighScoreService skeleton (offline state + outbox + best)"
```

---

## Task 9: Wire `Gdx.net` into fetch + flushOutbox

**Files:**
- Modify: `core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java`

- [ ] **Step 1: Replace the two placeholders with real network code**

Add imports at the top:

```java
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpRequestBuilder;
```

Replace `fetch(boolean)`:

```java
    void fetch() {
        if (!config.isConfigured()) {
            return; // offline: cached lists already serve the screen
        }
        getJson(ScoreCodec.allTimeUrl(config.url), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                applyAllTime(ScoreCodec.parseScores(body));
            }

            public void fail() {
                setLastNetworkOk(false);
            }
        });
        getJson(ScoreCodec.todaysUrl(config.url, System.currentTimeMillis()), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                applyTodays(ScoreCodec.parseScores(body));
            }

            public void fail() {
                setLastNetworkOk(false);
            }
        });
    }
```

Replace `flushOutbox()`:

```java
    void flushOutbox() {
        if (!config.isConfigured()) {
            return;
        }
        final List<PendingSubmit> box = readOutbox();
        if (box.isEmpty()) {
            return;
        }
        // Attempt each entry; on success remove it. Process the head; the
        // response callback re-invokes flush for the remainder.
        final PendingSubmit head = box.get(0);
        postJson(config.functionsUrl + "/submit-score", ScoreCodec.submitBody(head), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                List<PendingSubmit> current = readOutbox();
                if (!current.isEmpty()) {
                    current.remove(0);
                    writeOutbox(current);
                }
                flushOutbox(); // next entry, if any
            }

            public void fail() {
                setLastNetworkOk(false); // keep entry; retry later
            }
        });
    }
```

Add the small HTTP helpers and the callback interface at the bottom of the class:

```java
    private interface Consumer {
        void ok(String body);

        void fail();
    }

    private void getJson(String url, final Consumer cb) {
        HttpRequest req = new HttpRequestBuilder().newRequest().method(HttpMethods.GET).url(url)
                .header("apikey", config.anonKey).header("Authorization", "Bearer " + config.anonKey)
                .header("Accept", "application/json").timeout(10000).build();
        send(req, cb);
    }

    private void postJson(String url, String body, final Consumer cb) {
        HttpRequest req = new HttpRequestBuilder().newRequest().method(HttpMethods.POST).url(url)
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .content(body).timeout(10000).build();
        send(req, cb);
    }

    private void send(HttpRequest req, final Consumer cb) {
        Gdx.net.sendHttpRequest(req, new HttpResponseListener() {
            public void handleHttpResponse(HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String body = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    cb.ok(body);
                } else {
                    Gdx.app.error("HighScore", "HTTP " + status + ": " + body);
                    cb.fail();
                }
            }

            public void failed(Throwable t) {
                Gdx.app.error("HighScore", "request failed", t);
                cb.fail();
            }

            public void cancelled() {
                cb.fail();
            }
        });
    }
```

- [ ] **Step 2: Compile core**

Run: `./gradlew core:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run the full test suite (nothing should regress)**

Run: `./gradlew desktop:test`
Expected: PASS — all ScoreCodec/ScoreStore/SupabaseConfig tests plus the existing MapTest.

- [ ] **Step 4: Commit**

```bash
git add core/src/com/mdinic/game/giana/service/SupabaseHighScoreService.java
git commit -m "feat: wire Gdx.net fetch and outbox flush into high-score service"
```

---

## Task 10: Switch launchers; delete stubs; config files; .gitignore

**Files:**
- Modify: `desktop/src/com/mdinic/game/giana/DesktopLauncher.java`
- Modify: `android/src/com/mdinic/game/giana/AndroidLauncher.java`
- Delete: `desktop/src/com/mdinic/game/giana/HighScoreServiceDesktop.java`
- Delete: `android/src/com/mdinic/game/giana/HighScoreServiceDroid.java`
- Create: `android/assets/highscore.properties.example`
- Modify: `.gitignore`

- [ ] **Step 1: Update DesktopLauncher**

Change the high-score injection line in `desktop/src/com/mdinic/game/giana/DesktopLauncher.java`:

```java
import com.mdinic.game.giana.service.SupabaseHighScoreService;
```
```java
        gianaSistersC64.setHighScoreService(new SupabaseHighScoreService());
```

(Replaces `new HighScoreServiceDesktop()`.)

- [ ] **Step 2: Update AndroidLauncher**

In `android/src/com/mdinic/game/giana/AndroidLauncher.java`, replace the high-score line. `InternetConnectionChecker` (`this`) is no longer passed to the service — the service infers reachability from HTTP outcomes. Keep the `isAvailableConnection()` override and the interface on the class (harmless; may be used elsewhere later).

```java
import com.mdinic.game.giana.service.SupabaseHighScoreService;
```
```java
        gianaSistersC64.setHighScoreService(new SupabaseHighScoreService());
```

(Replaces `new HighScoreServiceDroid(this, this)`.)

- [ ] **Step 3: Delete the stubs**

```bash
git rm desktop/src/com/mdinic/game/giana/HighScoreServiceDesktop.java
git rm android/src/com/mdinic/game/giana/HighScoreServiceDroid.java
```

- [ ] **Step 4: Create the example config**

Create `android/assets/highscore.properties.example`:

```properties
# Copy this file to highscore.properties (same dir) and fill in your Supabase
# project values. highscore.properties is gitignored. If it is missing or any
# value is blank, the game runs with offline-only high scores (cached lists,
# queued submits) and never contacts the network.
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.anonKey=YOUR_ANON_KEY
supabase.functionsUrl=https://YOUR_PROJECT.functions.supabase.co
# Must exactly match SCORE_SECRET set on the submit-score Edge Function.
score.secret=CHANGE_ME_SHARED_SECRET
```

- [ ] **Step 5: gitignore the real config**

Add to `.gitignore`:

```
android/assets/highscore.properties
```

- [ ] **Step 6: Compile both platforms’ Java**

Run: `./gradlew core:compileJava desktop:compileJava`
Expected: BUILD SUCCESSFUL, no reference to the deleted classes.

> Android compile requires the SDK + JDK 21 toolchain (see CLAUDE.md). If available: `./gradlew android:compileDebugJavaWithJavac`. If the Android SDK is not installed in this environment, note it and rely on the desktop compile + the identical injected line.

- [ ] **Step 7: Commit**

```bash
git add desktop/src/com/mdinic/game/giana/DesktopLauncher.java android/src/com/mdinic/game/giana/AndroidLauncher.java android/assets/highscore.properties.example .gitignore
git commit -m "feat: inject shared SupabaseHighScoreService; remove platform stubs"
```

---

## Task 11: Supabase backend (SQL + Edge Function + docs)

These files are deployed by the user; they are not compiled by Gradle. The Edge Function's HMAC must match `ScoreCodec` exactly: `HmacSHA256` over `name|score|level|ts`, lowercase hex.

**Files:**
- Create: `supabase/schema.sql`
- Create: `supabase/functions/submit-score/index.ts`
- Create: `supabase/README.md`

- [ ] **Step 1: Write the schema**

`supabase/schema.sql`:

```sql
-- Leaderboard table.
create table if not exists public.scores (
  id          bigint generated always as identity primary key,
  name        text   not null,
  score       int    not null,
  level       int    not null,
  ts          bigint not null,
  created_at  timestamptz not null default now()
);

-- A retried offline submission must not double-insert.
create unique index if not exists scores_dedup
  on public.scores (name, score, level, ts);

alter table public.scores enable row level security;

-- Reads are public; writes are NOT (only the Edge Function's service role inserts).
drop policy if exists scores_select_anon on public.scores;
create policy scores_select_anon on public.scores
  for select to anon using (true);
-- No insert/update/delete policy for anon on purpose.
```

- [ ] **Step 2: Write the Edge Function**

`supabase/functions/submit-score/index.ts`:

```ts
// Verifies the client's HMAC then inserts with the service role.
// Deploy: supabase functions deploy submit-score
// Secrets: supabase secrets set SCORE_SECRET=<same as game's score.secret>
//   (SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided by the platform.)
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SECRET = Deno.env.get("SCORE_SECRET")!;
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

// Plausibility ceilings (tune to your game). Reject obvious garbage.
const MAX_SCORE = 9_999_999;
const MAX_LEVEL = 31;

function toHex(buf: ArrayBuffer): string {
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function hmacHex(secret: string, message: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(message));
  return toHex(sig);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("method not allowed", { status: 405 });
  }
  let body: { name?: string; score?: number; level?: number; ts?: number; sig?: string };
  try {
    body = await req.json();
  } catch {
    return new Response("bad json", { status: 400 });
  }
  const { name, score, level, ts, sig } = body;
  if (
    typeof name !== "string" || typeof score !== "number" ||
    typeof level !== "number" || typeof ts !== "number" || typeof sig !== "string"
  ) {
    return new Response("bad fields", { status: 400 });
  }
  if (score < 0 || score > MAX_SCORE || level < 0 || level > MAX_LEVEL || name.length > 32) {
    return new Response("out of range", { status: 422 });
  }
  const expected = await hmacHex(SECRET, `${name}|${score}|${level}|${ts}`);
  if (expected !== sig) {
    return new Response("bad signature", { status: 401 });
  }
  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE);
  // Ignore duplicates from offline retries (unique index scores_dedup).
  const { error } = await supabase
    .from("scores")
    .upsert({ name, score, level, ts }, { onConflict: "name,score,level,ts", ignoreDuplicates: true });
  if (error) {
    return new Response(error.message, { status: 500 });
  }
  return new Response(JSON.stringify({ ok: true }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
```

- [ ] **Step 3: Write the deploy/verify README**

`supabase/README.md`:

```markdown
# Supabase backend for Giana high scores

## 1. Create the table
Open the Supabase project → SQL Editor → paste & run `schema.sql`.

## 2. Deploy the Edge Function
Install the Supabase CLI, then from the repo root:

    supabase login
    supabase link --project-ref <your-project-ref>
    supabase secrets set SCORE_SECRET=<pick-a-strong-secret>
    supabase functions deploy submit-score

`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are injected automatically.

## 3. Point the game at it
Copy `android/assets/highscore.properties.example` to
`android/assets/highscore.properties` and fill in:
- `supabase.url` — project URL
- `supabase.anonKey` — Project Settings → API → anon public key
- `supabase.functionsUrl` — `https://<ref>.functions.supabase.co`
- `score.secret` — the SAME value you set as `SCORE_SECRET`

## 4. Smoke test the function
Compute a signature with the same algorithm the game uses
(HMAC-SHA256 of `name|score|level|ts`, lowercase hex). Example with openssl:

    MSG='Tester|500|1|1700000000000'
    SIG=$(printf '%s' "$MSG" | openssl dgst -sha256 -hmac "<your-secret>" | sed 's/^.* //')
    curl -s -X POST "https://<ref>.functions.supabase.co/submit-score" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"Tester\",\"score\":500,\"level\":1,\"ts\":1700000000000,\"sig\":\"$SIG\"}"

Expect `{"ok":true}`. A wrong/absent sig returns 401. Then verify the read:

    curl -s "https://<project>.supabase.co/rest/v1/scores?select=name,score,level&order=score.desc&limit=5" \
      -H "apikey: <anon-key>" -H "Authorization: Bearer <anon-key>"
```

- [ ] **Step 4: Verify the HMAC algorithms match (cross-check)**

The Java `ScoreCodec.hmacSha256Hex` and the TS `hmacHex` both compute lowercase-hex HMAC-SHA256 over the identical `name|score|level|ts` string. Confirm by signing the same `Tester|500|1|1700000000000` with the openssl command above and comparing to a temporary `System.out.println(ScoreCodec.hmacSha256Hex("<secret>", "Tester|500|1|1700000000000"))`. They must be identical. (No code change; this is a verification step. Remove any temporary print.)

- [ ] **Step 5: Commit**

```bash
git add supabase/schema.sql supabase/functions/submit-score/index.ts supabase/README.md
git commit -m "feat: add Supabase schema, submit-score Edge Function, deploy docs"
```

---

## Task 12: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Replace the dead-backend note**

In the "Platform abstraction via services" section, replace the sentences stating the high-score backend is dead/deferred with:

```markdown
High scores are backed by **Supabase**, accessed through a single shared
`core` implementation, `SupabaseHighScoreService`, that uses libGDX's
cross-platform `Gdx.net` (no platform-specific high-score classes remain).
Leaderboard reads hit PostgREST directly with the anon key; writes go only
through a `submit-score` Edge Function that verifies an HMAC-SHA256 signature
(RLS forbids direct anonymous inserts). Leaderboards are cached in libGDX
`Preferences` for offline play and submissions made offline are queued and
retried. Connection config lives in `android/assets/highscore.properties`
(gitignored; see `highscore.properties.example`); if absent, the game runs
offline-only. Backend assets live under `supabase/`. The HMAC secret ships in
the client, so this blocks casual REST forgery but is not tamper-proof against
a determined reverse-engineer.
```

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for Supabase high scores"
```

---

## Task 13: Full verification

- [ ] **Step 1: Build everything that can build here**

Run: `./gradlew core:compileJava desktop:compileJava desktop:test`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 2: Run desktop and exercise the flow (no config = offline)**

Without a `highscore.properties` present, run: `./gradlew desktop:run`
Expected: game launches; the high-score screen shows empty (or cached) lists; finishing a game with a qualifying score reaches the enter-name screen; no crash, no exception in the log (you may see a benign LWJGL JNI warning per CLAUDE.md). Close the window.

- [ ] **Step 3 (optional, requires a real Supabase project): online smoke test**

Deploy per `supabase/README.md`, create `android/assets/highscore.properties`, run `./gradlew desktop:run`, finish a game with a qualifying score, type a name, and confirm the score appears in the all-time/today's lists (and via the read `curl`). Restart with the network off to confirm cached lists still render and that a score earned offline appears after reconnecting.

- [ ] **Step 4: Final commit (if any verification fixups were needed)**

```bash
git add -A
git commit -m "chore: high-score verification fixups"
```
```
(Skip if nothing changed.)

---

## Self-review notes (for the implementer)

- **Spec coverage:** online leaderboard (Tasks 9, 11) ✓; all-time + today's lists (Task 4 URLs, Task 9 fetch) ✓; shared core impl + delete stubs (Tasks 8–10) ✓; gitignored config with offline fallback (Tasks 2, 8, 10) ✓; HMAC + Edge Function, RLS blocks direct insert (Tasks 3, 11) ✓; cache reads (Task 8 load + Task 9 `applyAllTime`/`applyTodays` persist) ✓; queue submits (Tasks 7–9) ✓; unit tests for mapping/URL/HMAC/outbox (Tasks 3–7) ✓; `goodForHighScores` fixed so desktop now prompts (Task 8) ✓.
- **`getMyBest()` returning `null`** is expected by `EnterYourNameScreen` (it null-checks). Preserved.
- **Threading:** `Gdx.net` callbacks mutate shared lists under `lock`; the render thread reads the poll flags under the same `lock`. `Preferences` writes happen on the callback thread; acceptable (single-writer-at-a-time here, flushes are atomic enough for this use).
- **Replay window:** intentionally NOT enforced (only plausibility ceilings), so offline-queued scores submitted hours later still pass — matches the spec's offline-queue requirement.
