package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Bee {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 2;
    static final float BACKWARD_VEL = 2;

    boolean alive = true;

    int state = FORWARD;
    float stateTime = 0;
    Map map;
    Rectangle bounds = new Rectangle();

    Vector2 vel = new Vector2();
    Vector2 pos = new Vector2();
    int fx = 0;
    int bx = 0;

    public Bee(Map map, float x, float y) {

        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;

        vel.set(-1, 0);
    }

    public void init() {
        int ix = (int) pos.x;
        int iy = (int) pos.y;

        int left = map.tiles[ix - 1][map.tiles[0].length - 1 - iy];
        int right = map.tiles[ix + 1][map.tiles[0].length - 1 - iy];

        if (left == Map.TILE) {
            vel.x = FORWARD_VEL;
            fx = 1;
        }
        if (right == Map.TILE) {
            vel.x = -FORWARD_VEL;
            bx = 1;
        }
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        pos.add(vel.x * deltaTime, vel.y * deltaTime);
        boolean change = false;
        int y = map.tiles[0].length - 1 - (int) pos.y;
        if (state == FORWARD) {
            change = map.isColidable(map.tiles[(int) Math.floor(pos.x) + fx][y]);
        } else {
            change = map.isColidable(map.tiles[(int) Math.ceil(pos.x) + bx][y]);
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

        if (alive && map.giana.bounds.overlaps(bounds)) {
            if (map.giana.state != GianaState.DYING) {
                map.giana.state = GianaState.DYING;
                map.giana.stateTime = 0;
            }
        }
    }
}
