package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;

public class SimpleImage {

    public Rectangle bounds = new Rectangle();
    SimpleImageType type;

    public SimpleImage(float x, float y, SimpleImageType type) {
        this.type = type;
        bounds.x = x;
        bounds.y = y;
        bounds.width = type.width;
        bounds.height = type.height;
    }

    public SimpleImage(SimpleImage image) {
        super();

        type = image.type;
        bounds.x = image.bounds.x;
        bounds.y = image.bounds.y;
        bounds.width = image.bounds.width;
        bounds.height = image.bounds.height;
    }

}
