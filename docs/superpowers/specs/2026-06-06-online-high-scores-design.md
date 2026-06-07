# Online High Scores — Design

Date: 2026-06-06

## Goal

Restore a working high-score feature. The UI flow (`EnterYourNameScreen` →
`HighScoreScreen`, gated by `goodForHighScores`) and the `HighScoreService`
interface already exist, but the data behind them is broken: the desktop
implementation uses hardcoded in-memory fakes that never persist, and the
Android implementation only stores a single personal-best while its leaderboard
lists are always empty.

Deliver a **real online leaderboard** backed by a managed BaaS (Supabase), with
**all-time** and **today's** lists, that **caches responses for offline play**,
**queues score submissions made while offline**, and **raises the bar against
casual score forgery** via an HMAC signature verified by a server-side Edge
Function.

## Non-goals

- Truly tamper-proof scoring. A client-only game with a shipped secret cannot be
  made cheat-proof (see Anti-cheat → Limitations). We block casual forgery
  through the public REST endpoint, not a determined reverse-engineer.
- Accounts, auth, or per-user identity beyond a typed name.
- Server-side replay/validation of a run.
- Changes to gameplay, scoring rules, or the look of the high-score screen.

## Decisions (from brainstorming)

- **Backend:** Supabase (PostgREST + one Edge Function). Chosen over Firebase
  because PostgREST offers clean REST ordering/filtering via URL query params and
  a flat JSON shape that maps directly onto `Gdx.net` calls.
- **Lists:** keep both "all time greatest" and "today's greatest" — the existing
  `HighScoreScreen` renders both sections.
- **Code structure:** one shared implementation in the `core` module using
  libGDX's cross-platform `Gdx.net`. Both launchers inject it. The two platform
  stub implementations are deleted. The `HighScoreService` interface is unchanged
  and remains the platform boundary.
- **Config/secrets:** Supabase URL + anon key read at startup from a gitignored
  config file, with a committed `.example`. Missing/blank config → offline mode,
  no crash.
- **Anti-cheat:** client HMAC-SHA256 signature + a Supabase Edge Function that
  verifies the signature server-side before inserting; RLS forbids direct
  anonymous insert.
- **Offline:** cache fetched leaderboards locally and serve them when
  offline/at startup; queue failed submissions to a persisted outbox and retry.

## Architecture

### One shared `core` implementation

New class `SupabaseHighScoreService` in
`core/src/com/mdinic/game/giana/service/` implementing the existing
`HighScoreService` interface with **no interface changes**.

`Gdx.net` works identically on desktop (LWJGL3) and Android, so the online client
lives once in `core`. Both `DesktopLauncher` and `AndroidLauncher` construct and
inject it via the existing `setHighScoreService(...)`. The stubs
`HighScoreServiceDesktop` and `HighScoreServiceDroid` are **deleted**.

`InternetConnectionChecker` (currently Android-only) is no longer required by the
service; reachability is inferred from HTTP success/failure. The service may
accept an optional checker for a fast pre-check, but it is not depended upon. (We
keep the interface for now; Android can still pass `this`.)

### No new dependencies

- Networking: `Gdx.net.sendHttpRequest` / `HttpRequestBuilder`.
- JSON parse/emit: libGDX `com.badlogic.gdx.utils.Json` / `JsonReader`.
- Local persistence: libGDX `Gdx.app.getPreferences("giana-highscores")`
  (cross-platform; replaces Android `SharedPreferences`).
- HMAC: `javax.crypto.Mac` with `"HmacSHA256"` (present on JDK and Android).

## Backend (Supabase)

### Table

```
scores (
  id          bigint generated always as identity primary key,
  name        text not null,
  score       int  not null,
  level       int  not null,
  ts          bigint not null,           -- client epoch millis used in the HMAC
  created_at  timestamptz not null default now()
)
```

Unique constraint on `(name, score, level, ts)` so a retried submission cannot
double-insert.

### Row-Level Security

- RLS **enabled**.
- Policy: anonymous **SELECT** allowed (reads are harmless).
- **No** anonymous INSERT/UPDATE/DELETE policy → the public REST endpoint cannot
  insert. Inserts happen only through the Edge Function using the service role.

### Read queries (PostgREST, via anon key)

- All-time:
  `GET /rest/v1/scores?select=name,score,level&order=score.desc&limit=5`
- Today's: same plus `&created_at=gte.<todayMidnightUtcISO>`
- Headers: `apikey: <anonKey>`, `Authorization: Bearer <anonKey>`.

### Write path (Edge Function `submit-score`)

- Client `POST <functionsUrl>/submit-score` with JSON
  `{ name, score, level, ts, sig }`.
- `sig = base16( HMAC_SHA256( SCORE_SECRET, name + "|" + score + "|" + level + "|" + ts ) )`.
- Function recomputes the HMAC using `SCORE_SECRET` from its env, and rejects on:
  - signature mismatch,
  - stale/future `ts` (outside a window, e.g. ±10 minutes — blunts replay while
    tolerating offline-queued submits within the window... see note),
  - out-of-range `score`/`level` (basic plausibility).
- On success it inserts with the service role and relies on the unique
  constraint to ignore duplicates (upsert/ignore-on-conflict).

> **Replay window vs. offline queue tension:** a strict `ts` freshness window
> would reject a score queued offline for hours. Resolution: the freshness check
> is **lenient** (or disabled) for now — its value against a shipped-secret
> attacker is limited anyway. The unique constraint + signature are the real
> gate. The window is documented as a tunable knob, defaulted wide enough that
> realistic offline delays pass. This keeps the offline-queue requirement intact.

### Deliverables in the implementation plan

- Exact SQL for table + RLS policies.
- Edge Function source (TypeScript/Deno) for `submit-score`.
- Deploy steps (`supabase functions deploy submit-score`, set `SCORE_SECRET`).
- A `curl` smoke test for the function.

## Config

File `highscore.properties` (gitignored), loaded at startup. Keys:

```
supabase.url=https://<project>.supabase.co
supabase.anonKey=<anon key>
supabase.functionsUrl=https://<project>.functions.supabase.co
score.secret=<shared HMAC secret, must match the Edge Function's SCORE_SECRET>
```

- Committed `highscore.properties.example` documents the format with placeholders.
- Loaded via `Gdx.files.internal("highscore.properties")`; folded into the
  desktop `dist` jar and `android/assets` the same way other data files are (the
  file lives where the launchers can read it — final location decided in the
  plan, likely `android/assets/` to match the single-asset-dir convention, or a
  classpath resource).
- If the file is absent, unreadable, or any required key is blank → **offline
  mode**: `internetAvailable()` returns false, fetches serve cache only, submits
  go to the outbox and never POST. The game runs normally; lists show cached or
  empty data. No crash.

## Data flow & threading (preserves the existing screen contract)

The screens already use a poll model: each frame `HighScoreScreen.render()`
checks `haveScoreUpdate()` / `haveTodaysScoreUpdate()` and, when true, consumes
`getScoreUpdate()` / `getTodaysScoreUpdate()`. `Gdx.net` callbacks run on a
background thread. This contract is preserved exactly.

- **Construction:** load config; load cached lists from `Preferences` into memory
  and set the `haveUpdate` flags true (so the screen shows cache immediately);
  flush the submit outbox (best effort).
- **`fetchHighScores()` / `fetchTodaysHighScores(saveLocalScoreToWeb)`:** fire
  async GETs. On success (callback thread): parse JSON → `List<Score>`, store
  under a lock, persist to the read cache, set the corresponding `haveUpdate`
  flag. On failure/cancel: log via `Gdx.app.error`, keep last-known/cached list.
  `saveLocalScoreToWeb=true` additionally triggers an outbox flush.
- **`getScoreUpdate()` / `getTodaysScoreUpdate()`:** return a copy under lock and
  clear the flag (unchanged contract).
- **`saveHighScore(score)`:** update local personal-best; compute the HMAC over
  the score with a captured `ts`; enqueue the signed payload to the persisted
  outbox; attempt the POST. On success remove from outbox and re-fetch both
  lists; on failure leave it queued.
- **`goodForHighScores(int score)`:** true if today's list has < 5 entries or
  `score` beats the lowest entry. (Desktop never prompted before because the stub
  always returned false; this fixes it.)
- **`getMyBest()`:** read/write local personal-best in `Preferences`. Used to
  pre-fill the name prompt. Local only.
- **`internetAvailable()`:** reflects whether config is present and the last
  network attempt succeeded (best-effort hint; not authoritative).

### Offline read cache

Every successful fetch serializes the all-time and today's lists (libGDX `Json`)
into `Preferences` under stable keys. At startup and on any failed/offline fetch,
the in-memory lists are seeded from this cache and the poll flags are raised, so
the screen is never blank when data was seen before.

### Offline submit outbox

`saveHighScore` always enqueues the **signed** payload (including its `ts`) to a
persisted outbox before attempting the network. The signature is computed at
enqueue time, so it survives an arbitrary offline delay. The outbox is flushed:
on service construction (next launch), and after any successful fetch /
`saveLocalScoreToWeb=true`. Each successful POST removes its entry; the Edge
Function's unique constraint makes a duplicate retry a no-op. Failures keep the
entry for the next attempt.

## Error handling

- Every `Gdx.net` request sets a timeout and provides
  `failed`/`cancelled`/`handleHttpResponse` callbacks.
- Non-2xx and transport errors log via `Gdx.app.error` and never throw into the
  render loop; the last-known/cached data is retained.
- JSON parse errors are caught per-response and treated as a failed fetch.
- Missing/blank config is treated as offline mode, not an error.

## Anti-cheat — limitations (stated plainly)

To sign a score the client must hold `score.secret`, and a secret shipped inside
a desktop jar or Android APK is extractable by a determined reverse-engineer.
This design blocks the **easy** attack (POSTing arbitrary rows straight to the
public REST endpoint) by closing anonymous insert via RLS and forcing all writes
through the signature-checking Edge Function. It does **not** defend against an
attacker who extracts the secret and signs their own payloads. Truly tamper-proof
scoring would require server-side validation of the run itself, which is out of
scope. This tradeoff was accepted during brainstorming.

## Testing

Pure-function unit tests under the existing headless `desktop/test` harness
(JUnit 4, `HeadlessApplication`, `workingDir = ../android/assets`):

- JSON ↔ `Score` mapping (parse a PostgREST response array; emit a submit body).
- PostgREST query-URL building (all-time and today's, including the
  `created_at=gte.` filter).
- HMAC signature determinism against a known test vector.
- Outbox enqueue → serialize → round-trip restore.

Not unit-tested (require a live endpoint): real network calls and the Edge
Function. The plan includes a `curl` smoke test for the deployed function.

The existing `MapTest` is unaffected.

## Files touched

- **Add:** `core/.../service/SupabaseHighScoreService.java` (+ small helpers for
  HMAC, JSON mapping, outbox/cache as needed).
- **Add:** `highscore.properties.example`; `.gitignore` entry for
  `highscore.properties`.
- **Add:** Edge Function source + SQL under a `supabase/` (or `docs/`) directory.
- **Add:** unit tests under `desktop/test/...`.
- **Edit:** `DesktopLauncher`, `AndroidLauncher` to inject the shared service.
- **Edit:** `CLAUDE.md` (high-score architecture note — currently says online is
  deferred).
- **Delete:** `HighScoreServiceDesktop`, `HighScoreServiceDroid`.
- **Unchanged:** `HighScoreService` interface, all screens, `Score`.
