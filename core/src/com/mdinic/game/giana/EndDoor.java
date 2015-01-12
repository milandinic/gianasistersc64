package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;

public class EndDoor {
    public Rectangle bounds = new Rectangle();

    public EndDoor(float x, float y) {
        this.bounds.x = x;
        this.bounds.y = y;
        this.bounds.width = 2;
        this.bounds.height = 2;
    }
}
