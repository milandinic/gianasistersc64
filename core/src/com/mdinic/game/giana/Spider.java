package com.mdinic.game.giana;

import com.mdinic.game.giana.Bullet.BulletState;
import com.mdinic.game.giana.Sounds.Sfx;

public class Spider extends Monster {

    private static final int SPIDER_HITS_TO_KILL = 30;

    BossState state = BossState.WAIT;

    int hits = 0;

    enum BossState {
        ATACK, RETREAT, STAY, WAIT, GIANA_ON_SIGHT
    }

    public Spider(GameMap map, float x, float y) {
        super(map, x, y);
        bounds.width = 2;
        bounds.height = 1;

        vel.set(-1, 0);
    }

    public void update(float deltaTime) {
        if (alive) {

            stateTime += deltaTime;
            bounds.x += vel.x * deltaTime;
            bounds.y += vel.y * deltaTime;

            float attactPos = pos.x - GameMap.MAP_HEIGHT / 1.7f;
            switch (state) {
            case WAIT:
                vel.set(0, 0);
                if (map.giana.pos.x > attactPos) {
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
                if (stateTime > 1) {
                    state = BossState.RETREAT;
                    stateTime = 0;
                } else {
                    vel.set(-10, 0);
                }
                break;
            case RETREAT:
                if (stateTime > 2) {

                    if (map.giana.pos.x < attactPos) {
                        state = BossState.WAIT;
                    } else {
                        state = BossState.STAY;
                    }
                    stateTime = 0;
                } else {
                    vel.set(5, 0);
                }
                break;
            default:
                break;
            }

            killByGiana(true);

            tryToKillGiana();

            pos.x = bounds.x;
            pos.y = bounds.y;
        }
    }

    @Override
    protected void killByGiana(boolean canBeKilled) {
        boolean hit = (map.giana.bullet.active && map.giana.bullet.bounds.overlaps(bounds) && map.giana.bullet.state == BulletState.FLY);

        if (hit && alive && !map.demo) {
            hits++;
            System.out.println(hits);

            if (hits == SPIDER_HITS_TO_KILL) {
                alive = false;
                map.score += 10000;
                map.sounds.play(Sfx.SPIDER_DEAD);
            }

            if (map.giana.bullet.state != BulletState.EXPLODE) {
                map.giana.bullet.time = 0;
                map.giana.bullet.state = BulletState.EXPLODE;
            }
        }

    }
    // spider 0xeef11c

}
