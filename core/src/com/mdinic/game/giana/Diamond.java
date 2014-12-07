package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Diamond {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    float stateTime = 0;
    boolean active = true;
    Map map;

    public Diamond(Map map, float x, float y) {
        this.map = map;
        pos.x = x;
        pos.y = y;
        stateTime = 0;
        bounds.x = x;
        bounds.y = y;
        bounds.width = bounds.height = 1;

    }

    public void update(float deltaTime) {
        if (active) {
            stateTime += deltaTime;

            if (map.giana.bounds.overlaps(bounds)) {
                MapResource.getInstance().getDiamondSfx().play();
                active = false;
                map.collectDiamound();
            }
        }
    }
}
