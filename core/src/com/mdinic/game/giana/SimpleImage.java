package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;

public class SimpleImage {

    Rectangle bounds = new Rectangle();
    SimpleImageType type;

    public SimpleImage(float x, float y, SimpleImageType type) {
        this.type = type;
        bounds.x = x;
        bounds.y = y;
        bounds.width = type.width;
        bounds.height = type.height;
    }
}
