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

        int left = map.tiles[ix - 1][map.tiles[0].length - 1 - iy];
        int right = map.tiles[ix + 1][map.tiles[0].length - 1 - iy];

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
            if (map.isColidable(map.tiles[(int) Math.floor(pos.x) + fx][y])) {
                return;
            } else {
                vel.set(0, -8);
                pos.add(vel.x * deltaTime, vel.y * deltaTime);
            }
        }
        boolean change = false;

        if (state == FORWARD) {
            int newX = (int) Math.floor(pos.x) + fx;
            change = newX < 0 || map.isColidable(map.tiles[newX][y]);
        } else {
            int newX = (int) Math.ceil(pos.x) + bx;
            change = newX < 0 || map.isColidable(map.tiles[newX][y]);
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

        tryToKilGiana();
    }

}
