package com.mdinic.game.giana.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.Sounds;
import com.mdinic.game.giana.Sounds.BgMusic;
import com.mdinic.game.giana.service.Score;

public class HighScoreScreen extends GianaSistersScreen {

    SpriteBatch batch;

    private float time = 0;

    List<Score> scores = new ArrayList<Score>();
    List<Score> todaysScores = new ArrayList<Score>();

    Sounds sounds;

    public HighScoreScreen(Game game, Sounds sounds, MapRenderer renderer) {
        super(game, renderer);
        this.sounds = sounds;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, 320);

        getGame().getHighScoreService().fetchHighScores();
        getGame().getHighScoreService().fetchTodaysHighScores(true);

        sounds.play(BgMusic.HIGHSCORES);
        sounds.getCurrentMusic().setLooping(true);
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

        renderer.whiteFont10.draw(batch, "ALL TIME GREATEST                STAGE", 50, 280);

        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);

            String name = String.format("%-22s", score.getName());
            String f = String.format(" %d. %07d %s", i + 1, score.getScore(), name.substring(0, 22));
            renderer.redFont10.draw(batch, f, 50, 260 - i * 15);

            String level = String.format("%02d", score.getLevel());
            renderer.redFont10.draw(batch, level, 400, 260 - i * 15);
        }

        renderer.whiteFont10.draw(batch, "TODAYS GREATEST                  STAGE", 50, 265 - 6 * 15);

        for (int i = 0; i < todaysScores.size(); i++) {
            Score score = todaysScores.get(i);

            String name = String.format("%-22s", score.getName());
            String f = String.format(" %d. %07d %s", i + 1, score.getScore(), name.substring(0, 22));

            renderer.redFont10.draw(batch, f, 50, 260 - (i + 7) * 15);

            String level = String.format("%02d", score.getLevel());
            renderer.redFont10.draw(batch, level, 400, 260 - (i + 7) * 15);
        }

        if (time > 1 && (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched())) {
            game.setScreen(new IntroScreen(game, sounds, renderer));
            return;
        }

        batch.end();
    }

    @Override
    public void resume() {
        super.resume();
        Music music = sounds.getCurrentMusic();
        if (music != null) {
            music.play();
        }
    }

    @Override
    public void pause() {
        super.pause();
        Music music = sounds.getCurrentMusic();
        if (music != null) {
            music.pause();
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("HighScoreScreen", "dispose intro");
        batch.dispose();
        sounds.stop(BgMusic.HIGHSCORES);
    }
}
