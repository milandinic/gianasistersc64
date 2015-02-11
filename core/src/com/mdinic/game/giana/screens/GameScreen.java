package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.GianaState;
import com.mdinic.game.giana.LevelConf;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.OnscreenControlRenderer;

public class GameScreen extends GianaSistersScreen {

    GameMap map;
    OnscreenControlRenderer controlRenderer;

    boolean stopMusic = true;
    boolean fromBonus = false;

    ShapeRenderer shape = new ShapeRenderer();
    boolean paused = false;

    // from bonus
    public GameScreen(Game game, GameMap oldMap, MapRenderer renderer, boolean fromBonus) {
        super(game, renderer);
        this.fromBonus = fromBonus;
        map = oldMap;
        renderer.setMap(map, false);
    }

    public GameScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        map = new GameMap(oldMap);
        renderer.setMap(map, true);
    }

    @Override
    public void show() {
        map.sounds.setMute(!getGame().getSettingsService().isSoundEnabled());

        controlRenderer = new OnscreenControlRenderer(map, this);

        if (!fromBonus)
            map.sounds.play(LevelConf.values()[map.level].getMusic());

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {

                if (keycode == Keys.BACK) {
                    if (paused) {
                        getGame().getGeneralService().showConfirmExitDialog();
                    } else {
                        paused = true;
                        pause();
                        return false;
                    }
                }
                return false;
            }
        });
        Gdx.input.setCatchBackKey(true);
    }

    @Override
    public void render(float delta) {

        if (paused) {

            // un-pause on touch
            if (Gdx.input.isTouched()) {
                paused = false;
                resume();
            }
            delta = 0;
        } else {
            delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());
            if (delta > 0) {
                map.update(delta);
            }
        }

        if (map.giana.state == GianaState.DEAD) {
            map.level--;
            if (map.lives == 0) {
                if (getGame().getHighScoreService().goodForHighScores(map.score))
                    game.setScreen(new EnterYourNameScreen(game, map, renderer));
                else
                    game.setScreen(new HighScoreScreen(game, map.sounds, renderer));
            } else {
                game.setScreen(new LevelStartingScreen(game, map, renderer));
            }
            return;
        }

        if (map.giana.state != GianaState.DYING) {
            map.time = 99 - (int) map.giana.stateTime;
        }

        Gdx.gl.glClearColor(map.r, map.g, map.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(delta);
        controlRenderer.render();

        if (paused) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shape.setColor(0, 0, 0, 0.5f);
            shape.begin(ShapeType.Filled);
            shape.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shape.end();

            renderer.fontBatch.begin();
            renderer.yellowFont10.draw(renderer.fontBatch, "PAUSED", SCREEN_WIDTH / 2 - 10, SCREEN_HEIGHT / 2);
            renderer.fontBatch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (map.endDoor != null && map.endDoor.bounds.contains(map.giana.bounds)) {
            game.setScreen(new LevelOverScreen(game, map, renderer));
        }

        if (map.bonusLevelDoor != null) {
            if (map.giana.state != GianaState.DYING && map.giana.killerBounds.overlaps(map.bonusLevelDoor.bounds)) {
                stopMusic = false;
                game.setScreen(new BonusGameScreen(game, map, renderer));
            }
        }

    }

    @Override
    public void resume() {
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.play();
        }
    }

    @Override
    public void pause() {
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.pause();
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose game screen");
        if (stopMusic)
            map.sounds.stop(LevelConf.values()[map.level].getMusic());
        controlRenderer.dispose();
    }

}
