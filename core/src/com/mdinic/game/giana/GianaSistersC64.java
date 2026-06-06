package com.mdinic.game.giana;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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
        // libGDX backends default to LOG_INFO, which swallows Gdx.app.debug().
        // Raise to LOG_DEBUG so debug() lines (desktop console / Android logcat)
        // are actually emitted. Set here in create() because it runs once after
        // Gdx.app is live and before any screen shows; the Lwjgl3Application
        // constructor blocks in the render loop, so the launcher is too late.
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
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
