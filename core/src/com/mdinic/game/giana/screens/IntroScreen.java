package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.Sounds;
import com.mdinic.game.giana.Sounds.BgMusic;

public class IntroScreen extends GianaSistersScreen {

    GameMap map;

    public IntroScreen(Game game, Sounds sounds, MapRenderer renderer) {
        super(game, renderer);
        map = new GameMap(0, sounds);
        map.demo = true;
        renderer.setMap(map, false);
    }

    @Override
    public void show() {
        map.sounds.setMute(!getGame().getSettingsService().isSoundEnabled());
        map.sounds.play(BgMusic.INTRO);
    }

    @Override
    public void render(float delta) {

        delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());

        if (delta == 0) {
            return;
        }

        map.update(delta);

        Gdx.gl.glClearColor(map.r, map.g, map.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(delta);

        if (map.endDoor != null && map.endDoor.bounds.overlaps((map.giana.bounds))) {
            game.setScreen(new HighScoreScreen(game, map.sounds, renderer));
        }

        map.giana.runRight();

        if (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched()) {
            game.setScreen(new LevelStartingScreen(game, map, renderer));
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
        map.sounds.stop(BgMusic.INTRO);
    }

}
