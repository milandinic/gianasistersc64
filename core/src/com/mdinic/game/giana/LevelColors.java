package com.mdinic.game.giana;

public enum LevelColors {

    INTRO(0x826fe8), LEVEL1(0x826fe8), LEVEL2(0x826fe8), LEVEL3(0x826fe8), LEVEL4(0x826fe8), LEVEL5(0x826fe8), LEVEL6(
            0x826fe8), LEVEL7, LEVEL8(0x826fe8), LEVEL9, LEVEL10, LEVEL11(BrickColor.RED), LEVEL12(BrickColor.GREY), LEVEL13(
            BrickColor.GREY), LEVEL14, LEVEL15;

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

    private LevelColors(int backgroundColor, BrickType brickType, BrickColor brickColor) {
        this.backgroundColor = backgroundColor;
        this.brickType = brickType;
        this.brickColor = brickColor;
    }

    private LevelColors(int backgroundColor, BrickColor brickColor) {
        this();
        this.backgroundColor = backgroundColor;
        this.brickColor = brickColor;
    }

    private LevelColors(int backgroundColor) {
        this();
        this.backgroundColor = backgroundColor;
    }

    private LevelColors(BrickColor brickColor) {
        this();
        this.brickColor = brickColor;
    }

    private LevelColors() {
        this.backgroundColor = 0;
        this.brickType = BrickType.SMALL;
        this.brickColor = BrickColor.BROWN;
    }

    private LevelColors(LevelColors colors) {
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
}
