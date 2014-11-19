package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.mdinic.game.giana.GianaSistersC64;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.OnscreenControlRenderer;

public class GameScreen extends GianaSistersScreen {
    Map map;
    MapRenderer renderer;
    OnscreenControlRenderer controlRenderer;
    int level;

    public GameScreen(Game game, int level) {
        super(game);
        this.level = level;
    }

    @Override
    public void show() {
        map = new Map(level);
        renderer = new MapRenderer(map);
        controlRenderer = new OnscreenControlRenderer(map);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());
        map.update(delta);

        map.time = 99 - (int) map.giana.stateTime;
        Gdx.gl.glClearColor(map.r, map.g, map.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(delta);
        controlRenderer.render();

        if (map.giana.bounds.overlaps(map.endDoor.bounds)) {
            if (level == 0) {
                game.setScreen(new IntroScreen(game));
            } else {
                game.setScreen(new LevelOverScreen(game, level + 1));
            }
        }

        if (map.lives == 0) {
            game.setScreen(new GameOverScreen(game));
        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            game.setScreen(new IntroScreen(game));
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose game screen");
        renderer.dispose();
        controlRenderer.dispose();
    }

    public GianaSistersC64 getGame() {
        return (GianaSistersC64) game;
    }
}
