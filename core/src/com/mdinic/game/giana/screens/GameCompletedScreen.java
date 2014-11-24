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

public class GameCompletedScreen extends GianaSistersScreen {

    private int fontSize;
    private BitmapFont yellowFont;
    private float time = 0;
    private SpriteBatch batch;
    private OrthographicCamera cam;
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
        yellowFont.draw(batch, "    GIANA GET UP", x, Gdx.graphics.getHeight() / 2 + fontSize * 8);
        yellowFont.draw(batch, "THE SUN HAS FRIGHTENED", x, Gdx.graphics.getHeight() / 2 + fontSize * 6);
        yellowFont.draw(batch, "    OFF THE NIGHT", x, Gdx.graphics.getHeight() / 2 + fontSize * 4);

        batch.end();

        if (time > 5) {
            game.setScreen(new EnterYourNameScreen(game, oldMap));
        }
    }

    @Override
    public void hide() {
        batch.dispose();
        yellowFont.dispose();

    }
}