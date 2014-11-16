package com.mdinic.game.giana;

public enum SimpleImageType {

    BIG_CLOUD(3, 1), SMALL_CLOUD(2, 1), MUSHROOM(2, 2), ROUND_BUSH(2, 2), WIDE_BUSH(6, 2), COLUMN(2, 1);

    int width;
    int height;

    SimpleImageType(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
