package com.mdinic.game.giana;

public enum GianaPower {
    // ball
    NONE,
    // lightning
    BREAK_BRICK,
    // double lightning
    SHOOT,
    // strawberry
    HOMING,
    // nothing for now
    LAST;

    public static boolean isBig(GianaPower power) {
        return power.compareTo(NONE) > 0;
    }

    public boolean hasGun() {
        return this.compareTo(SHOOT) >= 0;
    }

    public boolean hasNext() {
        return this.compareTo(LAST) != 0;
    }

    public boolean isHoming() {
        return this.compareTo(HOMING) > 0;
    }

}
