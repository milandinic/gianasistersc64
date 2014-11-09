package com.badlogic.cubocy;

import com.badlogic.cubocy.Giana.GianaState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

public class Map {
    static int EMPTY = 0;
    static int TILE = 0xffffff;
    static int START = 0xff0000;
    static int END = 0xff00ff;

    static int DIAMOND = 5570300;
    static int SPIKES = 0x00ff00;
    static int ROCKET = 0x0000ff;
    static int MOVING_SPIKES = 0xffff00;
    static int LASER = 0x00ffff;
    static int TREAT_BOX = 0xff8a00;

    int[][] tiles;
    public Giana giana;

    Array<Rocket> rockets = new Array<Rocket>();
    Array<MovingSpikes> movingSpikes = new Array<MovingSpikes>();
    Array<Laser> lasers = new Array<Laser>();
    Array<Diamond> diamonds = new Array<Diamond>();
    Array<TreatBox> treatBoxes = new Array<TreatBox>();
    StartPosition startPosition;
    // row, column
    ArrayMap<Integer, ArrayMap<Integer, TreatBox>> treatBoxesMap = new ArrayMap<Integer, ArrayMap<Integer, TreatBox>>();
    public EndDoor endDoor;

    public Map() {
        loadBinary();
    }

    private void loadBinary() {
        Pixmap pixmap = new Pixmap(Gdx.files.internal("data/levels.png"));
        tiles = new int[pixmap.getWidth()][pixmap.getHeight()];
        for (int y = 0; y < 35; y++) {
            treatBoxesMap.put(new Integer(y), new ArrayMap<Integer, TreatBox>());
            for (int x = 0; x < 150; x++) {
                int pix = (pixmap.getPixel(x, y) >>> 8) & 0xffffff;
                if (match(pix, START)) {
                    startPosition = new StartPosition(x, pixmap.getHeight() - 1 - y);

                    giana = new Giana(this, startPosition.bounds.x, startPosition.bounds.y);
                    giana.state = GianaState.SPAWN;

                } else if (match(pix, DIAMOND)) {
                    diamonds.add(new Diamond(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, TREAT_BOX)) {
                    TreatBox treatBox = new TreatBox(this, x, pixmap.getHeight() - 1 - y);
                    treatBoxes.add(treatBox);
                    treatBoxesMap.get(y).put(x, treatBox);
                    tiles[x][y] = pix;
                } else if (match(pix, ROCKET)) {
                    Rocket rocket = new Rocket(this, x, pixmap.getHeight() - 1 - y);
                    rockets.add(rocket);
                } else if (match(pix, MOVING_SPIKES)) {
                    movingSpikes.add(new MovingSpikes(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, LASER)) {
                    lasers.add(new Laser(this, x, pixmap.getHeight() - 1 - y));
                } else if (match(pix, END)) {
                    endDoor = new EndDoor(x, pixmap.getHeight() - 1 - y);
                } else {
                    tiles[x][y] = pix;
                }
            }
        }

        for (int i = 0; i < movingSpikes.size; i++) {
            movingSpikes.get(i).init();
        }
        for (int i = 0; i < lasers.size; i++) {
            lasers.get(i).init();
        }
    }

    boolean match(int src, int dst) {
        return src == dst;
    }

    public void update(float deltaTime) {
        giana.update(deltaTime);
        if (giana.state == GianaState.DEAD)
            giana = new Giana(this, startPosition.bounds.x, startPosition.bounds.y);

        for (Rocket rocket : rockets) {
            rocket.update(deltaTime);
        }
        for (MovingSpikes spikes : movingSpikes) {
            spikes.update(deltaTime);
        }
        for (Diamond diamond : diamonds) {
            diamond.update(deltaTime);
        }
        for (TreatBox box : treatBoxes) {
            box.update(deltaTime);
        }
        for (int i = 0; i < lasers.size; i++) {
            lasers.get(i).update();
        }
    }

    public boolean isDeadly(int tileId) {
        return tileId == SPIKES;
    }
}
