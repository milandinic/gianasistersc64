package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Bullet {

    static final float VELOCITY = 22;

    static final float DISTANCE = 8;

    GameMap map;
    boolean homing = true;
    float time = 0;
    Vector2 startPos = new Vector2();
    Vector2 pos = new Vector2();
    Vector2 vel = new Vector2();
    Rectangle bounds = new Rectangle();

    Monster victimMonster = null;
    Vector2 target;

    enum BulletState {
        FIND_VICTIM, FLY, EXPLODE
    };

    BulletState state = BulletState.FIND_VICTIM;

    int sign;
    boolean active = true;

    public Bullet(GameMap map, Vector2 gianaPos, int sign, boolean homing) {
        float x = gianaPos.x;
        float y = gianaPos.y + 0.7f;
        this.map = map;
        this.startPos.set(x, y);
        this.pos.set(x, y);
        this.bounds.x = x + 0.2f;
        this.bounds.y = y + 0.2f;
        this.bounds.width = 0.3f;
        this.bounds.height = 0.3f;
        this.vel.set(sign * VELOCITY, 0);
        this.sign = sign;
        this.target = new Vector2();
        this.homing = homing;
    }

    boolean sameSign(float x, float y) {
        return (x >= 0) ^ (y < 0);
    }

    void findVictim() {

        Vector2 giana = map.giana.pos;
        // find closest alive monster

        for (GroundMonster monster : map.groundMonsters) {
            if (monster.alive) {

                Vector2 v = new Vector2(monster.pos);
                v.sub(giana);

                if (sameSign(v.x, -sign) && v.x < DISTANCE && v.y < DISTANCE) {
                    if (victimMonster == null) {
                        victimMonster = monster;
                    } else {
                        checkIsNewVictim(giana, monster);
                    }
                }
            }
        }

        for (Bee bee : map.bees) {
            if (bee.alive) {

                Vector2 v = new Vector2(bee.pos);
                v.sub(giana);

                if (sameSign(v.x, -sign) && v.x < DISTANCE && v.y < DISTANCE) {
                    if (victimMonster == null) {
                        victimMonster = bee;
                    } else {
                        checkIsNewVictim(giana, bee);
                    }
                }
            }
        }
    }

    private void checkIsNewVictim(Vector2 giana, Monster monster) {
        Vector2 v2 = new Vector2(victimMonster.pos);
        Vector2 v3 = new Vector2(monster.pos);

        if (v2.sub(giana).len2() > v3.sub(giana).len2()) {
            victimMonster = monster;
        }
    }

    public void update(float deltaTime) {
        time += deltaTime;
        switch (state) {
        case FIND_VICTIM:
            if (homing)
                findVictim();

            state = BulletState.FLY;

            if (victimMonster == null) {
                target = new Vector2(map.giana.pos);
                target.add(-sign * DISTANCE + 2, 0);
            }

            break;
        case FLY:

            break;
        case EXPLODE:
            if (time > 0.1f) {
                active = false;
                state = BulletState.FLY;
            }
            return;
        default:
            // do nothing
            return;
        }

        if (victimMonster != null) {
            target = victimMonster.pos;
        }

        vel.set(target);

        if (victimMonster == null && Math.round(vel.x) == Math.round(pos.x)) {
            active = false;
            return;
        }
        vel.sub(pos).nor().scl(VELOCITY);
        pos.add(vel.x * deltaTime, vel.y * deltaTime);
        if (pos.x < 0) {
            active = false;
            return;
        }
        bounds.x = pos.x;
        bounds.y = pos.y;
        if (checkHit()) {
            active = false;

        }
    }

    Rectangle[] r = { new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle() };

    private boolean checkHit() {
        fetchCollidableRects();
        for (int i = 0; i < r.length; i++) {
            if (bounds.overlaps(r[i])) {
                return true;
            }
        }
        return false;
    }

    private void fetchCollidableRects() {
        int p1x = (int) bounds.x;
        int p1y = (int) Math.floor(bounds.y);
        int p2x = (int) (bounds.x + bounds.width);
        int p2y = (int) Math.floor(bounds.y);
        int p3x = (int) (bounds.x + bounds.width);
        int p3y = (int) (bounds.y + bounds.height);
        int p4x = (int) bounds.x;
        int p4y = (int) (bounds.y + bounds.height);

        if (map.tiles[0].length - 2 - p1y < 0) {
            return;
        }

        int[][] tiles = map.tiles;
        int tile1 = tiles[p1x][map.tiles[0].length - 1 - p1y];
        int tile2 = tiles[p2x][map.tiles[0].length - 1 - p2y];
        int tile3 = tiles[p3x][map.tiles[0].length - 1 - p3y];
        int tile4 = tiles[p4x][map.tiles[0].length - 1 - p4y];

        if (tile1 != GameMap.EMPTY)
            r[0].set(p1x, p1y, 1, 1);
        else
            r[0].set(-1, -1, 0, 0);
        if (tile2 != GameMap.EMPTY)
            r[1].set(p2x, p2y, 1, 1);
        else
            r[1].set(-1, -1, 0, 0);
        if (tile3 != GameMap.EMPTY)
            r[2].set(p3x, p3y, 1, 1);
        else
            r[2].set(-1, -1, 0, 0);
        if (tile4 != GameMap.EMPTY)
            r[3].set(p4x, p4y, 1, 1);
        else
            r[3].set(-1, -1, 0, 0);
    }
}
