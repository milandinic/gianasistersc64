package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.service.Score;

/**
 * Lets a qualifying player type their name into the hall of fame.
 *
 * Layout is top-anchored: on Android the soft keyboard owns the bottom half of
 * the screen, so the name line and the DONE/SKIP buttons all live in the top
 * band where the keyboard can't cover them — and the buttons are hit-tested in
 * that band only, so tapping a keyboard key never accidentally finishes entry.
 * Editing (and the keyboard) start automatically on {@link #show()}; the player
 * just types. DONE saves the score; SKIP continues without saving.
 *
 * We render our own text field rather than calling
 * {@code Gdx.input.getTextInput(...)} because that method is not implemented on
 * the LWJGL3 desktop backend (GLFW and AWT can't coexist) — it would fire
 * {@code canceled()} immediately. A custom field driven by libGDX key events is
 * the cross-platform way and works the same on desktop and Android.
 */
public class EnterYourNameScreen extends GianaSistersScreen {

    private static final int MAX_NAME = 12;
    private static final float CURSOR_BLINK = 0.4f;

    /**
     * Buttons are only tappable above this virtual-Y line. Everything below is
     * where the Android soft keyboard sits, so keyboard taps can't match a
     * button.
     */
    private static final float BUTTON_BAND_Y = 270;

    private final GameMap oldMap;

    SpriteBatch batch;

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
        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
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

        // Start typing immediately: pre-fill the last-used name and raise the
        // keyboard. There is no separate "tap to start" step any more.
        Score best = getGame().getHighScoreService().getMyBest();
        if (best != null && best.getName() != null) {
            String prev = best.getName().trim();
            name.append(prev.length() > MAX_NAME ? prev.substring(0, MAX_NAME) : prev);
        }
        Gdx.input.setOnscreenKeyboardVisible(true);
    }

    @Override
    public void render(float delta) {
        blinkTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // DONE / SKIP live in the top band, above the Android soft keyboard.
        renderer.yellowFont12.draw(batch, "DONE", 20, 300);
        renderer.yellowFont12.draw(batch, "SKIP", 480 - 70, 300);

        renderer.yellowFont12.draw(batch, "    CONGRATULATION!", 100, 250);
        renderer.yellowFont12.draw(batch, "YOU CAN TYPE YOUR NAME", 100, 230);
        renderer.yellowFont12.draw(batch, "  IN THE HALL OF FAME", 100, 210);
        renderer.yellowFont12.draw(batch, String.format("   YOUR SCORE %07d", oldMap.score), 100, 185);

        boolean cursorOn = ((int) (blinkTime / CURSOR_BLINK)) % 2 == 0;
        String shown = "NAME: " + name + (cursorOn ? "_" : " ");
        renderer.yellowFont12.draw(batch, shown, 100, 155);

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

        // DONE (top-left) and SKIP (top-right) only register in the top band, so
        // a touch on the soft keyboard (bottom of the screen) can never match.
        boolean done = false;
        boolean skip = false;
        // Two pointers because on Android a keyboard touch and a button touch
        // can be down at once; check each pointer against both buttons.
        for (int p = 0; p <= 1; p++) {
            if (!Gdx.input.isTouched(p)) {
                continue;
            }
            float x = (Gdx.input.getX(p) / (float) Gdx.graphics.getWidth()) * 480;
            float y = 320 - (Gdx.input.getY(p) / (float) Gdx.graphics.getHeight()) * 320;
            if (y < BUTTON_BAND_Y) {
                continue; // below the buttons (keyboard area)
            }
            if (x < 100) {
                done = true;
            } else if (x > 380) {
                skip = true;
            }
        }

        if (skip) {
            skip();
        } else if (done) {
            confirm();
        }
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
        batch.dispose();
    }

}
