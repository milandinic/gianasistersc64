package com.mdinic.game.giana;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Frame-coupled motion (position += constant per frame) runs faster at higher
 * frame rates. These tests drive the same WALL-CLOCK duration at two frame
 * rates and assert the resulting motion matches, proving the logic is
 * delta-scaled rather than per-frame. Surfaced after the libGDX 1.14.2 bump,
 * which uncapped FPS on high-refresh displays.
 */
public class FrameRateIndependenceTest {

    private HeadlessApplication app;

    @Before
    public void setUp() {
        app = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
    }

    @After
    public void tearDown() {
        if (app != null) {
            app.exit();
            app = null;
        }
    }

    /**
     * The intro demo auto-walks Giana right. Over one simulated second she must
     * travel the same distance at 60 FPS and at 165 FPS.
     */
    @Test
    public void demoWalk_sameDistanceAcrossFrameRates() {
        float at60 = demoWalkDistance(60);
        float at165 = demoWalkDistance(165);
        // Allow a tiny tolerance for delta rounding; pre-fix this differs ~2.75x.
        assertEquals(at60, at165, 0.05f);
    }

    /** Simulates 1 second of intro demo walk at the given FPS, returns distance. */
    private float demoWalkDistance(int fps) {
        GameMap map = new GameMap(0, null);
        map.demo = true;
        float startX = map.giana.bounds.x;
        float dt = 1f / fps;
        for (int i = 0; i < fps; i++) {
            map.giana.update(dt);
        }
        return map.giana.bounds.x - startX;
    }
}
