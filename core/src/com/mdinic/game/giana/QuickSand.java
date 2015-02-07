package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class QuickSand {
    GameMap map;
    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    boolean active = true;
    float stateTime = 0;

    enum State {
        NORMAL, GONE
    };

    State state = State.NORMAL;

    public QuickSand(GameMap map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        bounds.x = x;
        bounds.y = y;
        bounds.width = 1f;
        bounds.height = 1f;
    }

    public void update(float deltaTime) {

        if (active) {
            switch (state) {
            case NORMAL:
                if (map.giana.killerBounds.overlaps(bounds)) {
                    stateTime += deltaTime;
                    if (stateTime > 0.2f) {
                        state = State.GONE;
                    }
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
