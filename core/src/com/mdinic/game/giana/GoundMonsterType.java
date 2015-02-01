package com.mdinic.game.giana;

public enum GoundMonsterType {

    OWL(0x7a2991, false, true), JELLY(0x5a2b8f, false, true), LOBSTER(0x5ad68f, true, true), WORM(0xb5b737, true, false), EYE(
            0xffb662, true, true), BUG(0x3dff3d, true, true, 1f, 0.5f), YELLOW_ALIEN(0x3d223d, true, false), PURPLE_ALIEN(
            0xc640c4, true, true), DAGGER(0xe8c056, true, true, 1, 0.8f);

    boolean needsMirror;
    int mapColor;
    boolean canBeKilled;
    float width;
    float height;

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = 1;
        this.height = 1;
    }

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled, float width, float height) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
        this.width = width;
        this.height = height;
    }

    public static GoundMonsterType containsColor(int color) {
        for (GoundMonsterType type : values()) {
            if (type.mapColor == color)
                return type;
        }
        return null;
    }

}
