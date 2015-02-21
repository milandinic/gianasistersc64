package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Ball {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 5;
    static final float BACKWARD_VEL = 3;

    boolean alive = true;

    int state = FORWARD;
    float stateTime = 0;
    GameMap map;
    Rectangle bounds = new Rectangle();

    Vector2 vel = new Vector2();
    Vector2 pos = new Vector2();
    Vector2 startPos = new Vector2();

    public Ball(GameMap map, float x, float y) {

        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;

        startPos.x = x;
        startPos.y = y;

        vel.set(0, FORWARD_VEL);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        bounds.x += vel.x * deltaTime;
        bounds.y += vel.y * deltaTime;

        boolean change = false;
        if (state == FORWARD) {
            change = pos.y - 2 > startPos.y;
        } else {
            change = pos.y < startPos.y;
        }
        if (change) {
            stateTime = 0;
            bounds.x -= vel.x * deltaTime;
            bounds.y -= vel.y * deltaTime;
            state = -state;
            vel.scl(-3);
            if (state == FORWARD)
                vel.nor().scl(FORWARD_VEL);
            if (state == BACKWARD)
                vel.nor().scl(BACKWARD_VEL);
        }

        if (alive && map.giana.bounds.overlaps(bounds) && !map.demo) {
            if (map.giana.state != GianaState.DYING) {
                map.giana.state = GianaState.DYING;
                map.giana.stateTime = 0;
            }
        }

        pos.x = bounds.x;
        pos.y = bounds.y;
    }
}
