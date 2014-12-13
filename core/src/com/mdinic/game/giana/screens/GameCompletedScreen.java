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

public class GameCompletedScreen extends GianaSistersScreen {

    private static final int TEXT_X = 140;
    private BitmapFont yellowFont;
    private float time = 0;
    private SpriteBatch batch;

    private final Map oldMap;

    static int LEVEL_COUNT = 4;

    public GameCompletedScreen(Game game, Map oldMap) {
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
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        yellowFont.draw(batch, "    GIANA GET UP", TEXT_X, 220);
        yellowFont.draw(batch, "THE SUN HAS FRIGHTENED", TEXT_X, 200);
        yellowFont.draw(batch, "    OFF THE NIGHT", TEXT_X, 180);

        batch.end();

        if (time > 5) {
            if (getGame().getHighScoreService().goodForHighScores(oldMap.score)) {
                game.setScreen(new EnterYourNameScreen(game, oldMap));
            } else {
                game.setScreen(new LevelStartingScreen(game, oldMap));
            }
        }
    }

    @Override
    public void hide() {
        batch.dispose();
        yellowFont.dispose();

    }
}
