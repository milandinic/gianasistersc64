package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.mdinic.game.giana.GianaState;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.OnscreenControlRenderer;

public class GameScreen extends GianaSistersScreen {
    Map map;
    MapRenderer renderer;
    OnscreenControlRenderer controlRenderer;

    public GameScreen(Game game, int level) {
        super(game);
        map = new Map(level);
    }

    public GameScreen(Game game, Map oldMap) {
        super(game);
        map = new Map(oldMap);
    }

    @Override
    public void show() {
        renderer = new MapRenderer(map);
        controlRenderer = new OnscreenControlRenderer(map);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());
        map.update(delta);

        if (map.giana.state == GianaState.DEAD) {
            map.level--;
            if (map.lives == 0) {
                game.setScreen(new EnterYourNameScreen(game, map));
            } else {
                game.setScreen(new LevelStartingScreen(game, map));
            }

            return;
        }

        map.time = 99 - (int) map.giana.stateTime;
        Gdx.gl.glClearColor(map.r, map.g, map.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(delta);
        if (map.level > 0)
            controlRenderer.render();

        if (map.giana.bounds.overlaps(map.endDoor.bounds)) {
            if (map.level == 0) {
                game.setScreen(new HighScoreScreen(game));
            } else if (map.level + 1 == LEVEL_COUNT) {
                game.setScreen(new GameCompletedScreen(game, map));
            } else {
                game.setScreen(new LevelOverScreen(game, map));
            }
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

}
