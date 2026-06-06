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
