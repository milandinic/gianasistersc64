package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.Vector3;
import com.mdinic.game.giana.Giana.GianaState;

public class MapRenderer {
    Map map;
    OrthographicCamera cam;
    SpriteCache cache;
    SpriteBatch batch = new SpriteBatch(5460);
    ImmediateModeRenderer20 renderer = new ImmediateModeRenderer20(false, true, 0);
    int[][] blocks;
    TextureRegion tile;
    Animation bobLeft;
    Animation bobRight;
    Animation bobJumpLeft;
    Animation bobJumpRight;
    Animation bobIdleLeft;
    Animation bobIdleRight;
    Animation bobDead;
    Animation zap;
    Animation diamondAnim;
    Animation treatBoxAnim;

    TextureRegion usedTreatBox;

    // TextureRegion dispenser;
    Animation spawn;
    TextureRegion dying;
    // TextureRegion spikes;
    // Animation rocket;
    // Animation rocketExplosion;
    // TextureRegion rocketPad;
    TextureRegion endDoor;
    // TextureRegion movingSpikesOld;
    // TextureRegion laser;
    FPSLogger fps = new FPSLogger();
    private Animation movingSpikesAnim;

    private Animation owlAnim;
    private Animation jellyAnim;
    private Animation lobsterAnim;

    public MapRenderer(Map map) {
        this.map = map;
        this.cam = new OrthographicCamera(16, 16);
        this.cam.position.set(map.giana.pos.x, map.giana.pos.y, 0);
        this.cache = new SpriteCache(this.map.tiles.length * this.map.tiles[0].length, false);
        this.blocks = new int[(int) Math.ceil(this.map.tiles.length / 24.0f)][(int) Math
                .ceil(this.map.tiles[0].length / 16.0f)];

        createAnimations();
        createBlocks();
    }

    private void createBlocks() {
        int width = map.tiles.length;
        int height = map.tiles[0].length;
        for (int blockY = 0; blockY < blocks[0].length; blockY++) {
            for (int blockX = 0; blockX < blocks.length; blockX++) {
                cache.beginCache();
                for (int y = blockY * 16; y < blockY * 16 + 16; y++) {
                    for (int x = blockX * 24; x < blockX * 24 + 24; x++) {
                        if (x > width)
                            continue;
                        if (y > height)
                            continue;
                        int posX = x;
                        int posY = height - y - 1;
                        if (map.match(map.tiles[x][y], Map.TILE))
                            cache.add(tile, posX, posY, 1, 1);

                        // if (map.match(map.tiles[x][y], Map.MOVING_SPIKES))
                        // cache.add(spikes, posX, posY, 1, 1);
                    }
                }
                blocks[blockX][blockY] = cache.endCache();
            }
        }
        Gdx.app.debug("GianaSisters", "blocks created");
    }

    private void createAnimations() {
        Texture sprites = new Texture(Gdx.files.internal("data/sprites.png"));
        this.tile = new TextureRegion(sprites, 150, 103, 24, 16);
        this.endDoor = new TextureRegion(sprites, 16, 196, 32, 32);

        Texture gianaTexture = new Texture(Gdx.files.internal("data/giana.png"));
        Texture diamondTexture = new Texture(Gdx.files.internal("data/diamond.png"));
        Texture treatboxTexture = new Texture(Gdx.files.internal("data/treatbox.png"));
        Texture movingSpikesTexture = new Texture(Gdx.files.internal("data/movingspikes.png"));

        Texture groundMonstersTexture = new Texture(Gdx.files.internal("data/groundmonsters.png"));
        Texture lobsterTexture = new Texture(Gdx.files.internal("data/lobster.png"));

        TextureRegion groundMonstersRegion = new TextureRegion(groundMonstersTexture);
        groundMonstersRegion.setRegion(0, 0, 240, 20);

        owlAnim = new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]);
        groundMonstersRegion.setRegion(240, 0, 240, 20);
        jellyAnim = new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]);

        lobsterAnim = new Animation(0.2f, new TextureRegion(lobsterTexture).split(24, 20)[0]);

        movingSpikesAnim = new Animation(0.3f, new TextureRegion(movingSpikesTexture).split(48, 16)[0]);

        diamondAnim = new Animation(0.3f, new TextureRegion(diamondTexture).split(16, 16)[0]);
        TextureRegion[] treatboxRegion = new TextureRegion(treatboxTexture).split(30, 20)[0];
        usedTreatBox = treatboxRegion[treatboxRegion.length - 1];
        List<TextureRegion> tbArray = new ArrayList<TextureRegion>();
        for (int i = 0; i < treatboxRegion.length; i++) {
            tbArray.add(treatboxRegion[i]);
        }
        for (int i = treatboxRegion.length - 1; i >= 0; i--) {
            tbArray.add(treatboxRegion[i]);
        }
        treatBoxAnim = new Animation(0.2f, tbArray.toArray(new TextureRegion[10]));

        TextureRegion gianaRegion = new TextureRegion(gianaTexture);
        gianaRegion.setRegion(0, 0, 189, 28);
        TextureRegion[] gianaSmallRight = gianaRegion.split(27, 28)[0];
        gianaRegion.setRegion(0, 29, 189, 28);
        TextureRegion[] gianaSmallLeft = gianaRegion.split(27, 28)[0];

        gianaRegion.setRegion(0, 59, 27, 28);
        dying = gianaRegion.split(27, 28)[0][0];
        bobDead = new Animation(0.2f, dying);

        bobRight = new Animation(0.1f, gianaSmallRight[1], gianaSmallRight[2], gianaSmallRight[3], gianaSmallRight[4]);
        bobLeft = new Animation(0.1f, gianaSmallLeft[1], gianaSmallLeft[2], gianaSmallLeft[3], gianaSmallLeft[4]);

        bobJumpRight = new Animation(0.1f, gianaSmallRight[5]);
        bobJumpLeft = new Animation(0.1f, gianaSmallLeft[5]);

        bobIdleRight = new Animation(0.5f, gianaSmallRight[0]);
        bobIdleLeft = new Animation(0.5f, gianaSmallLeft[0]);

        spawn = new Animation(0.1f, gianaSmallRight[0]);
    }

    float stateTime = 0;
    Vector3 lerpTarget = new Vector3();

    public void render(float deltaTime) {

        float camX = map.giana.pos.x;
        if (camX < 7) {
            camX = 7;

        }

        if (camX > 137) {
            camX = 137;
        }

        cam.position.lerp(lerpTarget.set(camX, 153, 0), 4f * deltaTime);
        cam.update();

        cache.setProjectionMatrix(cam.combined);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        cache.begin();
        // int b = 0;
        for (int blockY = 0; blockY < 4; blockY++) {
            for (int blockX = 0; blockX < 6; blockX++) {
                cache.draw(blocks[blockX][blockY]);
                // b++;
            }
        }
        cache.end();
        stateTime += deltaTime;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        if (map.endDoor != null)
            batch.draw(endDoor, map.endDoor.bounds.x, map.endDoor.bounds.y, 2, 2);

        renderMovingSpikes();
        renderGroundMonsters();

        renderBob();
        renderDiamonds();
        renderTreatBoxes();

        batch.end();

        // fps.log();
    }

    private void renderBob() {
        Animation anim = null;
        boolean loop = true;
        if (map.giana.state == GianaState.RUN) {
            if (map.giana.dir == Giana.LEFT)
                anim = bobLeft;
            else
                anim = bobRight;
        }
        if (map.giana.state == GianaState.IDLE) {
            if (map.giana.dir == Giana.LEFT)
                anim = bobIdleLeft;
            else
                anim = bobIdleRight;
        }
        if (map.giana.state == GianaState.JUMP) {
            if (map.giana.dir == Giana.LEFT)
                anim = bobJumpLeft;
            else
                anim = bobJumpRight;
        }
        if (map.giana.state == GianaState.SPAWN) {
            anim = spawn;
            loop = false;
        }
        if (map.giana.state == GianaState.DYING) {
            batch.draw(dying, map.giana.pos.x, map.giana.pos.y, 1, 1);
            return;
        }
        if (map.giana.active)
            batch.draw(anim.getKeyFrame(map.giana.stateTime, loop), map.giana.pos.x, map.giana.pos.y, 1, 1);
    }

    private void renderDiamonds() {
        for (Diamond currentDiamond : map.diamonds) {
            if (currentDiamond.active) {
                batch.draw(diamondAnim.getKeyFrame(currentDiamond.stateTime, true), currentDiamond.pos.x,
                        currentDiamond.pos.y, 0.8f, 0.8f);
            }
        }
    }

    private void renderTreatBoxes() {
        for (TreatBox box : map.treatBoxes) {
            if (box.active) {
                batch.draw(treatBoxAnim.getKeyFrame(box.stateTime, true), box.pos.x, box.pos.y, 1, 1);
            } else {
                batch.draw(usedTreatBox, box.pos.x, box.pos.y, 1, 1);
            }
        }
    }

    private void renderMovingSpikes() {
        for (int i = 0; i < map.movingSpikes.size; i++) {
            MovingSpikes spikes = map.movingSpikes.get(i);
            batch.draw(movingSpikesAnim.getKeyFrame(spikes.stateTime, true), spikes.pos.x, spikes.pos.y, 3, 1);
        }
    }

    private void renderGroundMonsters() {
        for (int i = 0; i < map.groundMonsters.size; i++) {
            GroundMonster monster = map.groundMonsters.get(i);
            Animation anim = null;
            switch (monster.type) {
            case JELLY:
                anim = jellyAnim;
                break;
            case LOBSTER:
                anim = lobsterAnim;
                break;
            case OWL:
                anim = owlAnim;
                break;
            default:
                throw new IllegalStateException("ground monster type is not supported " + monster.type);
            }
            if (monster.alive) {
                batch.draw(anim.getKeyFrame(monster.stateTime, true), monster.pos.x, monster.pos.y, 1, 1);
            }
        }
    }

    public void dispose() {
        cache.dispose();
        batch.dispose();
        tile.getTexture().dispose();
    }
}
