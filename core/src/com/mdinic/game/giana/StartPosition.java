package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class StartPosition {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();

    public StartPosition(float x, float y) {
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;
    }
}
