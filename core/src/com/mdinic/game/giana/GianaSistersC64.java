package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.IntroScreen;
import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.SettingsService;

public class GianaSistersC64 extends Game {

    private HighScoreService highScoreService;
    private SettingsService settingsService;

    public GianaSistersC64() {
        super();
    }

    @Override
    public void create() {
        setScreen(new IntroScreen(this));
    }

    public HighScoreService getHighScoreService() {
        return highScoreService;
    }

    public void setHighScoreService(HighScoreService highScoreService) {
        this.highScoreService = highScoreService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

}
