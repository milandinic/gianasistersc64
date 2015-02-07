package com.mdinic.game.giana;

import com.badlogic.gdx.math.Vector2;

public class SmallDiamoind {

    float stateTime = 0;
    Vector2 pos = new Vector2();
    Vector2 startPos = new Vector2();
    GameMap map;
    boolean active = true;

    boolean initSecondFly = true;

    public SmallDiamoind(GameMap map, float x, float y) {
        super();
        this.map = map;
        pos.x = x + 0.3f;
        pos.y = y;

        startPos.x = x + 0.3f;
        startPos.y = y;
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;

        if (stateTime < 0.3f) {
            pos.y += 0.1f;
        } else if (stateTime < 0.5f) {
        } else if (stateTime < 0.8f) {
            if (initSecondFly) {
                initSecondFly = false;
                pos = startPos;
            }
            pos.y += 0.1f;
        } else {
            active = false;
        }
    }
}
