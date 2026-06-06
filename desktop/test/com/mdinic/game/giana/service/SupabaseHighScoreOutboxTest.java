package com.mdinic.game.giana.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 * Outbox behavior of {@link SupabaseHighScoreService}, exercising the
 * package-private seam ({@code readOutbox}, {@code writeOutbox}, the
 * config+prefs constructor). Lives in the {@code service} package so it can
 * reach those members.
 *
 * Guards two poison-pill fixes:
 * <ul>
 * <li>An offline (unconfigured) save must NOT enqueue an unsignable entry.</li>
 * <li>{@code flushOutbox} must DROP an empty-signature head rather than retry it
 * forever (self-heal for queues written by older builds).</li>
 * </ul>
 */
public class SupabaseHighScoreOutboxTest {

    private HeadlessApplication app;
    private Preferences prefs;

    private static final SupabaseConfig OFFLINE = new SupabaseConfig("", "", "", "");
    private static final SupabaseConfig ONLINE = new SupabaseConfig("https://x.supabase.co", "anon",
            "https://x.functions.supabase.co", "s3cr3t");

    @Before
    public void setUp() {
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
        prefs = Gdx.app.getPreferences("giana-highscores-outbox-test");
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

    /** Offline save records the local best but must not queue an unsignable entry. */
    @Test
    public void offlineSave_doesNotEnqueue() {
        SupabaseHighScoreService svc = new SupabaseHighScoreService(OFFLINE, prefs);

        svc.saveHighScore(new Score("MILAN", 375, 1));

        assertEquals("MILAN", svc.getMyBest().getName());
        assertTrue("offline save must not enqueue", svc.readOutbox().isEmpty());
    }

    /** An empty-sig head (poison from an older build) is dropped, unblocking the queue. */
    @Test
    public void flushOutbox_dropsEmptySigHead() {
        SupabaseHighScoreService svc = new SupabaseHighScoreService(ONLINE, prefs);

        // Two poison entries (no signature) followed by a validly-signed one.
        java.util.ArrayList<PendingSubmit> seed = new java.util.ArrayList<PendingSubmit>();
        seed.add(new PendingSubmit("MILAN", 375, 1, 1L, ""));
        seed.add(new PendingSubmit("MILAN", 375, 1, 2L, ""));
        seed.add(new PendingSubmit("MILAN", 550, 1, 3L, "abc123"));
        svc.writeOutbox(seed);

        svc.flushOutbox();

        // Both empty-sig heads are dropped synchronously; only the signed entry
        // remains (its POST is async and left in place by the test's no-network).
        List<PendingSubmit> remaining = svc.readOutbox();
        assertEquals(1, remaining.size());
        assertEquals("abc123", remaining.get(0).sig);
        assertEquals(550, remaining.get(0).score);
    }
}
