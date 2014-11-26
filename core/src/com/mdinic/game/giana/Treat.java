package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Treat {
    private static final float MIN_MOVE = 0.1f;

    static final int LEFT = -1;
    static final int RIGHT = 1;
    static final float ACCELERATION = 20f;
    static final float GRAVITY = 20.0f;

    Rectangle nowayCollidableRect = new Rectangle(-1, -1, 0, 0);

    Vector2 pos = new Vector2();
    Vector2 startPos = new Vector2();
    Vector2 accel = new Vector2();
    Vector2 vel = new Vector2();
    public Rectangle bounds = new Rectangle();

    TreatState state;
    public float stateTime = 0;
    int dir = RIGHT;
    Map map;
    boolean active = true;

    Rectangle[] r = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };

    enum TreatState {
        SPAWN, RUNNING
    }

    public Treat(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        startPos.x = x;
        startPos.y = y;
        bounds.width = 0.9f;
        bounds.height = 0.9f;
        bounds.x = x;
        bounds.y = y;
        state = TreatState.SPAWN;
        stateTime = 0;
    }

    public void update(float deltaTime) {

        if (state == TreatState.SPAWN) {
            if (startPos.y + 1 > pos.y) {
                bounds.y += MIN_MOVE;
                pos.y = bounds.y;
            } else {
                bounds.y = startPos.y + 1;
                pos.y = bounds.y;

                state = TreatState.RUNNING;
            }
        } else {
            accel.x = ACCELERATION * dir;
            vel.x = dir == RIGHT ? 3 : -3;
            accel.y = -GRAVITY;
            accel.scl(deltaTime);
            vel.add(accel.x, accel.y);

            vel.scl(deltaTime);
            tryMove();
            vel.scl(1.0f / deltaTime);
        }
        stateTime += deltaTime;

        if (map.giana.bounds.overlaps(bounds)) {
            active = false;
            map.score += 100;
            map.giana.big = true;
            map.giana.state = GianaState.GROW;
            map.giana.stateTime = 0;

        }

    }

    private void tryMove() {
        bounds.x += vel.x;
        fetchCollidableRects();
        for (int i = 0; i < r.length; i++) {
            Rectangle rect = r[i];
            if (bounds.overlaps(rect)) {
                if (vel.x < 0) {
                    if (dir == LEFT) {
                        dir = RIGHT;
                    }
                    bounds.x = rect.x + rect.width + 0.01f;
                } else {
                    if (dir == RIGHT) {
                        dir = LEFT;
                    }
                    bounds.x = rect.x - bounds.width - 0.01f;
                }

                vel.x = 0;
            }
        }

        bounds.y += vel.y;
        fetchCollidableRects();
        for (int i = 0; i < r.length; i++) {
            Rectangle rect = r[i];
            if (bounds.overlaps(rect)) {
                if (vel.y < 0) {
                    bounds.y = rect.y + rect.height + 0.01f;
                } else
                    bounds.y = rect.y - bounds.height - 0.01f;
                vel.y = 0;
            }
        }

        pos.x = bounds.x;
        pos.y = bounds.y;
    }

    private void fetchCollidableRects() {
        int p1x = (int) bounds.x;
        int p1y = (int) Math.floor(bounds.y);

        int p2x = (int) (bounds.x + bounds.width);
        int p2y = (int) Math.floor(bounds.y);

        int p4x = (int) bounds.x;
        int p4y = (int) (bounds.y + bounds.height);

        int[][] tiles = map.tiles;
        int tile1 = 0;
        int tile2 = 0;
        int tile4 = 0;

        if (p1x <= 0 || p2x <= 0 || p4x <= 0 || bounds.x < map.giana.maxX - ((Map.MAP_WIDTH / 2) + 2)) {
            active = false;
            return;
        }

        int y = map.tiles[0].length - 1 - p1y;
        if (y > 0)
            tile1 = tiles[p1x][y]; // to the right
        else {
            active = false;
            return;
        }

        y = map.tiles[0].length - 1 - p2y;
        if (y > 0)
            tile2 = tiles[p2x][y];// to the left
        else {
            active = false;
            return;
        }

        y = map.tiles[0].length - 1 - p4y;
        if (y > 0)
            tile4 = tiles[p4x][y];// down
        else {
            active = false;
            return;
        }

        if (map.isColidable(tile1))
            r[0].set(p1x, p1y, 1, 1);
        else
            r[0].set(nowayCollidableRect);

        if (map.isColidable(tile2))
            r[1].set(p2x, p2y, 1, 1);
        else
            r[1].set(nowayCollidableRect);

        r[2].set(nowayCollidableRect);

        if (map.isColidable(tile4))
            r[3].set(p4x, p4y, 1, 1);
        else
            r[3].set(nowayCollidableRect);

    }
}
