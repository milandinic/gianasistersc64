package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.Sounds;
import com.mdinic.game.giana.Sounds.Sfx;

public class LevelOverScreen extends GianaSistersScreen {

    private BitmapFont yellowFont;
    private float time = 0;
    private SpriteBatch batch;
    private final Map oldMap;

    public LevelOverScreen(Game game, Map oldMap) {
        super(game);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        parameter.size = 10;
        yellowFont = generator.generateFont(parameter);
        yellowFont.setColor(new Color(0.87f, 0.95f, 0.47f, 1));
        generator.dispose();
        Sounds.getInstance().play(Sfx.END_LEVEL);
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        yellowFont.draw(batch, "TIME    BONUS   SCORE", 100, 180);

        if (oldMap.time > 0) {
            oldMap.score += 10;
            oldMap.time--;
        } else {
            time += delta;
        }

        String update = String.format(" %02d   x  10      %06d", oldMap.time, oldMap.score);

        yellowFont.draw(batch, update, 100, 160);

        batch.end();

        if (time > 3) {
            if (oldMap.level + 1 == LEVEL_COUNT) {
                game.setScreen(new GameCompletedScreen(game, oldMap));
            } else {
                game.setScreen(new LevelStartingScreen(game, oldMap));
            }
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose intro");
        batch.dispose();
        yellowFont.dispose();
    }
}
