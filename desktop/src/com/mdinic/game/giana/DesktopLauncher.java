package com.mdinic.game.giana;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mdinic.game.giana.service.SupabaseHighScoreService;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Giana Byte");
        config.setWindowedMode(940, 640);
        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new SupabaseHighScoreService());
        gianaSistersC64.setSettingsService(new SettingsServiceDesktop());
        gianaSistersC64.setGeneralService(new GeneralServiceDesktop());
        new Lwjgl3Application(gianaSistersC64, config);
    }
}
