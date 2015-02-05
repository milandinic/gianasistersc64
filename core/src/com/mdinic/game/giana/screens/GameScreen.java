package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.mdinic.game.giana.GianaState;
import com.mdinic.game.giana.LevelConf;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.OnscreenControlRenderer;
import com.mdinic.game.giana.Sounds;

public class GameScreen extends GianaSistersScreen {
    Map map;
    OnscreenControlRenderer controlRenderer;

    public GameScreen(Game game, int level, Sounds sounds, MapRenderer renderer) {
        super(game, renderer);

        map = new Map(level, sounds);
        renderer.setMap(map);
    }

    public GameScreen(Game game, Map oldMap, MapRenderer renderer) {
        super(game, renderer);
        map = new Map(oldMap);
    }

    @Override
    public void show() {
        renderer.setMap(map);
        controlRenderer = new OnscreenControlRenderer(map, this);

        map.sounds.play(LevelConf.values()[map.level].getMusic());
    }

    @Override
    public void render(float delta) {
        delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());

        if (delta == 0) {
            return;
        }
        map.update(delta);

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
        if (map.level > 0)
            controlRenderer.render();

        if (map.giana.bounds.overlaps(map.endDoor.bounds)) {
            if (map.level == 0) {
                game.setScreen(new HighScoreScreen(game, map.sounds, renderer));
            } else {
                game.setScreen(new LevelOverScreen(game, map, renderer));
            }
        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            game.setScreen(new IntroScreen(game, map.sounds, renderer));
        }

    }

    @Override
    public void resume() {
        super.resume();
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.play();
        }
    }

    @Override
    public void pause() {
        super.pause();
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.pause();
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose game screen");
        map.sounds.stop(LevelConf.values()[map.level].getMusic());
        controlRenderer.dispose();
    }

}
