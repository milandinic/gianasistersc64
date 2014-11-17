package com.mdinic.game.giana;

import com.badlogic.gdx.Game;
import com.mdinic.game.giana.screens.GameScreen;
import com.parse.ParseObject;

public class GianaSistersC64 extends Game {
    @Override
    public void create() {
        setScreen(new GameScreen(this, 1));

        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();
    }
}
