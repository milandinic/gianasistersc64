package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.screens.NameGridLayout.Rect;
import com.mdinic.game.giana.service.Score;

/**
 * Lets a qualifying player type their name into the hall of fame using an
 * in-game, tappable letter grid — never the native soft keyboard.
 *
 * The app is locked to landscape, where the Android soft keyboard covers
 * 60–75% of the screen and would hide the name line no matter how far up we
 * nudge it. So we raise no keyboard at all and draw our own A–Z/0–9 grid plus
 * DEL and SPC cells with {@code yellowFont12}. Selection is direct tap (click
 * on desktop), uniform on both platforms; the grid only offers valid
 * characters, so no character filtering is needed. DONE saves the score; SKIP
 * continues without saving. All geometry lives in the unit-tested
 * {@link NameGridLayout}, so what we draw and what we hit-test can't drift.
 */
public class EnterYourNameScreen extends GianaSistersScreen {

    private static final int MAX_NAME = 12;
    private static final float CURSOR_BLINK = 0.4f;

    private final GameMap oldMap;
    private final NameGridLayout grid = new NameGridLayout();

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

        // Pre-fill the last-used name. No native keyboard is ever raised.
        Score best = getGame().getHighScoreService().getMyBest();
        if (best != null && best.getName() != null) {
            String prev = best.getName().trim();
            name.append(prev.length() > MAX_NAME ? prev.substring(0, MAX_NAME) : prev);
        }
    }

    @Override
    public void render(float delta) {
        blinkTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // DONE / SKIP in the top corners.
        renderer.yellowFont12.draw(batch, "DONE", grid.doneRect().x + 5, grid.doneRect().y + 20);
        renderer.yellowFont12.draw(batch, "SKIP", grid.skipRect().x + 5, grid.skipRect().y + 20);

        renderer.yellowFont12.draw(batch, String.format("YOUR SCORE %07d", oldMap.score), 150, 305);

        boolean cursorOn = ((int) (blinkTime / CURSOR_BLINK)) % 2 == 0;
        String shown = "NAME: " + name + (cursorOn ? "_" : " ");
        renderer.yellowFont12.draw(batch, shown, 150, 275);

        // The letter grid.
        for (int i = 0; i < grid.cellCount(); i++) {
            Rect r = grid.cellRect(i);
            renderer.yellowFont12.draw(batch, grid.cellLabel(i), r.x, r.y + r.h);
        }

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

        // Scan both pointers (a stray second touch shouldn't block a real tap).
        for (int p = 0; p <= 1; p++) {
            if (!Gdx.input.isTouched(p)) {
                continue;
            }
            float vx = (Gdx.input.getX(p) / (float) Gdx.graphics.getWidth()) * SCREEN_WIDTH;
            float vy = SCREEN_HEIGHT - (Gdx.input.getY(p) / (float) Gdx.graphics.getHeight()) * SCREEN_HEIGHT;

            if (grid.hitDone(vx, vy)) {
                confirm();
                return;
            }
            if (grid.hitSkip(vx, vy)) {
                skip();
                return;
            }
            int cell = grid.hitTest(vx, vy);
            if (cell >= 0) {
                applyCell(cell);
                return;
            }
        }
    }

    private void applyCell(int cell) {
        if (grid.isDel(cell)) {
            if (name.length() > 0) {
                name.deleteCharAt(name.length() - 1);
            }
            return;
        }
        if (grid.isSpc(cell)) {
            if (name.length() < MAX_NAME) {
                name.append(' ');
            }
            return;
        }
        if (name.length() < MAX_NAME) {
            name.append(grid.cellLabel(cell));
        }
    }

    private void confirm() {
        // setScreen() only takes effect next frame; latch so a held tap can't
        // submit twice and land duplicate leaderboard rows.
        if (submitted) {
            return;
        }
        submitted = true;
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
        game.setScreen(new HighScoreScreen(game, oldMap.sounds, renderer));
    }

    @Override
    public void hide() {
        super.hide();
        batch.dispose();
    }

}
