package com.mdinic.game.giana;

public enum FixedTrapType {

    MOVING_SPIKES(3, 0.7f, 0x00ff00), WATER(1, 1, 0x7b7eae), TRIANGLE(1, 1, 0x894036), FIRE(1, 1, 0x8c4231, true);

    float height;
    float width;
    float mapColor;
    boolean moveHalfX = false;

    FixedTrapType(float width, float height, float mapColor) {
        this.height = height;
        this.width = width;
        this.mapColor = mapColor;
    }

    FixedTrapType(float width, float height, float mapColor, boolean move) {
        this.height = height;
        this.width = width;
        this.mapColor = mapColor;
        this.moveHalfX = move;
    }

    public static FixedTrapType containsColor(int color) {
        for (FixedTrapType type : values()) {
            if (type.mapColor == color)
                return type;
        }

        return null;
    }
}
