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

public class LevelStartingScreen extends GianaSistersScreen {

    private int fontSize;
    private BitmapFont yellowFont;
    private float time = 0;
    private SpriteBatch batch;
    private final Map oldMap;
    private OrthographicCamera cam;

    public LevelStartingScreen(Game game, Map oldMap) {
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

        this.cam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        cam.update();

        MapResource.getInstance().getStartLevelSfx().play();
        generator.dispose();
    }

    @Override
    public void render(float delta) {
        time += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.setProjectionMatrix(cam.combined);

        int x = Gdx.graphics.getWidth() / 2 - fontSize * 4;
        yellowFont.draw(batch, "  GIANA", x, Gdx.graphics.getHeight() / 2 + fontSize * 8);
        yellowFont.draw(batch, "GET READY", x, Gdx.graphics.getHeight() / 2 + fontSize * 6);
        String stage = String.format(" STAGE %02d", oldMap.level + 1);
        yellowFont.draw(batch, stage, x, Gdx.graphics.getHeight() / 2 + fontSize * 4);

        batch.end();

        if (time > 4) {
            oldMap.level++;
            MapResource.getInstance().getStartLevelSfx().stop();
            game.setScreen(new GameScreen(game, oldMap));
        }
    }

    @Override
    public void hide() {
        batch.dispose();
    }
}
