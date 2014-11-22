package com.mdinic.game.giana;

public enum SimpleImageType {

    BIG_CLOUD(3, 1, 0xfaffff), SMALL_CLOUD(2, 1, 0xfaf0ff), MUSHROOM(2, 2, 0xe56262), ROUND_BUSH(2, 2, 0x7be562), WIDE_BUSH(
            6, 2, 0x73b864), COLUMN(2, 1, 0xd0dc71, true), FLOATING_COLUMN_UP(2, 1, 0XFFF71F, true);

    int width;
    int height;
    int mapColor;
    boolean colidable;

    SimpleImageType(int width, int height, int mapColor) {
        this.mapColor = mapColor;
        this.width = width;
        this.height = height;
        this.colidable = false;
    }

    SimpleImageType(int width, int height, int mapColor, boolean colidable) {
        this.mapColor = mapColor;
        this.width = width;
        this.height = height;
        this.colidable = colidable;
    }

    public static SimpleImageType containsColor(int color) {
        for (SimpleImageType simpleImageType : values()) {
            if (simpleImageType.mapColor == color)
                return simpleImageType;
        }

        return null;
    }

}
