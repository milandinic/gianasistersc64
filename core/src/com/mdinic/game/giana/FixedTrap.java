package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class FixedTrap {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    float stateTime = 0;
    Map map;

    FixedTrapType type;

    public FixedTrap(Map map, float x, float y, FixedTrapType type) {
        this.map = map;
        pos.x = type.moveHalfX ? x + 0.5f : x;
        pos.y = y;
        stateTime = 0;
        bounds.x = x;
        bounds.y = y;
        bounds.height = type.height;
        bounds.width = type.width;
        this.type = type;
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;

        if (map.giana.bounds.overlaps(bounds) && !map.demo) {
            if (map.giana.state != GianaState.DYING) {
                map.giana.state = GianaState.DYING;
                map.giana.stateTime = 0;
            }
        }
    }
}
