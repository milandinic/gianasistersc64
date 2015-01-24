package com.mdinic.game.giana.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.mdinic.game.giana.service.Score;

public class HighScoreScreen extends GianaSistersScreen {

    SpriteBatch batch;

    private BitmapFont whileFont;

    private float time = 0;

    List<Score> scores = new ArrayList<Score>();
    List<Score> todaysScores = new ArrayList<Score>();

    private BitmapFont redFont;

    public HighScoreScreen(Game game) {
        super(game);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, 320);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        parameter.size = 10;
        whileFont = generator.generateFont(parameter);
        whileFont.setColor(new Color(1, 1, 1, 1));

        redFont = generator.generateFont(parameter);
        redFont.setColor(new Color(0.66f, 0.21f, 0.14f, 1));

        getGame().getHighScoreService().fetchHighScores();
        getGame().getHighScoreService().fetchTodaysHighScores(true);

        generator.dispose();
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        if (getGame().getHighScoreService().haveScoreUpdate()) {
            scores = getGame().getHighScoreService().getScoreUpdate();
        }

        if (getGame().getHighScoreService().haveTodaysScoreUpdate()) {
            todaysScores = getGame().getHighScoreService().getTodaysScoreUpdate();
        }

        whileFont.draw(batch, "ALL TIME GREATEST                STAGE", 50, 280);

        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);

            String name = String.format("%-22s", score.getName());
            String f = String.format(" %d. %07d %s", i + 1, score.getScore(), name.substring(0, 22));
            redFont.draw(batch, f, 50, 260 - i * 15);

            String level = String.format("%02d", score.getLevel());
            redFont.draw(batch, level, 400, 260 - i * 15);
        }

        whileFont.draw(batch, "TODAYS GREATEST                  STAGE", 50, 265 - 6 * 15);

        for (int i = 0; i < todaysScores.size(); i++) {
            Score score = todaysScores.get(i);

            String name = String.format("%-22s", score.getName());
            String f = String.format(" %d. %07d %s", i + 1, score.getScore(), name.substring(0, 22));

            redFont.draw(batch, f, 50, 260 - (i + 7) * 15);

            String level = String.format("%02d", score.getLevel());
            redFont.draw(batch, level, 400, 260 - (i + 7) * 15);
        }

        if (time > 1 && (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched())) {
            game.setScreen(new IntroScreen(game));
            return;
        }

        batch.end();
    }

    @Override
    public void hide() {
        Gdx.app.debug("HighScoreScreen", "dispose intro");
        batch.dispose();
        whileFont.dispose();
        redFont.dispose();
    }
}
