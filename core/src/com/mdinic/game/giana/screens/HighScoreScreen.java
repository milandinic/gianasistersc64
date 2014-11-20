package com.mdinic.game.giana.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.mdinic.game.giana.service.Score;

public class HighScoreScreen extends GianaSistersScreen {

    SpriteBatch batch;

    private BitmapFont whileFont;
    private int fontSize;
    OrthographicCamera cam;

    private float time = 0;

    List<Score> scores = new ArrayList<Score>();

    private BitmapFont redFont;

    public HighScoreScreen(Game game) {
        super(game);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, 320);

        cam = new OrthographicCamera();
        this.cam.setToOrtho(false);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        fontSize = Gdx.graphics.getWidth() / GameScreen.SCREEN_WIDTH * 12;

        parameter.size = fontSize;
        whileFont = generator.generateFont(parameter);
        whileFont.setColor(new Color(1, 1, 1, 1));

        redFont = generator.generateFont(parameter);
        redFont.setColor(new Color(0.66f, 0.21f, 0.14f, 1));

        getGame().getHighScoreService().fetchHighScores();
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.cam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        cam.update();

        batch.begin();
        batch.setProjectionMatrix(cam.combined);

        if (getGame().getHighScoreService().haveScoreUpdate()) {
            scores = getGame().getHighScoreService().getScoreUpdate();
        }

        whileFont.draw(batch, "ALL TIME GREATEST              STAGE", Gdx.graphics.getWidth() / 4,
                Gdx.graphics.getHeight() - fontSize * 5 - fontSize);

        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);

            String name = String.format("%-22s", score.getName());
            String f = String.format("%02d. %07d %s %02d", i + 1, score.getScore(), name.substring(0, 22),
                    score.getLevel());
            redFont.draw(batch, f, Gdx.graphics.getWidth() / 4, Gdx.graphics.getHeight() - fontSize * 9 - fontSize * 2
                    * i);

        }

        if (time > 1 && (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched()))
            game.setScreen(new GameScreen(game, 1));
        else if (time >= 10) {
            game.setScreen(new IntroScreen(game));
        }

        batch.end();
    }

    @Override
    public void hide() {
        Gdx.app.debug("HighScoreScreen", "dispose intro");
        batch.dispose();
    }

}
