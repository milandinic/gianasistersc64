package com.mdinic.game.giana.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.mdinic.game.giana.service.HighScoreListener;
import com.mdinic.game.giana.service.Score;

public class HighScoreScreen extends GianaSistersScreen implements HighScoreListener {

    SpriteBatch batch;

    private BitmapFont font;
    private int fontSize;
    OrthographicCamera scoreCam;

    List<Score> scores = new ArrayList<Score>();

    Object object = new Object();

    public HighScoreScreen(Game game) {
        super(game);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);

        scoreCam = new OrthographicCamera();
        this.scoreCam.setToOrtho(false);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        System.out.println(Gdx.graphics.getWidth());
        fontSize = Gdx.graphics.getWidth() / 640 * 12; // font size 12

        parameter.size = fontSize;
        font = generator.generateFont(parameter);
        font.setColor(new Color(0xFFFFFF));

        getGame().getHighScoreService().getHighScores(this);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.scoreCam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        scoreCam.update();

        batch.begin();
        batch.setProjectionMatrix(scoreCam.combined);

        synchronized (object) {
            for (int i = 0; i < scores.size(); i++) {
                Score score = scores.get(i);
                String formatted = String.format("%07d %s", score.getScore(), score.getName());
                font.draw(batch, formatted, Gdx.graphics.getWidth() / 3, Gdx.graphics.getHeight() - fontSize * 5
                        - fontSize * 2 * i);

            }
        }

        batch.end();
    }

    @Override
    public void hide() {
        Gdx.app.debug("HighScoreScreen", "dispose intro");
        batch.dispose();
    }

    @Override
    public void receiveHighScore(List<Score> scores) {
        synchronized (object) {
            this.scores = scores;
        }
    }
}
