# Stale high-score cache: fix today's-board qualification

## Problem

When a player earns a score that should enter the **daily** ("today's") high-score
board, they are sometimes sent straight to `HighScoreScreen` without ever being
asked for their name. It reproduces when the local cache already holds a previous
day's today's-board (e.g. a new UTC day has started but the cached board is
yesterday's, full of five high entries).

Root cause is an ordering/data problem, not a network problem:

- The decision to show `EnterYourNameScreen` is made the instant the player
  dies/finishes, in `GameScreen`, `BonusGameScreen`, and `GameCompletedScreen`:
  `if (highScoreService.goodForHighScores(map.score)) -> EnterYourNameScreen else -> HighScoreScreen`.
- `goodForHighScores` decides synchronously against the in-memory `todaysScores`
  list, which was loaded from the local `Preferences` cache (`K_TODAYS`) at
  startup.
- The cached today's-board carries **no notion of which day it represents**
  (`ScoreStore.scoresToJson` stores only name/score/level). So on a new day the
  gate consults yesterday's full board, the score fails to beat it, and the
  player is silently dropped to `HighScoreScreen`.
- The only thing that refreshes `todaysScores` from the server is `fetch()`,
  triggered from `HighScoreScreen.show()` — which runs *after* the gate decision.

The irreversible moment is the gate decision at death time: if we don't prompt
for a name then, that score is gone forever. (A captured name can be submitted
later via the outbox even if offline; an un-captured one cannot.)

## Philosophy

The client gate is a **generous pre-filter**; the Supabase server is the **judge**
of what actually lands on the board (writes go through the `submit-score` Edge
Function, and the board is re-read after the insert). So the gate's job is only
to avoid the false-negative — never silently skip a score that could qualify.
When we cannot confirm (offline, or stale cache), we **prompt**; the server keeps
the entry only if it truly qualifies once the queued submit goes through. This
behaves identically online and offline.

## Approach (chosen)

Approach 2 = **stale-day detection** (the real fix) + **proactive refresh**
(online accuracy boost).

### 1. Stale-day detection

Stamp the cached today's-board with the UTC day it was fetched for, and have the
gate treat a previous-day (or missing) stamp as an empty board.

- **New persisted key:** `K_TODAYS_DAY = "cache.todays.day"` in
  `SupabaseHighScoreService`, storing the UTC day as the ISO string
  `yyyy-MM-ddT00:00:00Z` (reuse `ScoreCodec.utcMidnightIso`). It is a sibling of
  `K_TODAYS`; the score-list storage format (`ScoreStore`) is unchanged, so there
  is no cache-format migration.
- **Writer:** `applyTodays(List<Score> fresh, long now)` writes `K_TODAYS` as
  today, then `K_TODAYS_DAY = utcMidnightIso(now)`. `now` is threaded in from the
  same value `fetch()` used to build `todaysUrl`, so the queried day and the
  stamped day always match.
- **Reader:** `goodForHighScores(int score)`:
  - `score <= 0` -> `false` (unchanged).
  - stamp missing **or** `!= utcMidnightIso(clock.now())` -> treat today's board
    as **empty** -> any `score > 0` -> `true` (the fix; also the existing-user
    path, since installs have no stamp yet).
  - stamp `==` current UTC day -> existing logic: `todaysScores.size() < LIMIT`,
    or `score` beats some entry.

### 2. Proactive refresh

In `init()` (the deferred lazy-init that already loads caches and calls
`flushOutbox()`), fetch today's board so that, online, the in-memory board is
fresh well before any death.

- Only fetch when no submit is in flight: `flushOutbox()` returns `true` when a
  submit is in flight (its success callback already re-fetches), so `init()`
  calls `fetch()` itself only when `flushOutbox()` returns `false` (queue empty /
  offline / drained). This avoids a duplicate fetch.
- Offline, `fetch()` no-ops (config not configured, or network failure) — the
  stale-day logic in the gate still produces the correct prompt.

## Time seam (testability)

`goodForHighScores` and `applyTodays` need "now" to compute/compare the UTC day,
but the codebase deliberately keeps testable logic free of
`System.currentTimeMillis()` (cf. `ScoreCodec`, `ScoreStore`). Introduce a small
package-private time source mirroring the existing `EnvLookup` seam:

```java
interface Clock { long now(); }
// production default: () -> System.currentTimeMillis()
// test: () -> fixedMillis
```

The public `HighScoreService` interface and the three gate call sites
(`GameScreen`, `BonusGameScreen`, `GameCompletedScreen`) are **unchanged** —
`goodForHighScores(int)` keeps its signature and reads `clock.now()` internally.
`fetch()` uses the same `clock.now()` for `todaysUrl` and passes it to
`applyTodays`, so the queried day and stamped day are consistent.

## Behavior summary

- **Reported scenario (new day, stale full cache, online):** launch fetch
  replaces the cached board with today's (empty) board and stamps it; even if the
  fetch has not returned by death, the stale stamp makes the gate treat the board
  as empty -> name prompt -> score captured and queued -> server judges.
- **Offline:** no fetch; stale/missing stamp still triggers the prompt; the
  captured score sits in the outbox and is retried when the network returns.
- **Existing installs:** no stamp key yet -> treated as stale -> the fix takes
  effect immediately; the first successful fetch writes a real stamp and the
  system reaches steady state. No migration code.

## Testing

Extend `desktop/test/.../SupabaseHighScoreServiceTest.java` (already injects
`SupabaseConfig` + `Preferences`; add `Clock` injection via the test constructor
seam):

- `goodForHighScores` with a fixed `Clock`:
  - stamp == today, board full, lower score -> `false`.
  - stamp == yesterday, same board/score -> `true`.
  - missing stamp -> `true` for `score > 0`.
  - `score <= 0` -> `false` regardless of stamp.
- `applyTodays(fresh, now)` writes both `K_TODAYS` and `K_TODAYS_DAY` for the
  clock's UTC day (assert via the injected `Preferences`).

## Scope / non-goals

- No intra-day staleness handling beyond "server judges" (e.g. another player
  grabbing the last slot during the session) — accepted by the philosophy.
- No change to the day definition: "today" stays UTC (`utcMidnightIso`),
  consistent with `todaysUrl`.
- No fetch-and-wait/spinner at death; screen transitions stay instant.
- No change to the public `HighScoreService` interface or the three gate call
  sites.
- No change to the cached score-list format (`ScoreStore`) — only an added
  sibling timestamp key.
