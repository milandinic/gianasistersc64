package com.mdinic.game.giana;

public class GroundMonster extends Monster {
    static final int FORWARD = 1;
    static final int BACKWARD = -1;
    static final float FORWARD_VEL = 1;
    static final float BACKWARD_VEL = 1;

    GoundMonsterType type = GoundMonsterType.OWL;

    int state = FORWARD;

    public GroundMonster(GameMap map, float x, float y, GoundMonsterType type) {
        super(map, x, y);
        this.type = type;
        this.bounds.width = type.width;
        this.bounds.height = type.height;
        vel.set(type.speed, 0);
    }

    public void update(float deltaTime) {
        stateTime += deltaTime;
        bounds.x += vel.x * deltaTime;
        bounds.y += vel.y * deltaTime;

        boolean change = false;
        int y = map.tiles[0].length - 1 - (int) Math.floor(bounds.y);
        // tileAt guards both axes: x can leave the grid at a map edge and y+1
        // can step past the top row, either of which would otherwise throw.
        if (state == FORWARD) {
            int x = (int) Math.ceil(bounds.x);
            change = map.isColidable(map.tileAt(x, y, GameMap.EMPTY));
            change = change || map.tileAt(x, y + 1, GameMap.EMPTY) == 0;
        } else {
            int x = (int) Math.floor(bounds.x);
            change = map.isColidable(map.tileAt(x, y, GameMap.EMPTY));
            change = change || map.tileAt(x, y + 1, GameMap.EMPTY) == 0;
        }
        if (change) {
            bounds.x -= vel.x * deltaTime;
            bounds.y -= vel.y * deltaTime;
            state = -state;
            vel.scl(-1);
            if (state == FORWARD)
                vel.nor().scl(FORWARD_VEL);
            if (state == BACKWARD)
                vel.nor().scl(BACKWARD_VEL);
        }

        killByGiana(type.canBeKilled);

        tryToKillGiana();

        pos.x = bounds.x;
        pos.y = bounds.y;

    }
}
