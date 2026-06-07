package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.mdinic.game.giana.service.Score;
import com.mdinic.game.giana.service.SupabaseHighScoreService;

public class SupabaseHighScoreServiceTest {

    private HeadlessApplication app;

    /**
     * Name of the libGDX Preferences the service persists to. Mirrors
     * SupabaseHighScoreService.PREFS (package-private in another package, so we
     * repeat the literal here). The headless backend stores these on disk
     * (~/.prefs/giana-highscores), so without clearing them a saved best from a
     * prior run leaks into the next, breaking test isolation.
     */
    private static final String PREFS = "giana-highscores";

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, config);
        clearPrefs();
    }

    @After
    public void tearDown() {
        clearPrefs();
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    /** Wipes the on-disk Preferences so each test starts and ends with a clean slate. */
    private static void clearPrefs() {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        prefs.clear();
        prefs.flush();
    }

    /**
     * Reproduces the launcher lifecycle: both launchers construct the service in
     * main()/onCreate(), BEFORE libGDX populates the static Gdx.* handles. The
     * service constructor must therefore not touch Gdx. Here we null Gdx.app and
     * Gdx.files to mimic that pre-init moment, construct, then restore.
     */
    @Test
    public void noArgConstructor_doesNotTouchGdx() {
        Application savedApp = Gdx.app;
        Files savedFiles = Gdx.files;
        try {
            Gdx.app = null;
            Gdx.files = null;
            new SupabaseHighScoreService(); // must NOT throw
        } finally {
            Gdx.app = savedApp;
            Gdx.files = savedFiles;
        }
    }

    /**
     * After the pre-init construction, once Gdx is available the service must
     * work: no config => offline mode, empty lists, prompt allowed, best saved.
     */
    @Test
    public void worksAfterGdxRestored_offlineMode() {
        Application savedApp = Gdx.app;
        Files savedFiles = Gdx.files;
        SupabaseHighScoreService svc;
        try {
            Gdx.app = null;
            Gdx.files = null;
            svc = new SupabaseHighScoreService();
        } finally {
            Gdx.app = savedApp;
            Gdx.files = savedFiles;
        }

        // No highscore.properties on the test classpath => offline.
        assertFalse(svc.internetAvailable());
        // Fewer than the limit of cached today's scores => any score qualifies.
        assertTrue(svc.goodForHighScores(10));
        assertNull(svc.getMyBest());

        svc.fetchHighScores();
        svc.fetchTodaysHighScores(true);
        assertEquals(0, svc.getScoreUpdate().size());
        assertEquals(0, svc.getTodaysScoreUpdate().size());

        // saveHighScore must persist a personal best without throwing offline.
        svc.saveHighScore(new Score("Tester", 4242, 5));
        Score best = svc.getMyBest();
        assertEquals("Tester", best.getName());
        assertEquals(4242, best.getScore());
    }
}
