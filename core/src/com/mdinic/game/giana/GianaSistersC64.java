package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.IntroScreen;
import com.mdinic.game.giana.service.GeneralService;
import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.SettingsService;

public class GianaSistersC64 extends Game {

    private HighScoreService highScoreService;
    private SettingsService settingsService;
    private GeneralService generalService;
    Sounds sounds;
    MapRenderer renderer;

    public GianaSistersC64() {
        super();
    }

    @Override
    public void create() {
        sounds = new Sounds();
        renderer = new MapRenderer();
        // setScreen(new GameScreen(this, new GameMap(27, sounds), renderer));
        setScreen(new IntroScreen(this, sounds, renderer));
    }

    @Override
    public void dispose() {
        super.dispose();
        sounds.dispose();
        renderer.dispose();

        sounds = null;
        renderer = null;
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

    public GeneralService getGeneralService() {
        return generalService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
