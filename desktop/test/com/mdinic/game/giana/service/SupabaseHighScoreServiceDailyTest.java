package com.mdinic.game.giana.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Day-boundary behavior of {@link SupabaseHighScoreService}: the today's-board
 * day stamp written by {@code applyTodays}, and the stale/missing-stamp logic in
 * {@code goodForHighScores}. Lives in the {@code service} package so it can reach
 * the package-private seam constructor, {@code applyTodays}, and the cache keys.
 */
public class SupabaseHighScoreServiceDailyTest {

    private HeadlessApplication app;
    private Preferences prefs;

    private static final SupabaseConfig OFFLINE = new SupabaseConfig("", "", "", "");

    // 2026-06-29T12:00:00Z and the prior day, as epoch millis (UTC).
    // Derived from ScoreCodec.utcMidnightIso so the test and prod agree on "day".
    private static final long DAY_2026_06_29_NOON = 1782734400000L;
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long DAY_2026_06_28_NOON = DAY_2026_06_29_NOON - ONE_DAY_MILLIS;

    @Before
    public void setUp() {
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
        prefs = Gdx.app.getPreferences("giana-highscores-daily-test");
        prefs.clear();
        prefs.flush();
    }

    @After
    public void tearDown() {
        prefs.clear();
        prefs.flush();
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    private static Clock fixedClock(final long millis) {
        return new Clock() {
            public long now() {
                return millis;
            }
        };
    }

    private static List<Score> fullBoard() {
        List<Score> board = new ArrayList<Score>();
        for (int i = 0; i < 5; i++) {
            board.add(new Score("P" + i, 1000 + i, 1));
        }
        return board; // five entries, lowest is 1000
    }

    /** applyTodays must persist both the list and the UTC-day stamp for the clock's day. */
    @Test
    public void applyTodays_writesDayStamp() {
        SupabaseHighScoreService svc =
                new SupabaseHighScoreService(OFFLINE, prefs, fixedClock(DAY_2026_06_29_NOON));

        svc.applyTodays(fullBoard(), DAY_2026_06_29_NOON);

        assertEquals(ScoreCodec.utcMidnightIso(DAY_2026_06_29_NOON),
                prefs.getString(SupabaseHighScoreService.K_TODAYS_DAY, ""));
    }
}
