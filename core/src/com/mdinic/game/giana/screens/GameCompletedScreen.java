package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;

public class GameCompletedScreen extends GianaSistersScreen {

    private static final int TEXT_X = 140;

    private float time = 0;
    private SpriteBatch batch;

    private final GameMap oldMap;

    public GameCompletedScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        renderer.yellowFont10.draw(batch, "    GIANA GET UP", TEXT_X, 220);
        renderer.yellowFont10.draw(batch, "THE SUN HAS FRIGHTENED", TEXT_X, 200);
        renderer.yellowFont10.draw(batch, "    OFF THE NIGHT", TEXT_X, 180);

        batch.end();

        if (time > 5) {
            if (getGame().getHighScoreService().goodForHighScores(oldMap.score)) {
                oldMap.level--;
                game.setScreen(new EnterYourNameScreen(game, oldMap, renderer));
            } else {
                game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
            }
        }
    }

    @Override
    public void hide() {
        batch.dispose();
    }
}
