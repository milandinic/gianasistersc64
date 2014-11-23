package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.mdinic.game.giana.GianaSistersC64;

public abstract class GianaSistersScreen implements Screen {

    protected final Game game;

    public static final int SCREEN_WIDTH = 480;
    public static final int LEVEL_COUNT = 7;

    public GianaSistersScreen(Game game) {
        this.game = game;
    }

    public GianaSistersC64 getGame() {
        return (GianaSistersC64) game;
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }
}
