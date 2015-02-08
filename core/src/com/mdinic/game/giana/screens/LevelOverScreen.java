package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.Sounds.BgMusic;

public class LevelOverScreen extends GianaSistersScreen {

    private float time = 0;
    private float stateTime = 0;
    private SpriteBatch batch;
    private final GameMap oldMap;

    public LevelOverScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        oldMap.sounds.play(BgMusic.END_LEVEL);
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        renderer.yellowFont10.draw(batch, "TIME    BONUS   SCORE", 100, 180);

        if (oldMap.time > 0) {
            oldMap.score += 10;
            oldMap.time--;
        } else {
            time += delta;
        }

        String update = String.format(" %02d   x  10      %06d", oldMap.time, oldMap.score);

        renderer.yellowFont10.draw(batch, update, 100, 160);

        batch.draw(renderer.yellowCristalAnim.getKeyFrame(stateTime, true), 240, 150);

        batch.end();

        if (time > 3) {
            if (oldMap.level == LEVEL_COUNT) {
                game.setScreen(new GameCompletedScreen(game, oldMap, renderer));
            } else {
                game.setScreen(new LevelStartingScreen(game, oldMap, renderer));
            }
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose intro");
        batch.dispose();
    }
}
