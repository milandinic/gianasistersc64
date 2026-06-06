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
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.mdinic.game.giana.service.Score;
import com.mdinic.game.giana.service.SupabaseHighScoreService;

public class SupabaseHighScoreServiceTest {

    private HeadlessApplication app;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, config);
    }

    @After
    public void tearDown() {
        if (app != null) {
            app.exit();
            app = null;
        }
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
