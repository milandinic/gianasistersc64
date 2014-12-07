package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.mdinic.game.giana.MapResource;

public class IntroScreen extends GameScreen {
    Music music;

    public IntroScreen(Game game) {
        super(game, 0);
        map.demo = true;
    }

    @Override
    public void show() {
        super.show();
        music = Gdx.audio.newMusic(Gdx.files.internal("data/intro.mp3"));
        music.play();
        MapResource.getInstance();
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
        super.hide();
        music.stop();
        music.dispose();
    }

}
