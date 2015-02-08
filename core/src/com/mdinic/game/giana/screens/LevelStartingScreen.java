package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.Sounds.BgMusic;

public class LevelStartingScreen extends GianaSistersScreen {

    private static final int TEXT_X = 200;
    private float time = 0;
    private SpriteBatch batch;
    private final GameMap oldMap;

    public LevelStartingScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        oldMap.sounds.play(BgMusic.START_LEVEL);
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        renderer.yellowFont10.draw(batch, "  GIANA", TEXT_X, 220);
        renderer.yellowFont10.draw(batch, "GET READY", TEXT_X, 200);
        String stage = String.format(" STAGE %02d", oldMap.level + 1);
        renderer.yellowFont10.draw(batch, stage, TEXT_X, 180);

        batch.end();

        if (time > 4) {
            oldMap.level++;
            game.setScreen(new GameScreen(game, oldMap, renderer));
        }
    }

    @Override
    public void hide() {
        batch.dispose();
    }
}
