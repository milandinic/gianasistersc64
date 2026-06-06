# High-score config: environment-variable override

**Date:** 2026-06-06
**Status:** Approved, ready for implementation plan

## Goal

When running the game from IntelliJ IDEA (desktop), supply Supabase high-score
config via environment variables so that no secrets are written to disk and
cannot be read off the filesystem. Production deployments keep using
`highscore.properties` unchanged. Environment variables override the file on a
per-value basis; if neither source supplies a value, the existing offline-only
behavior is preserved.

## Precedence

Resolved independently for each of the four config values:

```
System.getenv(GIANA_*)  →  highscore.properties  →  empty (offline)
```

A blank environment value (`""`) is treated as absent, identical to a blank
property. `SupabaseConfig`'s constructor already trims values and
`isConfigured()` rejects any blank field, so blanks never produce a
half-configured service.

## Environment variable names

All four are optional. The `GIANA_` prefix namespaces them to avoid clashing
with the Supabase CLI's own `SUPABASE_*` / `SCORE_SECRET` variables when run in
the same shell.

| Env var                         | Property key             |
| ------------------------------- | ------------------------ |
| `GIANA_SUPABASE_URL`            | `supabase.url`           |
| `GIANA_SUPABASE_ANON_KEY`       | `supabase.anonKey`       |
| `GIANA_SUPABASE_FUNCTIONS_URL`  | `supabase.functionsUrl`  |
| `GIANA_SCORE_SECRET`            | `score.secret`           |

## Scope of change

Only two `core` classes change. No platform launcher changes, no new service
constructor, no change to the production file workflow.

### `SupabaseConfig`

Add a new factory that merges an env source over a file source:

```java
/** Resolves config: env value (if non-blank) wins over file property. */
public static SupabaseConfig fromSources(Properties fileProps, EnvLookup env);
```

- `EnvLookup` is a tiny single-method interface defined in `core`
  (`String get(String name)`). It exists purely so tests can inject a fake
  environment without touching real process env vars. Production passes an
  anonymous `EnvLookup` that delegates to `System.getenv(name)`.
- For each field, `fromSources` takes the env value when non-blank, otherwise
  the file property (or `null` when `fileProps == null`).
- The existing `fromProperties(Properties)` is retained for the current tests
  and reimplemented as `fromSources(p, env-returning-null)`, so its behavior is
  unchanged.

The codebase uses pre-lambda anonymous-class style throughout `core` (see the
`Consumer` / `HttpResponseListener` callbacks in `SupabaseHighScoreService`);
this change follows that convention rather than introducing lambdas/method
references.

### `SupabaseHighScoreService.loadConfig()`

Change the final mapping so env vars are always consulted, and so that
**env-only config works even when the file is absent**:

```java
static SupabaseConfig loadConfig() {
    try {
        FileHandle fh = Gdx.files.internal("highscore.properties");
        Properties p = null;
        if (fh.exists()) {
            p = new Properties();
            p.load(fh.read());
        }
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

This closes a subtle gap in the current code: today a missing file
short-circuits to offline mode, which would ignore env vars. After this change,
env vars alone are sufficient even with no `highscore.properties` present.

`System.getenv` is plain JDK and cross-platform. On Android it returns `null`
for these names, so the Android resolution path is unchanged (file → offline).
It carries no platform-specific behavior, so it does not violate the project's
"no platform APIs in core" rule the way bypassing `Gdx` file I/O would.

## Data flow

```
loadConfig()
  ├─ file exists?  load Properties p   else  p = null
  └─ SupabaseConfig.fromSources(p, env)        // env delegates to System.getenv
         per field: env(GIANA_X) non-blank ? env : p?.getProperty(key)
```

## Error handling

Unchanged. The existing try/catch in `loadConfig` still falls back to an
offline `SupabaseConfig` on any exception (e.g. malformed properties file).

## Testing

Add unit tests driving `SupabaseConfig.fromSources` with a fake `EnvLookup`.
All are pure — no Gdx, no real environment variables:

1. Env value overrides the file value for the same field.
2. File value is used when the env value is absent or blank.
3. Env-only config (Properties `null`) yields a `isConfigured()`-true config.
4. Neither source set → `isConfigured()` is false (offline).

## Documentation

- Update `highscore.properties.example` header to mention the env-var override
  and list the four `GIANA_*` names.
- Update `CLAUDE.md`'s high-score section to note the precedence and that IDEA
  run configurations set the four `GIANA_*` env vars (in the run config's
  *Environment variables* field).

## Out of scope (YAGNI)

- No Android launcher changes.
- No new service constructor or setter.
- No change to the production `highscore.properties` workflow.