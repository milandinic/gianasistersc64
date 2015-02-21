package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.mdinic.game.giana.TreatBox.TreatType;

public class GameMap {

    static int MAP_HEIGHT = 16;
    static int MAP_WIDTH = 16;

    static int EMPTY = 0;
    static int TILE = 0xffffff;

    static int START = 0xff0000;

    static int DIAMOND = 5570300;
    static int PIRANHA = 0x6a75ff;
    static int BALL = 0x6a0101;

    static int TREAT_BOX_DIAMOND = 0xff8a00;
    static int TREAT_BOX_USED = 0xffffaf;
    static int TREAT_BOX = 0xffcb8d;

    static int BEE = 0xd2a285;
    static int QUICK_SAND = 0xA45A04;

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
    public int time = 99;
    public int score;

    public boolean demo = false;

    int[][] tiles;
    public Giana giana;

    Array<Diamond> diamonds = new Array<Diamond>();
    Array<TreatBox> treatBoxes = new Array<TreatBox>();
    Array<GroundMonster> groundMonsters = new Array<GroundMonster>();
    Map<SimpleImageType, List<SimpleImage>> simpleImages = new HashMap<SimpleImageType, List<SimpleImage>>();
    Array<Treat> treats = new Array<Treat>();
    Array<SmallDiamoind> treatSmallDiamoinds = new Array<SmallDiamoind>();
    Array<Tile> tileArray = new Array<Tile>();
    Array<QuickSand> quickSandArray = new Array<QuickSand>();

    Array<Bee> bees = new Array<Bee>();
    Array<Fish> fishes = new Array<Fish>();
    Array<Ball> balls = new Array<Ball>();
    Array<FixedTrap> fixedTraps = new Array<FixedTrap>();

    public SimpleImage endDoor;
    public SimpleImage bonusLevelEndDoor;
    public SimpleImage bonusLevelDoor;

    public Sounds sounds;

    int mapLength = 150;
    boolean bonus = false;
    private int pixmapHeight;

    public GameMap(int level, Sounds sounds, boolean bonusMap) {
        mapLength = 24;
        this.bonus = bonusMap;

        this.level = level;
        this.sounds = sounds;
        loadBinary(level, "bonuslevels");

        if (giana == null) {
            throw new IllegalStateException("Giana not on the map");
        }

        colidableColors.add(TREAT_BOX_DIAMOND);
        colidableColors.add(TREAT_BOX);
        colidableColors.add(TILE);
        colidableColors.add(TREAT_BOX_USED);
        colidableColors.add(QUICK_SAND);
    }

    public GameMap(GameMap oldMap) {
        this(oldMap.level, oldMap.sounds);

        this.lives = oldMap.lives;
        this.diamondsCollected = oldMap.diamondsCollected;
        this.score = oldMap.score;
        this.giana.power = oldMap.giana.power;
        this.sounds = oldMap.sounds;
    }

    public void turnBonusDoorIntoSand() {
        quickSandArray.add(new QuickSand(this, bonusLevelDoor.bounds.x, bonusLevelDoor.bounds.y));
        int y = pixmapHeight - 1 - (int) bonusLevelDoor.bounds.y;
        tiles[(int) bonusLevelDoor.bounds.x][y] = QUICK_SAND;

        bonusLevelDoor.bounds.width = 0;
        bonusLevelDoor.bounds.height = 0;
    }

    public GameMap(int level, Sounds sounds) {
        this.level = level;
        this.sounds = sounds;
        loadBinary(level, "levels");
        if (giana == null) {
            throw new IllegalStateException("Giana not on the map");
        }
        if (endDoor == null) {
            throw new IllegalStateException("End door not on the map");
        }

        colidableColors.add(TREAT_BOX_DIAMOND);
        colidableColors.add(TREAT_BOX);
        colidableColors.add(TILE);
        colidableColors.add(TREAT_BOX_USED);
        colidableColors.add(QUICK_SAND);
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

    public void loadBinary(int level, String filename) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal("data/" + filename + ".png"));

        // background color
        int pix;
        if (bonus) {
            pix = BonusLevelConf.BONUS.getBackgroundColor();
        } else {
            pix = LevelConf.values()[level].getBackgroundColor();
        }

        // (pixmap.getPixel(0, level * LEVEL_PIXELBUFFER) >>> 8) & 0xffffff;
        r = (pix & 0xff0000) >>> 16;
        g = (pix & 0x00ff00) >>> 8;
        b = (pix & 0x0000ff);

        r /= 255f;
        g /= 255f;
        b /= 255f;
        pixmapHeight = pixmap.getHeight();

        tiles = new int[pixmap.getWidth()][pixmapHeight];
        for (int y = 0; y < MAP_HEIGHT; y++) {

            for (int x = 0; x < mapLength; x++) {
                pix = (pixmap.getPixel(x, y + (level * LEVEL_PIXELBUFFER)) >>> 8) & 0xffffff;
                int newY = pixmapHeight - 1 - y;
                if (match(pix, START)) {
                    giana = new Giana(this, x, newY);
                } else if (match(pix, DIAMOND)) {
                    diamonds.add(new Diamond(this, x, newY));
                } else if (match(pix, PIRANHA)) {
                    fishes.add(new Fish(this, x, newY));
                } else if (match(pix, BALL)) {
                    balls.add(new Ball(this, x, newY));
                } else if (match(pix, QUICK_SAND)) {
                    quickSandArray.add(new QuickSand(this, x, newY));
                    tiles[x][y] = pix;
                } else if (match(pix, BEE)) {
                    bees.add(new Bee(this, x, newY));
                } else if (GoundMonsterType.containsColor(pix) != null) {
                    groundMonsters.add(new GroundMonster(this, x, newY, GoundMonsterType.containsColor(pix)));

                } else if (SimpleImageType.containsColor(pix) != null) {
                    SimpleImageType imageType = SimpleImageType.containsColor(pix);

                    SimpleImage simpleImage = new SimpleImage(x, newY, imageType);

                    switch (imageType) {
                    case SPIRAL_WAGON:
                        bonusLevelEndDoor = simpleImage;
                        break;
                    case MAGICWATER:
                        bonusLevelDoor = simpleImage;
                        break;
                    case END_LEVEL_DOOR:
                        endDoor = simpleImage;
                        break;
                    default:
                        break;
                    }
                    if (simpleImages.containsKey(imageType)) {
                        simpleImages.get(imageType).add(simpleImage);
                    } else {
                        simpleImages.put(imageType, new ArrayList<SimpleImage>());
                        simpleImages.get(imageType).add(simpleImage);
                    }

                    if (imageType.colidable) {
                        for (int j = 0; j < imageType.height; j++) {
                            for (int i = 0; i < imageType.width; i++) {
                                tiles[x + i][y - j] = pix;
                            }
                        }
                    }
                } else if (FixedTrapType.containsColor(pix) != null) {
                    FixedTrapType type = FixedTrapType.containsColor(pix);
                    fixedTraps.add(new FixedTrap(this, x, newY, type));
                } else if (match(pix, TREAT_BOX_DIAMOND)) {
                    treatBoxes.add(new TreatBox(this, x, newY, TreatType.DIAMOND));
                    tiles[x][y] = pix;
                } else if (match(pix, TREAT_BOX)) {
                    treatBoxes.add(new TreatBox(this, x, newY, TreatType.BALL));
                    tiles[x][y] = pix;
                } else if (match(pix, TREAT_BOX_USED)) {
                    tiles[x][y] = pix;
                    TreatBox treatBox = new TreatBox(this, x, newY, TreatType.DIAMOND);
                    treatBox.active = false;
                    treatBoxes.add(treatBox);
                } else {
                    if (tiles[x][y] == 0) {
                        tiles[x][y] = pix;
                        if (match(pix, TILE))
                            tileArray.add(new Tile(this, x, newY));
                    }
                }
            }
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

        for (FixedTrap water : fixedTraps) {
            water.update(deltaTime);
        }
        for (GroundMonster monster : groundMonsters) {
            if (monster.alive) {
                monster.update(deltaTime);
            }
        }

        for (Bee bee : bees) {
            bee.update(deltaTime);
        }

        for (Treat treat : treats) {
            if (treat.active)
                treat.update(deltaTime);
        }

        for (Fish fish : fishes) {
            fish.update(deltaTime);
        }

        for (Ball ball : balls) {
            ball.update(deltaTime);
        }

        for (Tile tile : tileArray) {
            tile.update(deltaTime);
        }

        for (QuickSand sand : quickSandArray) {
            sand.update(deltaTime);
        }

    }

    public boolean isDeadly(int tileId) {
        return FixedTrapType.containsColor(tileId) != null;
    }
}
