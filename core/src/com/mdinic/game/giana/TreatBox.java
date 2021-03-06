package com.mdinic.game.giana;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mdinic.game.giana.Sounds.Sfx;

public class TreatBox {

    Vector2 pos = new Vector2();
    Rectangle bounds = new Rectangle();
    float stateTime = 0;
    boolean active = true;
    GameMap map;

    enum TreatType {
        DIAMOND, BALL
    };

    TreatType type = TreatType.DIAMOND;

    public TreatBox(GameMap map, float x, float y, TreatType type) {
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
                    diamond();
                }
                break;
            case BALL:
                if (map.giana.headHitBounds.overlaps(bounds)) {
                    if (map.giana.power.hasNext()) {
                        active = false;
                        map.treats.add(new Treat(map, pos.x, pos.y));
                    } else {
                        diamond();
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    private void diamond() {
        map.sounds.play(Sfx.TREAT_BOX_COIN);
        active = false;
        map.collectDiamound();
        map.treatSmallDiamoinds.add(new SmallDiamoind(map, pos.x, pos.y));
    }
}
