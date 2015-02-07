package com.mdinic.game.giana;

import org.junit.Test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFiles;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.mdinic.game.giana.screens.GianaSistersScreen;

public class MapTest {

    @Test
    public void testRenderLevels() {
        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new HighScoreServiceDesktop());
        gianaSistersC64.setSettingsService(new SettingsServiceDesktop());

        LwjglNativesLoader.load();

        Sounds sounds = new Sounds();

        Gdx.files = new LwjglFiles();

        for (int i = 0; i <= GianaSistersScreen.LEVEL_COUNT; i++) {
            GameMap map = new GameMap(i, sounds);
        }

    }
}
