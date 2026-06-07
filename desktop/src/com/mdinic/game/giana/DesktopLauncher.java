package com.mdinic.game.giana;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mdinic.game.giana.service.SupabaseHighScoreService;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Giana Byte");
        config.setWindowedMode(940, 640);
        // The game was tuned at ~60 FPS. LWJGL3 otherwise renders at the
        // monitor's refresh rate (e.g. 165 Hz), which speeds up any remaining
        // frame-coupled logic. Cap to 60 with vsync so the desktop feel matches
        // the original; the game logic is also being made delta-independent so
        // it stays correct at any rate (e.g. on high-refresh phones).
        config.useVsync(true);
        config.setForegroundFPS(60);
        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new SupabaseHighScoreService());
        gianaSistersC64.setSettingsService(new SettingsServiceDesktop());
        gianaSistersC64.setGeneralService(new GeneralServiceDesktop());
        new Lwjgl3Application(gianaSistersC64, config);
    }
}
