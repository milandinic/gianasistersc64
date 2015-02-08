package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.service.Score;

public class EnterYourNameScreen extends GianaSistersScreen {

    private final GameMap oldMap;

    SpriteBatch batch;

    TextureRegion left;
    TextureRegion right;

    private boolean processKeys = true;

    public EnterYourNameScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {

        Texture texture = new Texture(Gdx.files.internal("data/scoresprites.png"));
        TextureRegion[] buttons = TextureRegion.split(texture, 64, 64)[0];
        right = buttons[0];
        left = buttons[1];

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        batch.draw(left, 0, 0);

        renderer.yellowFont12.draw(batch, "    CONGRATULATION!", 100, 220);
        renderer.yellowFont12.draw(batch, "YOU CAN TYPE YOUR NAME", 100, 200);
        renderer.yellowFont12.draw(batch, "  IN THE HALL OF FAME", 100, 180);
        renderer.yellowFont12.draw(batch, String.format("   YOUR SCORE %07d", oldMap.score), 100, 140);

        batch.draw(right, 480 - 64, 0);
        batch.end();

        processKeys();
    }

    private void processKeys() {
        float x0 = (Gdx.input.getX(0) / (float) Gdx.graphics.getWidth()) * 480;
        float x1 = (Gdx.input.getX(1) / (float) Gdx.graphics.getWidth()) * 480;
        float y0 = 320 - (Gdx.input.getY(0) / (float) Gdx.graphics.getHeight()) * 320;

        boolean leftButton = (Gdx.input.isTouched(0) && x0 < 70) || (Gdx.input.isTouched(1) && x1 < 70);

        boolean rightButton = (Gdx.input.isTouched(0) && x0 > 416 && x0 < 480 && y0 < 64)
                || (Gdx.input.isTouched(1) && x1 > 416 && x1 < 480 && y0 < 64);

        if (leftButton && processKeys) {
            processKeys = false;
            Score score = getGame().getHighScoreService().getMyBest();

            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override
                public void input(String text) {
                    if (!text.isEmpty()) {
                        Score score = new Score(text, oldMap.score, oldMap.level + 1);
                        getGame().getHighScoreService().saveHighScore(score);
                    }
                    game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
                }

                @Override
                public void canceled() {
                    game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
                }
            }, "", score != null ? score.getName() : "");

        } else if (rightButton && processKeys) {
            game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
        }
    }

    @Override
    public void hide() {
        left.getTexture().dispose();
        right.getTexture().dispose();
        batch.dispose();
    }

}
