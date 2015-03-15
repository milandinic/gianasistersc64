package com.mdinic.game.giana;

import com.mdinic.game.giana.Bullet.BulletState;
import com.mdinic.game.giana.Sounds.Sfx;

public class Boss extends Monster {

    BossState state = BossState.WAIT;

    enum BossState {
        ATACK, RETREAT, STAY, WAIT, GIANA_ON_SIGHT
    }

    public Boss(GameMap map, float x, float y) {
        super(map, x, y);
        bounds.width = 2;
        bounds.height = 1;

        vel.set(-1, 0);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        bounds.x += vel.x * deltaTime;
        bounds.y += vel.y * deltaTime;

        switch (state) {
        case WAIT:
            vel.set(0, 0);
            if (map.giana.pos.x > pos.x - GameMap.MAP_HEIGHT / 2) {
                state = BossState.GIANA_ON_SIGHT;
            }
            break;
        case GIANA_ON_SIGHT:
            state = BossState.STAY;
            stateTime = 0;
            break;
        case STAY:
            if (stateTime < 0.5) {
                vel.set(-2, 0);
            } else if (stateTime < 1) {
                vel.set(2, 0);
            } else {
                state = BossState.ATACK;
                stateTime = 0;
            }

            break;
        case ATACK:
            if (stateTime > 2) {
                state = BossState.RETREAT;
                stateTime = 0;
            } else {
                vel.set(-4, 0);
            }
            break;
        case RETREAT:
            if (stateTime > 3) {
                state = BossState.STAY;
                stateTime = 0;
            } else {
                vel.set(2, 0);
            }
            break;
        default:
            break;
        }

        // boolean change = false;
        // int y = map.tiles[0].length - 1 - (int) Math.floor(bounds.y);
        // if (state == FORWARD) {
        // // System.out.println("fw");
        // change = map.isColidable(map.tiles[(int) Math.ceil(bounds.x)][y]);
        // change = change || map.tiles[(int) Math.ceil(bounds.x)][y + 1] == 0;
        // } else {
        // // System.out.println("bw");
        // change = map.isColidable(map.tiles[(int) Math.floor(bounds.x)][y]);
        // change = change || map.tiles[(int) Math.floor(bounds.x)][y + 1] == 0;
        // }
        // if (change) {
        // bounds.x -= vel.x * deltaTime;
        // bounds.y -= vel.y * deltaTime;
        // state = -state;
        // vel.scl(-1);
        // if (state == FORWARD)
        // vel.nor().scl(FORWARD_VEL);
        // if (state == BACKWARD)
        // vel.nor().scl(BACKWARD_VEL);
        // }
        //
        // killByGiana(type.canBeKilled);

        tryToKillGiana();

        pos.x = bounds.x;
        pos.y = bounds.y;

    }

    @Override
    protected void killByGiana(boolean canBeKilled) {
        boolean hit = (map.giana.bullet.active && map.giana.bullet.bounds.overlaps(bounds));
        if (hit && alive && !map.demo) {

            if (canBeKilled) {
                alive = false;
                map.score += 50;
                map.sounds.play(Sfx.KILL);
            }

            if (map.giana.bullet.state != BulletState.EXPLODE) {
                map.giana.bullet.time = 0;
                map.giana.bullet.state = BulletState.EXPLODE;
            }
        }

    }
    // spider 0xeef11c

}
