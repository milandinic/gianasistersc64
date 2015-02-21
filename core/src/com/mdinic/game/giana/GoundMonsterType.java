package com.mdinic.game.giana;

public enum GoundMonsterType {

    OWL(0x7a2991, false, true), JELLY(0x5a2b8f, false, true, 1.1f), LOBSTER(0x5ad68f, true, true, 1.3f), WORM(0xb5b737,
            true, false), EYE(0xffb662, true, true, 1.2f), BUG(0x3dff3d, true, true, 1f, 0.5f, 2f), YELLOW_ALIEN(
            0x3d223d, true, false), PURPLE_ALIEN(0xc640c4, true, true), DAGGER(0xe8c056, true, true, 1, 0.8f);

    boolean needsMirror;
    int mapColor;
    boolean canBeKilled;
    float width;
    float height;
    float speed;

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = 1;
        this.height = 1;
        this.speed = 1;
    }

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled, float width, float height) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = width;
        this.height = height;
        this.speed = 1;
    }

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled, float speed) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = 1;
        this.height = 1;
        this.speed = speed;
    }

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled, float width, float height, float speed) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = width;
        this.height = height;
        this.speed = speed;
    }

    public static GoundMonsterType containsColor(int color) {
        for (GoundMonsterType type : values()) {
            if (type.mapColor == color)
                return type;
        }
        return null;
    }

}
