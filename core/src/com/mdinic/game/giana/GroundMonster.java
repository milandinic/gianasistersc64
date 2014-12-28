package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mdinic.game.giana.Sounds.Sfx;

public class GroundMonster {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 1;
    static final float BACKWARD_VEL = 1;

    boolean alive = true;

    GoundMonsterType type = GoundMonsterType.OWL;

    int state = FORWARD;
    float stateTime = 0;
    Map map;
    Rectangle bounds = new Rectangle();

    Vector2 vel = new Vector2();
    Vector2 pos = new Vector2();

    public GroundMonster(Map map, float x, float y, GoundMonsterType type) {
        this.type = type;
        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;

        vel.set(1, 0);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        bounds.x += vel.x * deltaTime;
        bounds.y += vel.y * deltaTime;

        boolean change = false;
        int y = map.tiles[0].length - 1 - (int) Math.floor(bounds.y);
        if (state == FORWARD) {
            // System.out.println("fw");
            change = map.isColidable(map.tiles[(int) Math.ceil(bounds.x)][y]);
            change = change || map.tiles[(int) Math.ceil(bounds.x)][y + 1] == 0;
        } else {
            // System.out.println("bw");
            change = map.isColidable(map.tiles[(int) Math.floor(bounds.x)][y]);
            change = change || map.tiles[(int) Math.floor(bounds.x)][y + 1] == 0;
        }
        if (change) {
            bounds.x -= vel.x * deltaTime;
            bounds.y -= vel.y * deltaTime;
            state = -state;
            vel.scl(-1);
            if (state == FORWARD)
                vel.nor().scl(FORWARD_VEL);
            if (state == BACKWARD)
                vel.nor().scl(BACKWARD_VEL);
        }

        if (map.giana.killerBounds.overlaps(bounds) && !map.demo) {
            if (map.giana.state != GianaState.DYING && alive && type.canBeKilled) {
                alive = false;
                map.score += 50;
                Sounds.getInstance().play(Sfx.KILL);
            }
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
