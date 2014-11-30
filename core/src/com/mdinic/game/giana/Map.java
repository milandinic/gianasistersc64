package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.mdinic.game.giana.TreatBox.TreatType;

public class Map {

    static int MAP_HEIGHT = 16;
    static int MAP_WIDTH = 16;

    static int EMPTY = 0;
    static int TILE = 0xffffff;
    static int START = 0xff0000;
    static int END = 0xff00ff;

    static int DIAMOND = 5570300;
    static int MOVING_SPIKES = 0x00ff00;

    static int TREAT_BOX = 0xff8a00;
    static int TREAT_BOX_BALL = 0xffcb8d;

    static int LEVEL_PIXELBUFFER = 20;

    List<Integer> colidableColors = new ArrayList<Integer>();

    // pixel on 0,0 position
    // background color
    public float r; // 0.0-1.0
    public float g; // 0.0-1.0
    public float b; // 0.0-1.0

    public int lives = 3;
    public int diamondsCollected;
    public int level;
    public int time;
    public int score;

    public boolean demo = false;

    int[][] tiles;
    public Giana giana;

    Array<Diamond> diamonds = new Array<Diamond>();
    Array<TreatBox> treatBoxes = new Array<TreatBox>();
    Array<MovingSpikes> movingSpikes = new Array<MovingSpikes>();
    Array<GroundMonster> groundMonsters = new Array<GroundMonster>();
    Array<SimpleImage> simpleImages = new Array<SimpleImage>();
    Array<Treat> treats = new Array<Treat>();
    Array<SmallDiamoind> treatSmallDiamoinds = new Array<SmallDiamoind>();
    Array<Tile> tileArray = new Array<Tile>();

    Array<Fish> fishes = new Array<Fish>();

    Vector2 startPosition = new Vector2();
    public EndDoor endDoor;

    Sound diamondSound;

    public Map(Map oldMap) {
        this(oldMap.level);

        diamondSound = Gdx.audio.newSound(Gdx.files.internal("data/sfx/diamond-collect.mp3"));

        this.lives = oldMap.lives;
        this.diamondsCollected = oldMap.diamondsCollected;
        this.score = oldMap.score;
        this.giana.big = oldMap.giana.big;
    }

    public Map(int level) {
        time = 99;
        this.level = level;
        loadBinary(level);

        colidableColors.add(TREAT_BOX);
        colidableColors.add(TREAT_BOX_BALL);
        colidableColors.add(TILE);
    }

    public void collectDiamound() {
        diamondsCollected++;
        score += 25;
        if (diamondsCollected >= 100) {
            diamondsCollected -= 100;
            lives++;
        }
    }

    public boolean isColidable(int value) {
        return colidableColors.contains(value) || SimpleImageType.containsColor(value) != null;
    }

    public void loadBinary(int level) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal("data/levels.png"));

        // background color
        int pix = (pixmap.getPixel(0, level * LEVEL_PIXELBUFFER) >>> 8) & 0xffffff;
        r = (pix & 0xff0000) >>> 16;
        g = (pix & 0x00ff00) >>> 8;
        b = (pix & 0x0000ff);

        r /= 255f;
        g /= 255f;
        b /= 255f;

        tiles = new int[pixmap.getWidth()][pixmap.getHeight()];
        for (int y = 0; y < MAP_HEIGHT; y++) {

            for (int x = 0; x < 150; x++) {
                pix = (pixmap.getPixel(x, y + (level * LEVEL_PIXELBUFFER)) >>> 8) & 0xffffff;
                if (match(pix, START)) {
                    startPosition.set(x, pixmap.getHeight() - 1 - y);

                    giana = new Giana(this, startPosition.x, startPosition.y);
                    giana.state = GianaState.SPAWN;

                } else if (match(pix, DIAMOND)) {
                    diamonds.add(new Diamond(this, x, pixmap.getHeight() - 1 - y));
                } else if (GoundMonsterType.containsColor(pix) != null) {
                    groundMonsters.add(new GroundMonster(this, x, pixmap.getHeight() - 1 - y, GoundMonsterType
                            .containsColor(pix)));

                } else if (SimpleImageType.containsColor(pix) != null) {
                    SimpleImageType imageType = SimpleImageType.containsColor(pix);
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, imageType));
                    if (imageType.colidable) {
                        for (int j = 0; j < SimpleImageType.COLUMN.height; j++) {
                            for (int i = 0; i < SimpleImageType.COLUMN.width; i++) {
                                tiles[x + i][y + j] = pix;
                            }
                        }
                    }
                } else if (match(pix, MOVING_SPIKES)) {
                    movingSpikes.add(new MovingSpikes(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, TREAT_BOX)) {
                    treatBoxes.add(new TreatBox(this, x, pixmap.getHeight() - 1 - y, TreatType.DIAMOND));
                    tiles[x][y] = pix;
                } else if (match(pix, TREAT_BOX_BALL)) {
                    treatBoxes.add(new TreatBox(this, x, pixmap.getHeight() - 1 - y, TreatType.BALL));
                    tiles[x][y] = pix;
                } else if (match(pix, END)) {
                    endDoor = new EndDoor(x, pixmap.getHeight() - 1 - y);
                } else {
                    if (tiles[x][y] == 0) {
                        tiles[x][y] = pix;
                        if (match(pix, TILE))
                            tileArray.add(new Tile(this, x, pixmap.getHeight() - 1 - y));
                    }
                }
            }
        }

        for (GroundMonster monster : groundMonsters) {
            monster.init();
        }
    }

    boolean match(int src, int dst) {
        return src == dst;
    }

    public void update(float deltaTime) {
        giana.update(deltaTime);
        if (giana.state == GianaState.DEAD)
            return;

        for (Diamond diamond : diamonds) {
            diamond.update(deltaTime);
        }
        for (TreatBox box : treatBoxes) {
            box.update(deltaTime);
        }
        for (SmallDiamoind diamond : treatSmallDiamoinds) {
            diamond.update(deltaTime);
        }
        for (MovingSpikes mSpike : movingSpikes) {
            mSpike.update(deltaTime);
        }
        for (GroundMonster monster : groundMonsters) {
            monster.update(deltaTime);
        }

        for (Treat treat : treats) {
            if (treat.active)
                treat.update(deltaTime);
        }

        for (Fish fish : fishes) {
            if (fish.active)
                fish.update(deltaTime);
        }

        for (Tile tile : tileArray) {
            tile.update(deltaTime);
        }

    }

    public boolean isDeadly(int tileId) {
        return tileId == MOVING_SPIKES;
    }
}
