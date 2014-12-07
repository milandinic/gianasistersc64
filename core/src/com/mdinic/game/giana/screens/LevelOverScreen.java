package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.MapResource;

public class LevelOverScreen extends GianaSistersScreen {

    private int fontSize;
    private BitmapFont yellowFont;
    private float time = 0;
    private SpriteBatch batch;
    private final Map oldMap;
    private OrthographicCamera cam;

    public LevelOverScreen(Game game, Map oldMap) {
        super(game);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);

        cam = new OrthographicCamera();
        this.cam.setToOrtho(false);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        fontSize = Gdx.graphics.getWidth() / SCREEN_WIDTH * 12; // font size 12

        parameter.size = fontSize;
        yellowFont = generator.generateFont(parameter);
        yellowFont.setColor(new Color(0.87f, 0.95f, 0.47f, 1));
        generator.dispose();
        MapResource.getInstance().getEndLevelSfx().play();
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.cam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        cam.update();

        batch.begin();
        batch.setProjectionMatrix(cam.combined);

        int x = Gdx.graphics.getWidth() / 4;
        yellowFont.draw(batch, "TIME    BONUS   SCORE", x, Gdx.graphics.getHeight() - fontSize * 6);

        if (oldMap.time > 0) {
            oldMap.score += 10;
            oldMap.time--;
        } else {
            time += delta;
        }

        String update = String.format(" %02d   x  10      %06d", oldMap.time, oldMap.score);

        yellowFont.draw(batch, update, x, Gdx.graphics.getHeight() - fontSize * 9);

        batch.end();

        if (time > 3) {
            MapResource.getInstance().getEndLevelSfx().stop();
            game.setScreen(new LevelStartingScreen(game, oldMap));
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose intro");
        batch.dispose();
        yellowFont.dispose();
    }
}
