package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mdinic.game.giana.Bullet.BulletState;
import com.mdinic.game.giana.Sounds.Sfx;

public class Monster {

    public boolean alive = true;

    public float stateTime = 0;
    public GameMap map;
    public Rectangle bounds = new Rectangle();

    public Vector2 vel = new Vector2();
    public Vector2 pos = new Vector2();

    public int fx = 0;
    public int bx = 0;

    public Monster(GameMap map, float x, float y) {

        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;

    }

    protected void killByGiana(boolean canBeKilled) {
        boolean hit = (map.giana.bullet.active && map.giana.bullet.bounds.overlaps(bounds))
                || map.giana.killerBounds.overlaps(bounds);
        if (hit && !map.demo) {
            if (map.giana.state != GianaState.DYING && alive) {
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
    }

    protected void tryToKilGiana() {
        if (alive && map.giana.bounds.overlaps(bounds) && !map.demo) {
            if (map.giana.state != GianaState.DYING) {
                map.giana.state = GianaState.DYING;
                map.giana.stateTime = 0;
            }
        }
    }
}
