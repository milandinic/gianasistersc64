package com.mdinic.game.giana;

public enum GoundMonsterType {

    OWL(0x7a2991, false), JELLY(0x5a2b8f, false), LOBSTER(0x5ad68f, true), WORM(0xb5b737, true);

    boolean needsMirror;
    int mapColor;

    GoundMonsterType(int mapColor, boolean needsMirror) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
    }

    public static GoundMonsterType containsColor(int color) {
        for (GoundMonsterType type : values()) {
            if (type.mapColor == color)
                return type;
        }
        return null;
    }

}
