package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class TreatBox {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    float stateTime = 0;
    boolean active = true;
    Map map;

    enum TreatType {
        DIAMOND, BALL
    };

    TreatType type = TreatType.DIAMOND;

    public TreatBox(Map map, float x, float y, TreatType type) {
        super();
        this.type = type;
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

            switch (type) {
            case DIAMOND:
                if (map.giana.headHitBounds.overlaps(bounds)) {
                    active = false;
                    map.score += 25;
                    map.diamondsCollected++;
                    map.treatSmallDiamoinds.add(new SmallDiamoind(map, pos.x, pos.y));
                }
                break;
            case BALL:
                if (map.giana.headHitBounds.overlaps(bounds)) {
                    active = false;
                    map.treats.add(new Treat(map, pos.x, pos.y));
                }
                break;
            default:
                break;
            }
        }
    }

}
