package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.service.Score;

/**
 * Lets a qualifying player type their name into the hall of fame.
 *
 * The pen (left) opens an in-game text field; the fast-forward (right) skips to
 * the high-score list. We render our own text field rather than calling
 * {@code Gdx.input.getTextInput(...)} because that method is not implemented on
 * the LWJGL3 desktop backend (GLFW and AWT can't coexist) — it would fire
 * {@code canceled()} immediately. A custom field driven by libGDX key events is
 * the cross-platform way and works the same on desktop and Android.
 */
public class EnterYourNameScreen extends GianaSistersScreen {

    private static final int MAX_NAME = 12;
    private static final float CURSOR_BLINK = 0.4f;

    private final GameMap oldMap;

    SpriteBatch batch;

    TextureRegion left;
    TextureRegion right;

    private boolean editing = false;
    private final StringBuilder name = new StringBuilder();
    private float blinkTime = 0;
    /** True while a touch is held, so one tap fires at most one action. */
    private boolean touchConsumed = false;
    /** Latch so the screen leaves (and submits) at most once. */
    private boolean submitted = false;

    public EnterYourNameScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {
        super.show();
        Texture texture = new Texture(Gdx.files.internal("data/scoresprites.png"));
        TextureRegion[] buttons = TextureRegion.split(texture, 64, 64)[0];
        right = buttons[0];
        left = buttons[1];

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (!editing) {
                    return false;
                }
                if (character == '\b') { // backspace
                    if (name.length() > 0) {
                        name.deleteCharAt(name.length() - 1);
                    }
                    return true;
                }
                if (character == '\r' || character == '\n') {
                    // Enter is handled in keyDown(); swallow it here so the
                    // newline isn't appended, but don't confirm twice.
                    return true;
                }
                char c = Character.toUpperCase(character);
                boolean allowed = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ';
                if (allowed && name.length() < MAX_NAME) {
                    name.append(c);
                }
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (!editing) {
                    return false;
                }
                if (keycode == Keys.ENTER) {
                    confirm();
                    return true;
                }
                if (keycode == Keys.ESCAPE || keycode == Keys.BACK) {
                    skip();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        blinkTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        batch.draw(left, 0, 0);

        renderer.yellowFont12.draw(batch, "    CONGRATULATION!", 100, 220);
        renderer.yellowFont12.draw(batch, "YOU CAN TYPE YOUR NAME", 100, 200);
        renderer.yellowFont12.draw(batch, "  IN THE HALL OF FAME", 100, 180);
        renderer.yellowFont12.draw(batch, String.format("   YOUR SCORE %07d", oldMap.score), 100, 140);

        if (editing) {
            boolean cursorOn = ((int) (blinkTime / CURSOR_BLINK)) % 2 == 0;
            String shown = "NAME: " + name + (cursorOn ? "_" : " ");
            renderer.yellowFont12.draw(batch, shown, 100, 100);
        }

        batch.draw(right, 480 - 64, 0);
        batch.end();

        processTouches();
    }

    private void processTouches() {
        boolean touched = Gdx.input.isTouched(0) || Gdx.input.isTouched(1);
        if (!touched) {
            touchConsumed = false;
            return;
        }
        if (touchConsumed) {
            return; // already handled this press
        }
        touchConsumed = true;

        float x0 = (Gdx.input.getX(0) / (float) Gdx.graphics.getWidth()) * 480;
        float x1 = (Gdx.input.getX(1) / (float) Gdx.graphics.getWidth()) * 480;
        float y0 = 320 - (Gdx.input.getY(0) / (float) Gdx.graphics.getHeight()) * 320;

        boolean leftButton = (Gdx.input.isTouched(0) && x0 < 70) || (Gdx.input.isTouched(1) && x1 < 70);
        boolean rightButton = (Gdx.input.isTouched(0) && x0 > 416 && x0 < 480 && y0 < 64)
                || (Gdx.input.isTouched(1) && x1 > 416 && x1 < 480 && y0 < 64);

        if (rightButton) {
            skip();
        } else if (leftButton) {
            if (editing) {
                confirm(); // tapping the pen again while editing confirms
            } else {
                startEditing();
            }
        }
    }

    private void startEditing() {
        editing = true;
        blinkTime = 0;
        name.setLength(0);
        Score best = getGame().getHighScoreService().getMyBest();
        if (best != null && best.getName() != null) {
            String prev = best.getName().trim();
            name.append(prev.length() > MAX_NAME ? prev.substring(0, MAX_NAME) : prev);
        }
        Gdx.input.setOnscreenKeyboardVisible(true);
    }

    private void confirm() {
        // setScreen() only takes effect next frame, so without this latch a
        // second trigger in the same frame (e.g. keyTyped + keyDown both firing
        // for one Enter, or a held key) would submit the score twice, landing
        // duplicate leaderboard rows that differ only by a few ms in ts.
        if (submitted) {
            return;
        }
        submitted = true;
        Gdx.input.setOnscreenKeyboardVisible(false);
        String typed = name.toString().trim();
        if (!typed.isEmpty()) {
            getGame().getHighScoreService().saveHighScore(new Score(typed, oldMap.score, oldMap.level + 1));
        }
        game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
    }

    private void skip() {
        if (submitted) {
            return;
        }
        submitted = true;
        Gdx.input.setOnscreenKeyboardVisible(false);
        game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
    }

    @Override
    public void hide() {
        super.hide();
        Gdx.input.setInputProcessor(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
        left.getTexture().dispose();
        right.getTexture().dispose();
        batch.dispose();
    }

}
