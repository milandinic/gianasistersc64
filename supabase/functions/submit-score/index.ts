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
