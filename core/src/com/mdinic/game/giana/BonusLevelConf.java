package com.mdinic.game.giana;

import com.mdinic.game.giana.LevelConf.BrickColor;
import com.mdinic.game.giana.Sounds.BgMusic;

public enum BonusLevelConf {

    BONUS;

    private int backgroundColor;
    private BrickColor brickColor;
    private BgMusic music;

    private BonusLevelConf() {
        this.backgroundColor = 0;
        this.brickColor = BrickColor.RED;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public BrickColor getBrickColor() {
        return brickColor;
    }

    public BgMusic getMusic() {
        return music;
    }

}
