package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.service.Score;

public class GameOverScreen extends GianaSistersScreen {
    TextureRegion intro;
    SpriteBatch batch;
    float time = 0;

    Map oldMap;

    public GameOverScreen(Game game, Map oldMap) {
        super(game);
        this.oldMap = oldMap;

        getGame().getHighScoreService().saveHighScore(
                new Score(getGame().getHighScoreService().getUsername(), oldMap.score));
    }

    @Override
    public void show() {
        intro = new TextureRegion(new Texture(Gdx.files.internal("data/gameover.png")), 0, 0, 480, 320);
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(intro, 0, 0);
        batch.end();

        time += delta;
        if (time > 1) {
            if (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched()) {
                game.setScreen(new IntroScreen(game));
            }
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose intro");
        batch.dispose();
        intro.getTexture().dispose();
    }
}
