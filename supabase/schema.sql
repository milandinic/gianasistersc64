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
