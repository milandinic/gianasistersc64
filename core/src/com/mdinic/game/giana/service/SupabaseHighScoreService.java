package com.mdinic.game.giana.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.net.HttpRequestBuilder;

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
    /** UTC day (yyyy-MM-ddT00:00:00Z) the cached today's-board was fetched for. */
    static final String K_TODAYS_DAY = "cache.todays.day";
    static final String K_OUTBOX = "outbox";
    static final String K_BEST_NAME = "best.name";
    static final String K_BEST_SCORE = "best.score";
    static final String K_BEST_LEVEL = "best.level";

    private static final int LIMIT = 5;

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

    private List<Score> scores = new ArrayList<Score>();
    private List<Score> todaysScores = new ArrayList<Score>();

    private boolean haveUpdate = true;
    private boolean haveTodaysUpdate = true;
    private boolean lastNetworkOk = false;
    /**
     * True while a submit-score POST is awaiting its response. Guards against
     * double-submitting the same outbox head when {@code flushOutbox()} is
     * triggered again (e.g. by a screen) before the in-flight POST returns and
     * removes the head. Read/written under {@link #lock}.
     */
    private boolean submitInFlight = false;

    /**
     * Production constructor. The launchers build the service in
     * main()/onCreate(), BEFORE libGDX populates the static {@code Gdx.*}
     * handles, so this MUST NOT touch Gdx. All Gdx-dependent setup is deferred
     * to {@link #ensureInit()}, which runs lazily on the first method call
     * (always after libGDX has initialized).
     */
    public SupabaseHighScoreService() {
    }

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

    /** Runs the Gdx-dependent setup exactly once. Safe to call on every method entry. */
    private synchronized void ensureInit() {
        if (initialized) {
            return;
        }
        this.config = loadConfig();
        this.prefs = Gdx.app.getPreferences(PREFS);
        init();
    }

    /** Loads cached lists and flushes the outbox. Requires config + prefs set. */
    private void init() {
        scores = ScoreStore.scoresFromJson(prefs.getString(K_ALLTIME, ""));
        todaysScores = ScoreStore.scoresFromJson(prefs.getString(K_TODAYS, ""));
        initialized = true;
        flushOutbox();
    }

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
            SupabaseConfig c = SupabaseConfig.fromSources(p, new EnvLookup() {
                public String get(String name) {
                    return System.getenv(name);
                }
            });
            if (!c.isConfigured()) {
                Gdx.app.error("HighScore", "config incomplete, running offline — missing: "
                        + String.join(", ", c.missingKeys()));
            }
            return c;
        } catch (Exception e) {
            Gdx.app.error("HighScore", "config load failed, offline mode", e);
            return SupabaseConfig.fromProperties(null);
        }
    }

    // --- poll contract -------------------------------------------------------

    @Override
    public boolean haveScoreUpdate() {
        ensureInit();
        synchronized (lock) {
            return haveUpdate;
        }
    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        ensureInit();
        synchronized (lock) {
            return haveTodaysUpdate;
        }
    }

    @Override
    public List<Score> getScoreUpdate() {
        ensureInit();
        synchronized (lock) {
            haveUpdate = false;
            return new ArrayList<Score>(scores);
        }
    }

    @Override
    public List<Score> getTodaysScoreUpdate() {
        ensureInit();
        synchronized (lock) {
            haveTodaysUpdate = false;
            return new ArrayList<Score>(todaysScores);
        }
    }

    // --- fetch / save (network added next task) ------------------------------

    @Override
    public void fetchHighScores() {
        ensureInit();
        fetch();
    }

    @Override
    public void fetchTodaysHighScores(boolean saveLocalScoreToWeb) {
        ensureInit();
        // When asked to also push the local score, flush FIRST. If that starts a
        // submit, the POST's success callback re-fetches the leaderboard after
        // the insert lands — so an up-front fetch() here would only read the
        // stale pre-insert list and get immediately superseded. Skip it in that
        // case; otherwise (nothing queued) fetch now so the screen still loads.
        if (saveLocalScoreToWeb && flushOutbox()) {
            return;
        }
        fetch();
    }

    void fetch() {
        if (!config.isConfigured()) {
            Gdx.app.debug("HighScore", "fetch skipped, offline (config incomplete)");
            return; // offline: cached lists already serve the screen
        }
        Gdx.app.debug("HighScore", "fetching leaderboards from " + config.url);
        getJson(ScoreCodec.allTimeUrl(config.url), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                List<Score> fresh = ScoreCodec.parseScores(body);
                Gdx.app.debug("HighScore", "received " + fresh.size() + " all-time scores");
                applyAllTime(fresh);
            }

            public void fail() {
                setLastNetworkOk(false);
            }
        });
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
    }

    @Override
    public void saveHighScore(Score score) {
        ensureInit();
        Gdx.app.debug("HighScore", "saving score name=" + score.getName() + " score=" + score.getScore()
                + " level=" + score.getLevel());
        Score best = getMyBest();
        if (best == null || best.getScore() < score.getScore()) {
            saveMyBest(score);
        }
        // Without a backend + secret we cannot sign the score, and an unsigned
        // entry can never pass the server's HMAC check — queuing it would only
        // poison the outbox and block every later submission. The local best is
        // already saved above, so just stop here when offline-only.
        if (!config.isConfigured()) {
            Gdx.app.debug("HighScore", "offline: score saved locally only, not submitted");
            return;
        }
        long ts = System.currentTimeMillis();
        String sig = ScoreCodec.hmacSha256Hex(config.secret,
                ScoreCodec.signingString(score.getName(), score.getScore(), score.getLevel(), ts));
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

    /**
     * Attempts to submit the outbox head, if any.
     *
     * @return true if a submit-score POST is in flight — either this call
     *         started one, or one was already running. In both cases that POST's
     *         success callback re-fetches the leaderboard after the insert lands,
     *         so callers should NOT also fetch up-front: a pre-insert read is
     *         wasted and the callback's post-insert fetch supersedes it. Returns
     *         false only when no submit will happen: offline, the queue is empty,
     *         or only unsignable poison entries were dropped — then the caller
     *         must fetch itself for the screen to load.
     */
    boolean flushOutbox() {
        if (!config.isConfigured()) {
            return false;
        }
        // A submit-score POST removes its head only when the response lands. If
        // flushOutbox() runs again in that window (saveHighScore flushes, then
        // HighScoreScreen.show() flushes too) it would re-read the same head and
        // POST it a second time — double-inserting the score and firing a second
        // follow-up fetch. Claim the in-flight slot first; if it's already taken,
        // that running POST will re-fetch, so report a submit is in flight.
        synchronized (lock) {
            if (submitInFlight) {
                return true;
            }
            submitInFlight = true;
        }
        final List<PendingSubmit> box = readOutbox();
        if (box.isEmpty()) {
            synchronized (lock) {
                submitInFlight = false;
            }
            return false;
        }
        // Attempt each entry; on success remove it. Process the head; the
        // response callback re-invokes flush for the remainder.
        final PendingSubmit head = box.get(0);
        // Self-heal: an entry with no signature (e.g. queued by an older build
        // before config was loaded) can never be accepted and would block the
        // queue forever. Drop it and move on to the next. No POST went out, so
        // release the slot before recursing; the recursive call's result tells
        // the caller whether the next entry started a submit.
        if (head.sig == null || head.sig.trim().isEmpty()) {
            box.remove(0);
            writeOutbox(box);
            synchronized (lock) {
                submitInFlight = false;
            }
            return flushOutbox();
        }
        Gdx.app.debug("HighScore", "submitting queued score to submit-score (" + box.size() + " in outbox)");
        postJson(config.functionsUrl + "/submit-score", ScoreCodec.submitBody(head), new Consumer() {
            public void ok(String body) {
                setLastNetworkOk(true);
                List<PendingSubmit> current = readOutbox();
                if (!current.isEmpty()) {
                    current.remove(0);
                    writeOutbox(current);
                }
                Gdx.app.debug("HighScore", "submit accepted, " + readOutbox().size() + " entries remain");
                // Release the slot before any follow-up flush/fetch so the next
                // queued entry can be submitted.
                synchronized (lock) {
                    submitInFlight = false;
                }
                if (readOutbox().isEmpty()) {
                    // Queue drained: the just-inserted score(s) are now in the
                    // table, so re-read the leaderboard. Without this the screen
                    // that triggered the submit shows the pre-insert list (the
                    // reads raced ahead of the write) and the new entry only
                    // appears on the next visit. HighScoreScreen polls
                    // haveScoreUpdate() each frame, so this refreshes live.
                    fetch();
                } else {
                    flushOutbox(); // more entries queued: submit the next one
                }
            }

            public void fail() {
                setLastNetworkOk(false); // keep entry; retry later
                synchronized (lock) {
                    submitInFlight = false;
                }
            }
        });
        return true; // a POST is now in flight; its callback will re-fetch
    }

    // --- personal best -------------------------------------------------------

    @Override
    public Score getMyBest() {
        ensureInit();
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
        ensureInit();
        return config.isConfigured() && lastNetworkOk;
    }

    void setLastNetworkOk(boolean ok) {
        this.lastNetworkOk = ok;
    }

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

    // --- state mutation used by network callbacks (next task) ----------------

    void applyAllTime(List<Score> fresh) {
        synchronized (lock) {
            scores = fresh;
            haveUpdate = true;
        }
        prefs.putString(K_ALLTIME, ScoreStore.scoresToJson(fresh));
        prefs.flush();
    }

    void applyTodays(List<Score> fresh, long now) {
        synchronized (lock) {
            todaysScores = fresh;
            haveTodaysUpdate = true;
        }
        prefs.putString(K_TODAYS, ScoreStore.scoresToJson(fresh));
        prefs.putString(K_TODAYS_DAY, ScoreCodec.utcMidnightIso(now));
        prefs.flush();
    }

    // --- HTTP plumbing -------------------------------------------------------

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
}
