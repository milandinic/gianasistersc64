package com.mdinic.game.giana;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Reproduces the {@link ArrayIndexOutOfBoundsException} at Bee.update: a bee
 * killed near the bottom of the map keeps falling (vel.y = -8) because dead bees
 * are not alive-guarded in {@code GameMap.update}. Once {@code pos.y} drops below
 * 0, the row index {@code tiles[0].length - 1 - (int) pos.y} runs past the array
 * length and the JVM throws. Surfaced after the libGDX 1.14.2 bump.
 */
public class BeeFallOffMapTest {

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

    /** A dead bee falling off the bottom of the map must not crash. */
    @Test
    public void deadBeeFallingOffBottom_doesNotThrow() {
        // Level 9 is the first that contains bees (see git history). Any level
        // works; we drive the bee directly rather than relying on placement.
        GameMap map = new GameMap(9, null);

        Bee bee = new Bee(map, 5f, 0.5f);
        bee.alive = false; // killed by Giana
        bee.vel.set(0, -8); // falling

        // Drive enough frames to push pos.y below 0 (off the bottom).
        for (int i = 0; i < 60; i++) {
            bee.update(0.06f);
        }
    }
}
