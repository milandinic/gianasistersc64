package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.GameScreen;

public class GianaSistersC64 extends Game {
    @Override
    public void create() {
        setScreen(new GameScreen(this, 0));
    }
}
