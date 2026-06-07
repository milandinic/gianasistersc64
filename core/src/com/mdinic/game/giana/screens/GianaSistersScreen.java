package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.mdinic.game.giana.GianaSistersC64;
import com.mdinic.game.giana.MapRenderer;

public abstract class GianaSistersScreen implements Screen {

    protected final Game game;
    protected final MapRenderer renderer;

    public static final int SCREEN_WIDTH = 480;
    public static final int SCREEN_HEIGHT = 320;
    public static final int LEVEL_COUNT = 30;

    public GianaSistersScreen(Game game, MapRenderer renderer) {
        this.game = game;
        this.renderer = renderer;
    }

    public GianaSistersC64 getGame() {
        return (GianaSistersC64) game;
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
        // Subclasses override and call super.show() first, so this logs every
        // screen load with the concrete screen's class name as the tag.
        Gdx.app.debug(getClass().getSimpleName(), "show (load)");
    }

    @Override
    public void hide() {
        Gdx.app.debug(getClass().getSimpleName(), "hide (dispose)");
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
