package com.mdinic.game.giana;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.mdinic.game.giana.Giana.GianaState;
import com.mdinic.game.giana.GroundMonster.GoundMonsterType;

public class Map {

    static int MAP_HEIGHT = 16;
    static int MAP_WIDTH = 16;

    static int EMPTY = 0;
    static int TILE = 0xffffff;
    static int START = 0xff0000;
    static int END = 0xff00ff;

    static int DIAMOND = 5570300;
    static int MOVING_SPIKES = 0x00ff00;

    static int MOVING_SPIKES_OLD = 0xffff00;

    static int TREAT_BOX = 0xff8a00;
    static int OWL = 0x7a2991;
    static int JELLY = 0x5a2b8f;
    static int LOBSTER = 0x5ad68f;

    static int BIG_CLOUD = 0xfaffff;
    static int SMALL_CLOUD = 0xfaf0ff;
    static int MUSHROOM = 0xe56262;
    static int ROUND_BUSH = 0x7be562;
    static int WIDE_BUSH = 0x73b864;
    static int COLUMN = 0xd0dc71;

    static int LEVEL_PIXELBUFFER = 20;

    // pixel on 0,0 position
    // background color
    public float r; // 0.0-1.0
    public float g; // 0.0-1.0
    public float b; // 0.0-1.0

    public int lives = 3;
    public int level;
    public int time;
    public int diamondsCollected;

    public boolean demo;

    int[][] tiles;
    public Giana giana;

    Array<Diamond> diamonds = new Array<Diamond>();
    Array<TreatBox> treatBoxes = new Array<TreatBox>();
    Array<MovingSpikes> movingSpikes = new Array<MovingSpikes>();
    Array<GroundMonster> groundMonsters = new Array<GroundMonster>();
    Array<SimpleImage> simpleImages = new Array<SimpleImage>();

    StartPosition startPosition;
    // row, column
    ArrayMap<Integer, ArrayMap<Integer, TreatBox>> treatBoxesMap = new ArrayMap<Integer, ArrayMap<Integer, TreatBox>>();
    public EndDoor endDoor;

    public Map(int level) {
        time = 99;
        this.level = level;
        loadBinary(level);
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
            treatBoxesMap.put(new Integer(y), new ArrayMap<Integer, TreatBox>());
            for (int x = 0; x < 150; x++) {
                pix = (pixmap.getPixel(x, y + (level * LEVEL_PIXELBUFFER)) >>> 8) & 0xffffff;
                if (match(pix, START)) {
                    startPosition = new StartPosition(x, pixmap.getHeight() - 1 - y);

                    giana = new Giana(this, startPosition.bounds.x, startPosition.bounds.y);
                    giana.state = GianaState.SPAWN;

                } else if (match(pix, DIAMOND)) {
                    diamonds.add(new Diamond(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, OWL)) {
                    groundMonsters.add(new GroundMonster(this, x, pixmap.getHeight() - 1 - y, GoundMonsterType.OWL));
                } else if (match(pix, JELLY)) {
                    groundMonsters.add(new GroundMonster(this, x, pixmap.getHeight() - 1 - y, GoundMonsterType.JELLY));
                } else if (match(pix, LOBSTER)) {
                    // groundMonsters
                    // .add(new GroundMonster(this, x, pixmap.getHeight() - 1 -
                    // y, GoundMonsterType.LOBSTER));

                } else if (match(pix, BIG_CLOUD)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.BIG_CLOUD));
                } else if (match(pix, SMALL_CLOUD)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.SMALL_CLOUD));
                } else if (match(pix, MUSHROOM)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.MUSHROOM));
                } else if (match(pix, ROUND_BUSH)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.ROUND_BUSH));
                } else if (match(pix, WIDE_BUSH)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.WIDE_BUSH));
                } else if (match(pix, COLUMN)) {
                    simpleImages.add(new SimpleImage(x, pixmap.getHeight() - 1 - y, SimpleImageType.COLUMN));
                    for (int j = 0; j < SimpleImageType.COLUMN.height; j++) {
                        for (int i = 0; i < SimpleImageType.COLUMN.width; i++) {
                            tiles[x + i][y + j] = pix;
                        }
                    }

                } else if (match(pix, MOVING_SPIKES)) {
                    movingSpikes.add(new MovingSpikes(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, TREAT_BOX)) {
                    TreatBox treatBox = new TreatBox(this, x, pixmap.getHeight() - 1 - y);
                    treatBoxes.add(treatBox);
                    treatBoxesMap.get(y).put(x, treatBox);
                    tiles[x][y] = pix;
                } else if (match(pix, MOVING_SPIKES_OLD)) {
                    // movingSpikesOld.add(new Fish(this, x, pixmap.getHeight()
                    // - 1 - y));
                } else if (match(pix, END)) {
                    endDoor = new EndDoor(x, pixmap.getHeight() - 1 - y);
                } else {
                    if (tiles[x][y] == 0) {
                        tiles[x][y] = pix;
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
            giana = new Giana(this, startPosition.bounds.x, startPosition.bounds.y);

        for (Diamond diamond : diamonds) {
            diamond.update(deltaTime);
        }
        for (TreatBox box : treatBoxes) {
            box.update(deltaTime);
        }
        for (MovingSpikes mSpike : movingSpikes) {
            mSpike.update(deltaTime);
        }
        for (GroundMonster monster : groundMonsters) {
            monster.update(deltaTime);
        }

    }

    public boolean isDeadly(int tileId) {
        return tileId == MOVING_SPIKES;
    }
}
