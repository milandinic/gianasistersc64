package com.mdinic.game.giana;

public enum GianaPower {
    NONE, BIG, SHOOT, STRAWBERRY;

    public static boolean isBig(GianaPower power) {
        return power.compareTo(NONE) > 0;
    }

    public boolean hasGun() {
        return this.compareTo(BIG) > 0;
    }

    public boolean isHoming() {
        return this.compareTo(SHOOT) > 0;
    }

    public static boolean hasNext(GianaPower power) {
        return power.compareTo(STRAWBERRY) < 0;
    }
}
