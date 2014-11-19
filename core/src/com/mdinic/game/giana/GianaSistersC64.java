package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.GameScreen;
import com.mdinic.game.giana.service.HighScoreService;

public class GianaSistersC64 extends Game {

    private HighScoreService highScoreService;

    @Override
    public void create() {
        setScreen(new GameScreen(this, 1));
    }

    public HighScoreService getHighScoreService() {
        return highScoreService;
    }

    public void setHighScoreService(HighScoreService highScoreService) {
        this.highScoreService = highScoreService;
    }

}
