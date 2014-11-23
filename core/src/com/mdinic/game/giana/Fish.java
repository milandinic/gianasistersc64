package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Fish {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 10;
    static final float BACKWARD_VEL = 4;

    int state = FORWARD;

    Map map;
    Rectangle bounds = new Rectangle();
    Vector2 vel = new Vector2();
    Vector2 pos = new Vector2();
    float angle = 0;
    int fx = 0;
    int fy = 0;
    int bx = 0;
    int by = 0;

    boolean active = true;
    float stateTime = 0;

    public Fish(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;
    }

    public void init() {
        int ix = (int) pos.x;
        int iy = (int) pos.y;

        int top = map.tiles[ix][map.tiles[0].length - 1 - iy - 1];
        int bottom = map.tiles[ix][map.tiles[0].length - 1 - iy + 1];

        if (top == Map.TILE) {
            vel.y = -FORWARD_VEL;
            angle = 180;
            by = -1;
        }
        if (bottom == Map.TILE) {
            vel.y = FORWARD_VEL;
            angle = 0;
            fy = -1;
        }
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        pos.add(vel.x * deltaTime, vel.y * deltaTime);
        boolean change = false;
        if (state == FORWARD) {
            change = map.tiles[(int) pos.x + fx][map.tiles[0].length - 1 - (int) pos.y + fy] == Map.TILE;
        } else {
            change = map.tiles[(int) pos.x + bx][map.tiles[0].length - 1 - (int) pos.y + by] == Map.TILE;
        }
        if (change) {
            pos.x -= vel.x * deltaTime;
            pos.y -= vel.y * deltaTime;
            state = -state;
            vel.scl(-1);
            if (state == FORWARD)
                vel.nor().scl(FORWARD_VEL);
            if (state == BACKWARD)
                vel.nor().scl(BACKWARD_VEL);
        }

        bounds.x = pos.x;
        bounds.y = pos.y;

        if (map.giana.bounds.overlaps(bounds)) {
            if (map.giana.state != GianaState.DYING) {
                map.giana.state = GianaState.DYING;
                map.giana.stateTime = 0;
            }
        }

    }
}
