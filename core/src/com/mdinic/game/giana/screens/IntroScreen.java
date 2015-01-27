package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.mdinic.game.giana.Sounds;
import com.mdinic.game.giana.Sounds.BgMusic;

public class IntroScreen extends GameScreen {

    public IntroScreen(Game game) {
        super(game, 0);
        map.demo = true;
    }

    @Override
    public void show() {
        super.show();
        Sounds.getInstance().setMute(!getGame().getSettingsService().isSoundEnabled());
        Sounds.getInstance().play(BgMusic.INTRO);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        map.giana.runRight();

        if (Gdx.input.isKeyPressed(Keys.ANY_KEY) || Gdx.input.justTouched()) {
            game.setScreen(new LevelStartingScreen(game, map));
        }
    }

    @Override
    public void hide() {
        Sounds.getInstance().stop(BgMusic.INTRO);
        super.hide();
    }

}
