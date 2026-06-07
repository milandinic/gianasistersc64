# More logging — design

Date: 2026-06-07

## Goal

Add more diagnostic logging to the game:

1. A **debug** log on every screen load and unload.
2. **Debug** logs when high scores are sent and when they are read.
3. An **error** log at app start when env/file high-score properties are missing.

The log level is already raised to `LOG_DEBUG` in `GianaSistersC64.create()`, so
debug lines are emitted today (desktop console / Android logcat). No log-level
change is needed.

## Current state

- Screen lifecycle logging is inconsistent: some screens log on `hide()` with
  copy-pasted/incorrect tags and messages (`"dispose game screen"`,
  `"dispose intro"`), `IntroScreen` logs only on `show()`, and several screens
  (`GameCompletedScreen`, `LevelStartingScreen`, `EnterYourNameScreen`) log
  nothing. Tags mix `"GianaByte"` and screen names.
- `SupabaseHighScoreService` already has **error** logs for HTTP non-2xx
  (`send`), transport failure (`send`), and config-load exceptions
  (`loadConfig`). It has **no** info/debug logs for reads or sends.
- A merely *incomplete* config (the common case — no `highscore.properties` and
  no env vars) is **not** logged at all; the game silently drops into offline
  mode.

In libGDX, `Game` calls `show()` when a screen becomes current and `hide()` when
it is swapped out; `dispose()` is never invoked during normal screen swaps in
this game. So `show()` is the "load" hook and `hide()` is the "unload/dispose"
hook — that is where the lifecycle logging belongs.

## Design

### 1. Centralized screen lifecycle logging — `GianaSistersScreen`

Add to the base class:

```java
@Override
public void show() {
    Gdx.app.debug(getClass().getSimpleName(), "show (load)");
}

@Override
public void hide() {
    Gdx.app.debug(getClass().getSimpleName(), "hide (dispose)");
}
```

`getClass().getSimpleName()` makes the tag the concrete screen's name
(`IntroScreen`, `GameScreen`, …) automatically — new screens get logging for
free. Every screen that overrides `show()`/`hide()` adds a `super.show()` /
`super.hide()` call (first line). The old ad-hoc/incorrect log lines are
removed.

### 2. High-score read/send logging — `SupabaseHighScoreService`

Keep existing error logs; add debug logs (tag `"HighScore"`):

- `fetch()`: configured → `"fetching leaderboards from " + config.url`;
  offline → `"fetch skipped, offline (config incomplete)"`.
- All-time `ok` callback → `"received N all-time scores"`.
- Todays `ok` callback → `"received N todays scores"`.
- `saveHighScore(score)` → `"saving score name=… score=… level=…"`; when
  offline, note it is saved locally only and not submitted.
- `flushOutbox()` head submit → `"submitting queued score to submit-score"`;
  on `ok` → `"submit accepted, N entries remain"`.

### 3. Missing-config error at startup — `loadConfig`

After building the `SupabaseConfig`, if `!isConfigured()`, emit one
`Gdx.app.error("HighScore", "config incomplete, running offline — missing: <keys>")`
naming exactly which of `supabase.url` / `supabase.anonKey` /
`supabase.functionsUrl` / `score.secret` are blank.

This needs a small testable helper on `SupabaseConfig`:

```java
/** Property keys whose values are blank (empty when fully configured). */
public java.util.List<String> missingKeys() { ... }
```

`loadConfig()` runs in `ensureInit()` on the first service method call — after
libGDX is up — so `Gdx.app` is non-null (respects "no Gdx in service
constructors").

## Testing

- `SupabaseConfig.missingKeys()` is a pure function — add a unit test under
  `desktop/test` alongside the existing `SupabaseConfig` tests (all-present →
  empty; all-blank → all four keys; partial → just the blanks).
- The logging calls themselves go through `Gdx.app` and are not unit-tested,
  consistent with the existing code.

## Out of scope

- No log-level changes.
- No new logging framework — keep using `Gdx.app.{debug,error}`.
- No logging of secret values (never log `config.secret` or `anonKey`).