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
