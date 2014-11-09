package com.badlogic.cubocy;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.cubocy.Giana.GianaState;
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

    TextureRegion dispenser;
    Animation spawn;
    Animation dying;
    TextureRegion spikes;
    Animation rocket;
    Animation rocketExplosion;
    TextureRegion rocketPad;
    TextureRegion endDoor;
    TextureRegion movingSpikes;
    TextureRegion laser;
    FPSLogger fps = new FPSLogger();

    public MapRenderer(Map map) {
        this.map = map;
        this.cam = new OrthographicCamera(24, 16);
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
                        if (map.match(map.tiles[x][y], Map.SPIKES))
                            cache.add(spikes, posX, posY, 1, 1);
                    }
                }
                blocks[blockX][blockY] = cache.endCache();
            }
        }
        Gdx.app.debug("Cubocy", "blocks created");
    }

    private void createAnimations() {
        this.tile = new TextureRegion(new Texture(Gdx.files.internal("data/sprites.png")), 150, 103, 24, 16);
        Texture bobTexture = new Texture(Gdx.files.internal("data/bob.png"));
        Texture gianaTexture = new Texture(Gdx.files.internal("data/giana.png"));
        Texture diamondTexture = new Texture(Gdx.files.internal("data/diamond.png"));
        Texture treatboxTexture = new Texture(Gdx.files.internal("data/treatbox.png"));

        diamondAnim = new Animation(0.2f, new TextureRegion(diamondTexture).split(16, 16)[0]);
        TextureRegion[] treatboxRegion = new TextureRegion(treatboxTexture).split(30, 20)[0];
        List<TextureRegion> tbArray = new ArrayList<TextureRegion>();
        for (int i = 0; i < treatboxRegion.length; i++) {
            tbArray.add(treatboxRegion[i]);
        }
        for (int i = treatboxRegion.length - 1; i >= 0; i--) {
            tbArray.add(treatboxRegion[i]);
        }
        treatBoxAnim = new Animation(0.3f, tbArray.toArray(new TextureRegion[10]));

        TextureRegion gianaRegion = new TextureRegion(gianaTexture);
        gianaRegion.setRegion(0, 0, 189, 28);
        TextureRegion[] gianaSmallRight = gianaRegion.split(27, 28)[0];
        gianaRegion.setRegion(0, 29, 189, 28);
        TextureRegion[] gianaSmallLeft = gianaRegion.split(27, 28)[0];

        TextureRegion[] split = new TextureRegion(bobTexture).split(20, 20)[0];
        TextureRegion[] mirror = new TextureRegion(bobTexture).split(20, 20)[0];
        for (TextureRegion region : mirror)
            region.flip(true, false);
        spikes = split[5];

        bobRight = new Animation(0.1f, gianaSmallRight[1], gianaSmallRight[2], gianaSmallRight[3], gianaSmallRight[4]);
        bobLeft = new Animation(0.1f, gianaSmallLeft[1], gianaSmallLeft[2], gianaSmallLeft[3], gianaSmallLeft[4]);

        bobJumpRight = new Animation(0.1f, gianaSmallRight[5]);
        bobJumpLeft = new Animation(0.1f, gianaSmallLeft[5]);

        bobIdleRight = new Animation(0.5f, gianaSmallRight[0]);
        bobIdleLeft = new Animation(0.5f, gianaSmallLeft[0]);

        bobDead = new Animation(0.2f, split[0]);
        split = new TextureRegion(bobTexture).split(20, 20)[1];

        split = new TextureRegion(bobTexture).split(20, 20)[2];
        spawn = new Animation(0.1f, split[4], split[3], split[2], split[1]);
        dying = new Animation(0.1f, split[1], split[2], split[3], split[4]);
        dispenser = split[5];
        split = new TextureRegion(bobTexture).split(20, 20)[3];
        rocket = new Animation(0.1f, split[0], split[1], split[2], split[3]);
        rocketPad = split[4];
        split = new TextureRegion(bobTexture).split(20, 20)[4];
        rocketExplosion = new Animation(0.1f, split[0], split[1], split[2], split[3], split[4], split[4]);
        split = new TextureRegion(bobTexture).split(20, 20)[5];
        endDoor = split[2];
        movingSpikes = split[0];
        laser = split[1];
    }

    float stateTime = 0;
    Vector3 lerpTarget = new Vector3();

    public void render(float deltaTime) {

        cam.position.lerp(lerpTarget.set(map.giana.pos.x, 153, 0), 4f * deltaTime);

        cam.update();

        renderLaserBeams();

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
        renderDispensers();
        if (map.endDoor != null)
            batch.draw(endDoor, map.endDoor.bounds.x, map.endDoor.bounds.y, 1, 1);
        renderLasers();
        renderMovingSpikes();
        renderBob();
        renderDiamonds();
        renderTreatBoxes();

        renderRockets();
        batch.end();
        renderLaserBeams();

        fps.log();
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
            anim = dying;
            loop = false;
        }
        batch.draw(anim.getKeyFrame(map.giana.stateTime, loop), map.giana.pos.x, map.giana.pos.y, 1, 1);
    }

    private void renderRockets() {
        for (int i = 0; i < map.rockets.size; i++) {
            Rocket rocket = map.rockets.get(i);
            if (rocket.state == Rocket.FLYING) {
                TextureRegion frame = this.rocket.getKeyFrame(rocket.stateTime, true);
                batch.draw(frame, rocket.pos.x, rocket.pos.y, 0.5f, 0.5f, 1, 1, 1, 1, rocket.vel.angle());
            } else {
                TextureRegion frame = this.rocketExplosion.getKeyFrame(rocket.stateTime, false);
                batch.draw(frame, rocket.pos.x, rocket.pos.y, 1, 1);
            }
            batch.draw(rocketPad, rocket.startPos.x, rocket.startPos.y, 1, 1);
        }
    }

    private void renderDispensers() {
        for (int i = 0; i < map.dispensers.size; i++) {
            Dispenser dispenser = map.dispensers.get(i);
            batch.draw(this.dispenser, dispenser.bounds.x, dispenser.bounds.y, 1, 1);
        }
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
            batch.draw(treatBoxAnim.getKeyFrame(box.stateTime, true), box.pos.x, box.pos.y, 1, 1);
        }
    }

    private void renderMovingSpikes() {
        for (int i = 0; i < map.movingSpikes.size; i++) {
            MovingSpikes spikes = map.movingSpikes.get(i);
            batch.draw(movingSpikes, spikes.pos.x, spikes.pos.y, 0.5f, 0.5f, 1, 1, 1, 1, spikes.angle);
        }
    }

    private void renderLasers() {
        for (int i = 0; i < map.lasers.size; i++) {
            Laser laser = map.lasers.get(i);
            batch.draw(this.laser, laser.pos.x, laser.pos.y, 0.5f, 0.5f, 1, 1, 1, 1, laser.angle);
        }
    }

    private void renderLaserBeams() {
        cam.update(false);
        renderer.begin(cam.combined, GL20.GL_LINES);
        for (int i = 0; i < map.lasers.size; i++) {
            Laser laser = map.lasers.get(i);
            float sx = laser.startPoint.x, sy = laser.startPoint.y;
            float ex = laser.cappedEndPoint.x, ey = laser.cappedEndPoint.y;
            renderer.color(0, 1, 0, 1);
            renderer.vertex(sx, sy, 0);
            renderer.color(0, 1, 0, 1);
            renderer.vertex(ex, ey, 0);
        }
        renderer.end();
    }

    public void dispose() {
        cache.dispose();
        batch.dispose();
        tile.getTexture().dispose();
    }
}
