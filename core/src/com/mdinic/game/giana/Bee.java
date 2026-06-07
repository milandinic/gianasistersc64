package com.mdinic.game.giana;

public class Bee extends Monster {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 2;
    static final float BACKWARD_VEL = 2;

    int state = FORWARD;

    public Bee(GameMap map, float x, float y) {
        super(map, x, y);
        vel.set(-FORWARD_VEL, 0);
    }

    public void init() {
        int ix = (int) pos.x;
        int iy = (int) pos.y;

        int left = map.tileAt(ix - 1, map.tiles[0].length - 1 - iy, 0);
        int right = map.tileAt(ix + 1, map.tiles[0].length - 1 - iy, 0);

        if (map.isColidable(left)) {
            vel.x = FORWARD_VEL;
            fx = 1;
        }
        if (map.isColidable(right)) {
            vel.x = -FORWARD_VEL;
            bx = 1;
        }
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        int y = map.tiles[0].length - 1 - (int) pos.y;

        if (alive) {
            pos.add(vel.x * deltaTime, vel.y * deltaTime);
        } else {
            // A dead bee falls (vel.y = -8). Once it drops below the map (or off
            // any edge) the tile cell is out of range; tileAt returns 0 there
            // (not colidable), so it keeps falling without running off the array.
            if (map.isColidable(map.tileAt((int) Math.floor(pos.x) + fx, y, 0))) {
                return;
            } else {
                vel.set(0, -8);
                pos.add(vel.x * deltaTime, vel.y * deltaTime);
            }
        }
        boolean change = false;

        if (state == FORWARD) {
            int newX = (int) Math.floor(pos.x) + fx;
            change = newX < 0 || map.isColidable(map.tileAt(newX, y, 0));
        } else {
            int newX = (int) Math.ceil(pos.x) + bx;
            change = newX < 0 || map.isColidable(map.tileAt(newX, y, 0));
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

        killByGiana(true);

        tryToKillGiana();
    }

}
