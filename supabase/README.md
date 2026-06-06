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

`submit-score` is called anonymously by the game and is protected by the HMAC,
not by a JWT, so JWT verification must be **off** for it. `supabase/config.toml`
sets `verify_jwt = false` for the function, which `supabase functions deploy`
honors. (If you deploy without that config, add `--no-verify-jwt`.) If the gateway
still rejects calls with `401 {"code":"UNAUTHORIZED_NO_AUTH_HEADER"}`, the function
was deployed with JWT verification on — redeploy with the config above.

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

### Algorithm cross-check
The game's `ScoreCodec.hmacSha256Hex` and this function's `hmacHex` must agree.
Known vector: with secret `s3cr3t` and message `Giana|1260|3|1700000000000`, both
produce `009269c8313759bf08197f67cb04c64f44060887040fe31914270af99117c256`
(verified by `ScoreCodecTest.hmac_knownVector`). Confirm with:

    printf '%s' 'Giana|1260|3|1700000000000' | openssl dgst -sha256 -hmac 's3cr3t'
