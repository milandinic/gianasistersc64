package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mdinic.game.giana.Sounds.Sfx;

public class Tile {

    GameMap map;
    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    boolean active = true;
    float stateTime = 0;

    enum State {
        NORMAL, EXPLODING, GONE
    };

    State state = State.NORMAL;

    public Tile(GameMap map, float x, float y) {
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
                if (map.giana.headHitBounds.overlaps(bounds) && GianaPower.isBig(map.giana.power)
                        && map.giana.state == GianaState.JUMP) {
                    state = State.EXPLODING;
                    map.sounds.play(Sfx.BRICK_DESTROY);
                    stateTime = 0;
                }
                break;
            case EXPLODING:
                if (stateTime > 0.1f) {
                    map.tiles[(int) pos.x][map.tiles[0].length - 1 - (int) pos.y] = 0;
                }
                if (stateTime > 0.4f) {
                    state = State.GONE;
                }
                break;
            case GONE:
                active = false;
                break;
            default:
                break;
            }
        }
    }
}
