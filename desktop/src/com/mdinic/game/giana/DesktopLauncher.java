package com.mdinic.game.giana;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new HighScoreServiceDesktop());
        gianaSistersC64.setSettingsService(new SettingsServiceDesktop());
        gianaSistersC64.setGeneralService(new GeneralServiceDesktop());
        new LwjglApplication(gianaSistersC64, config);
    }
}
