package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class LevelOverScreen extends GianaSistersScreen {
    TextureRegion intro;
    SpriteBatch batch;
    float time = 0;
    int nextLevel;

    static int LEVEL_COUNT = 2;

    public LevelOverScreen(Game game, int nextLevel) {
        super(game);
        this.nextLevel = nextLevel;
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
                if (nextLevel == LEVEL_COUNT) {
                    game.setScreen(new GameOverScreen(game));
                } else {
                    game.setScreen(new GameScreen(game, nextLevel));
                }
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
