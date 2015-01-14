package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Water {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    float stateTime = 0;
    boolean active = true;
    Map map;

    public Water(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        stateTime = 0;
        bounds.x = x;
        bounds.y = y;
        bounds.height = 1;
        bounds.width = 3;
    }

    public void update(float deltaTime) {
        if (active) {
            stateTime += deltaTime;

            if (map.giana.bounds.overlaps(bounds)) {
                if (map.giana.state != GianaState.DYING) {
                    map.giana.state = GianaState.DYING;
                    map.giana.stateTime = 0;
                }
            }
        }
    }
}
