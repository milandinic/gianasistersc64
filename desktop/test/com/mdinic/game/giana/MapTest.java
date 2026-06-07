package com.mdinic.game.giana;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.mdinic.game.giana.screens.GianaSistersScreen;

public class MapTest {

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

    @Test
    public void testRenderLevels() {
        for (int i = 0; i <= GianaSistersScreen.LEVEL_COUNT; i++) {
            new GameMap(i, null);
        }
    }
}
