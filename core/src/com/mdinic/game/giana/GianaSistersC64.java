package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.IntroScreen;

public class GianaSistersC64 extends Game {
    @Override
    public void create() {
        setScreen(new IntroScreen(this));
    }
}
