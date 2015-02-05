package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.Sounds;
import com.mdinic.game.giana.Sounds.BgMusic;

public class IntroScreen extends GameScreen {

    public IntroScreen(Game game, Sounds sounds, MapRenderer renderer) {
        super(game, 0, sounds, renderer);
        map.demo = true;
    }

    @Override
    public void show() {
        super.show();

        map.sounds.setMute(!getGame().getSettingsService().isSoundEnabled());
        map.sounds.play(BgMusic.INTRO);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        map.giana.runRight();

        if (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched()) {
            game.setScreen(new LevelStartingScreen(game, map, renderer));
        }
    }

    @Override
    public void hide() {
        map.sounds.stop(BgMusic.INTRO);
        super.hide();
    }

}
