package com.mdinic.game.giana;

public enum GoundMonsterType {

    OWL(0x7a2991, false, true), JELLY(0x5a2b8f, false, true), LOBSTER(0x5ad68f, true, true), WORM(0xb5b737, true, false);

    boolean needsMirror;
    int mapColor;
    boolean canBeKilled;

    GoundMonsterType(int mapColor, boolean needsMirror, boolean canBeKilled) {
        this.mapColor = mapColor;
        this.needsMirror = needsMirror;
        this.canBeKilled = canBeKilled;
    }

    public static GoundMonsterType containsColor(int color) {
        for (GoundMonsterType type : values()) {
            if (type.mapColor == color)
                return type;
        }
        return null;
    }

}
