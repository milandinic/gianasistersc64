package com.mdinic.game.giana;

import com.mdinic.game.giana.Sounds.BgMusic;

public enum LevelConf {

    INTRO(0x826fe8, BgMusic.INTRO), LEVEL1(0x826fe8, BrickColor.BROWN), LEVEL2(0x826fe8), LEVEL3(0x826fe8,
            BgMusic.MUSIC_FAST), LEVEL4(0x826fe8), LEVEL5(0x826fe8), LEVEL6(0x826fe8), LEVEL7, LEVEL8(0x826fe8), LEVEL9, LEVEL10, LEVEL11(
            BrickColor.RED, BgMusic.MUSIC_FAST), LEVEL12(BrickColor.GREY, BgMusic.MUSIC_FAST), LEVEL13(BrickColor.GREY,
            BgMusic.MUSIC_FAST), LEVEL14(BrickColor.GREY, BgMusic.MUSIC_FAST), LEVEL15(BrickColor.GREY,
            BgMusic.MUSIC_FAST), LEVEL16(BrickColor.RED, BgMusic.MUSIC_FAST), LEVEL17(0x826fe8, BrickColor.BROWN,
            BgMusic.MUSIC_LIGHT), LEVEL18(0x826fe8), LEVEL19(0x826fe8), LEVEL20(0x826fe8), LEVEL21(BrickColor.VIOLET,
            BgMusic.MUSIC_FAST), LEVEL22(BrickColor.VIOLET, BgMusic.MUSIC_FAST);

    enum BrickType {
        SMALL, BIG
    };

    enum BrickColor {

        RED("red"), VIOLET("violet"), GREY("grey"), BROWN("brown");

        String name;

        BrickColor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    private int backgroundColor;
    private BrickType brickType;
    private BrickColor brickColor;
    private BgMusic music;

    private LevelConf(int backgroundColor, BrickType brickType, BrickColor brickColor, BgMusic music) {
        this.backgroundColor = backgroundColor;
        this.brickType = brickType;
        this.brickColor = brickColor;
        this.music = music;
    }

    private LevelConf(int backgroundColor, BrickColor brickColor, BgMusic music) {
        this();
        this.backgroundColor = backgroundColor;
        this.brickColor = brickColor;
        this.music = music;
    }

    private LevelConf(int backgroundColor, BrickColor brickColor) {
        this();
        this.backgroundColor = backgroundColor;
        this.brickColor = brickColor;
    }

    private LevelConf(int backgroundColor) {
        this();
        this.backgroundColor = backgroundColor;
    }

    private LevelConf(int backgroundColor, BgMusic music) {
        this();
        this.backgroundColor = backgroundColor;
        this.music = music;
    }

    private LevelConf(BrickColor brickColor) {
        this();
        this.brickColor = brickColor;
    }

    private LevelConf(BrickColor brickColor, BgMusic music) {
        this();
        this.brickColor = brickColor;
        this.music = music;
    }

    private LevelConf() {
        this.backgroundColor = 0;
        this.brickType = BrickType.SMALL;
        this.brickColor = BrickColor.BROWN;
        this.music = BgMusic.MUSIC_LIGHT;
    }

    private LevelConf(LevelConf colors) {
        this.backgroundColor = colors.backgroundColor;
        this.brickType = colors.brickType;
        this.brickColor = colors.brickColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public BrickType getBrickType() {
        return brickType;
    }

    public BrickColor getBrickColor() {
        return brickColor;
    }

    public BgMusic getMusic() {
        return music;
    }

}
