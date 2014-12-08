package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mdinic.game.giana.Sounds.Sfx;

public class Tile {

    Map map;
    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    boolean active = true;
    float stateTime = 0;

    enum State {
        NORMAL, EXPLODING, GONE
    };

    State state = State.NORMAL;

    public Tile(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = this.bounds.height = 1;
    }

    public void update(float deltaTime) {
        if (active) {
            stateTime += deltaTime;
            switch (state) {
            case NORMAL:
                if (map.giana.headHitBounds.overlaps(bounds) && map.giana.big && map.giana.state == GianaState.JUMP) {
                    state = State.EXPLODING;
                    Sounds.getInstance().play(Sfx.BRICK_DESTROY);
                    stateTime = 0;
                }
                break;
            case EXPLODING:
                if (stateTime > 0.3f) {
                    state = State.GONE;
                }
                break;
            case GONE:
                active = false;
                map.tiles[(int) pos.x][map.tiles[0].length - 1 - (int) pos.y] = 0;
                break;
            default:
                break;
            }
        }
    }
}
