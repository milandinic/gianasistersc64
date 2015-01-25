package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Vector3;

public class MapRenderer {

    public static final int SCENE_HEIGHT = 16;
    Map map;
    OrthographicCamera cam;

    SpriteCache cache;
    SpriteBatch batch = new SpriteBatch(5460);
    private final SpriteBatch fontBatch;

    int[][] blocks;
    TextureRegion tileTexture;

    Animation gianaLeft;
    Animation gianaRight;
    Animation gianaJumpLeft;
    Animation gianaJumpRight;
    Animation gianaIdleLeft;
    Animation gianaIdleRight;

    Animation gianaBigLeft;
    Animation gianaBigRight;
    Animation gianaBigJumpLeft;
    Animation gianaBigJumpRight;
    Animation gianaBigIdleLeft;
    Animation gianaBigIdleRight;

    Animation gianaDead;
    Animation gianaGrow;

    Animation smallDiamondAnim;

    Animation diamondAnim;
    Animation treatBoxAnim;

    TextureRegion usedTreatBox;
    TextureRegion spawn;
    TextureRegion dying;
    TextureRegion endDoor;

    TextureRegion bullet;
    TextureRegion bulletExplode;

    Animation movingSpikesAnim;

    java.util.Map<GoundMonsterType, Animation[]> groundMonsterAnimations = new HashMap<GoundMonsterType, Animation[]>();

    java.util.Map<GoundMonsterType, TextureRegion> deadGroundMonsterAnimations = new HashMap<GoundMonsterType, TextureRegion>();

    private BitmapFont font;

    int fontSize;

    java.util.Map<SimpleImageType, TextureRegion> simpleImageTextureRegions = new HashMap<SimpleImageType, TextureRegion>();
    private Animation treatBallRightAnim;
    private Animation treatBallLeftAnim;
    private Animation piranhaUpAnim;
    private Animation piranhaDownAnim;
    private Animation waspRightAnim;
    private Animation waspLeftAnim;
    private TextureRegion waspLeftDead;
    private TextureRegion waspRightDead;
    private Animation quicksandAnim;
    private Animation brickAnim;
    private Animation waterAnim;
    private Animation lightningAnim;
    private Animation doubleLightningAnim;
    private TextureRegion strawberry;

    public MapRenderer(Map map) {
        this.map = map;
        this.cam = new OrthographicCamera(20, SCENE_HEIGHT);

        fontBatch = new SpriteBatch();
        fontBatch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);

        this.cam.position.set(map.giana.pos.x, map.giana.pos.y, 0);

        this.cache = new SpriteCache(this.map.tiles.length * this.map.tiles[0].length, false);
        this.blocks = new int[(int) Math.ceil(this.map.tiles.length / 24.0f)][(int) Math
                .ceil(this.map.tiles[0].length / 16.0f)];

        createAnimations();
    }

    private void createAnimations() {
        LevelColors colors = LevelColors.values()[map.level];

        Texture brick = new Texture(Gdx.files.internal("data/bricks-" + colors.getBrickColor().getName() + ".png"));
        TextureRegion[] brickRegion = new TextureRegion(brick).split(24, 16)[0];
        brickAnim = new Animation(0.1f, brickRegion[1], brickRegion[2], brickRegion[3], brickRegion[4]);

        bullet = new TextureRegion(new Texture(Gdx.files.internal("data/bullet.png")));
        bulletExplode = new TextureRegion(new Texture(Gdx.files.internal("data/bulletexplode.png")));

        Texture sprites = new Texture(Gdx.files.internal("data/sprites.png"));
        this.tileTexture = brickRegion[0];
        this.endDoor = new TextureRegion(sprites, 16, 196, 32, 32);

        simpleImageTextureRegions.put(SimpleImageType.BIG_CLOUD, new TextureRegion(sprites, 13, 10, 42, 17));
        simpleImageTextureRegions.put(SimpleImageType.SMALL_CLOUD, new TextureRegion(sprites, 18, 46, 34, 17));
        simpleImageTextureRegions.put(SimpleImageType.MUSHROOM, new TextureRegion(sprites, 69, 44, 32, 24));
        simpleImageTextureRegions.put(SimpleImageType.ROUND_BUSH, new TextureRegion(sprites, 66, 7, 32, 22));
        simpleImageTextureRegions.put(SimpleImageType.WIDE_BUSH, new TextureRegion(sprites, 20, 83, 80, 20));
        simpleImageTextureRegions.put(SimpleImageType.COLUMN, new TextureRegion(sprites, 161, 259, 48, 25));
        simpleImageTextureRegions.put(SimpleImageType.FLOATING_COLUMN_UP, new TextureRegion(sprites, 164, 160, 48, 16));

        smallDiamondAnim = new Animation(0.1f, new TextureRegion(new Texture(
                Gdx.files.internal("data/smalldiamond.png"))).split(8, 8)[0]);

        waterAnim = new Animation(0.1f, new TextureRegion(new Texture(Gdx.files.internal("data/water.png"))).split(24,
                12)[0]);

        Texture gianaTexture = new Texture(Gdx.files.internal("data/giana.png"));
        Texture diamondTexture = new Texture(Gdx.files.internal("data/diamond.png"));
        Texture treatboxTexture = new Texture(Gdx.files.internal("data/treatbox.png"));
        Texture movingSpikesTexture = new Texture(Gdx.files.internal("data/movingspikes.png"));

        quicksandAnim = new Animation(0.1f, new TextureRegion(new Texture(Gdx.files.internal("data/quicksand-"
                + colors.getBrickColor().getName() + ".png"))).split(20, 20)[0]);

        Texture groundMonstersTexture = new Texture(Gdx.files.internal("data/groundmonsters.png"));
        Texture lobsterTexture = new Texture(Gdx.files.internal("data/lobster.png"));

        TextureRegion[] waspRightRegion = new TextureRegion(new Texture(Gdx.files.internal("data/wasp.png"))).split(24,
                20)[0];
        waspRightAnim = new Animation(0.3f, waspRightRegion);

        TextureRegion[] waspRegionLeft = new TextureRegion(new Texture(Gdx.files.internal("data/wasp.png"))).split(24,
                20)[0];

        waspRightDead = new TextureRegion(waspRightRegion[0]);
        waspRightDead.flip(false, true);
        waspLeftDead = new TextureRegion(waspRegionLeft[0]);
        waspLeftDead.flip(false, true);

        for (TextureRegion textureRegion : waspRegionLeft) {
            textureRegion.flip(true, false);
        }

        waspLeftAnim = new Animation(0.3f, waspRegionLeft);

        piranhaUpAnim = new Animation(0.2f,
                new TextureRegion(new Texture(Gdx.files.internal("data/piranha.png"))).split(20, 20)[0]);

        TextureRegion[] piranhaDownRegion = new TextureRegion(new Texture(Gdx.files.internal("data/piranha.png")))
                .split(20, 20)[0];
        for (TextureRegion textureRegion : piranhaDownRegion) {
            textureRegion.flip(false, true);
        }
        piranhaDownAnim = new Animation(0.2f, piranhaDownRegion);

        TextureRegion[] eyeRegionLeft = new TextureRegion(new Texture(Gdx.files.internal("data/eye.png")))
                .split(24, 17)[0];
        TextureRegion[] eyeRegionRight = new TextureRegion(new Texture(Gdx.files.internal("data/eye.png"))).split(24,
                17)[0];
        for (TextureRegion textureRegion : eyeRegionRight) {
            textureRegion.flip(true, false);
        }
        Animation eyeAnimLeft = new Animation(0.3f, eyeRegionLeft);
        Animation eyeAnimRight = new Animation(0.3f, eyeRegionRight);

        groundMonsterAnimations.put(GoundMonsterType.EYE, new Animation[] { eyeAnimRight, eyeAnimLeft });

        TextureRegion[] wormRegion = new TextureRegion(new Texture(Gdx.files.internal("data/worm.png"))).split(25, 21)[0];

        Animation wormAnimLeft = new Animation(0.1f, wormRegion);
        Animation wormAnimRight = new Animation(0.1f, wormRegion[6], wormRegion[5], wormRegion[4], wormRegion[3],
                wormRegion[2], wormRegion[1], wormRegion[0]);
        groundMonsterAnimations.put(GoundMonsterType.WORM, new Animation[] { wormAnimRight, wormAnimLeft });

        TextureRegion groundMonstersRegion = new TextureRegion(groundMonstersTexture);
        groundMonstersRegion.setRegion(0, 0, 240, 20);

        groundMonsterAnimations.put(GoundMonsterType.OWL,
                new Animation[] { new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]) });

        groundMonstersRegion.setRegion(240, 0, 240, 20);

        groundMonsterAnimations.put(GoundMonsterType.JELLY,
                new Animation[] { new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]) });

        TextureRegion[] lobsterTextureRegion = new TextureRegion(lobsterTexture).split(24, 20)[0];
        TextureRegion[] lobsterTextureRegionToFlip = new TextureRegion(lobsterTexture).split(24, 20)[0];
        for (TextureRegion textureRegion : lobsterTextureRegionToFlip) {
            textureRegion.flip(true, false);
        }
        Animation lobsterAnimRight = new Animation(0.2f, lobsterTextureRegion);
        Animation lobsterAnimLeft = new Animation(0.2f, lobsterTextureRegionToFlip);
        groundMonsterAnimations.put(GoundMonsterType.LOBSTER, new Animation[] { lobsterAnimLeft, lobsterAnimRight });

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

        TextureRegion treatBallRegion = new TextureRegion(gianaTexture, 0, 88, 175, 18);

        TextureRegion[] treatBall = treatBallRegion.split(22, 18)[0];
        treatBallRightAnim = new Animation(0.1f, treatBall);
        List<TextureRegion> asList = Arrays.asList(treatBall.clone());
        Collections.reverse(asList);
        treatBallLeftAnim = new Animation(0.1f, asList.toArray(new TextureRegion[treatBall.length]));

        lightningAnim = new Animation(0.1f,
                new TextureRegion(new Texture(Gdx.files.internal("data/lightning.png"))).split(9, 15)[0]);
        doubleLightningAnim = new Animation(0.1f, new TextureRegion(new Texture(
                Gdx.files.internal("data/double_lightning.png"))).split(19, 15)[0]);

        strawberry = new TextureRegion(new Texture(Gdx.files.internal("data/strawberry.png")));

        TextureRegion gianaBigRegion = new TextureRegion(gianaTexture);
        gianaBigRegion.setRegion(0, 125, 189, 28);
        TextureRegion[] gianaBRight = gianaBigRegion.split(27, 28)[0];
        gianaBigRegion.setRegion(0, 154, 189, 28);
        TextureRegion[] gianaBLeft = gianaBigRegion.split(27, 28)[0];

        gianaBigRight = new Animation(0.1f, gianaBRight[1], gianaBRight[2], gianaBRight[3], gianaBRight[4]);
        gianaBigLeft = new Animation(0.1f, gianaBLeft[1], gianaBLeft[2], gianaBLeft[3], gianaBLeft[4]);

        gianaBigJumpRight = new Animation(0.1f, gianaBRight[5]);
        gianaBigJumpLeft = new Animation(0.1f, gianaBLeft[5]);

        gianaBigIdleRight = new Animation(0.5f, gianaBRight[0]);
        gianaBigIdleLeft = new Animation(0.5f, gianaBLeft[0]);

        TextureRegion gianaRegion = new TextureRegion(gianaTexture);
        gianaRegion.setRegion(0, 0, 189, 28);
        TextureRegion[] gianaSmallRight = gianaRegion.split(27, 28)[0];
        gianaRegion.setRegion(0, 29, 189, 28);
        TextureRegion[] gianaSmallLeft = gianaRegion.split(27, 28)[0];

        gianaRegion.setRegion(0, 59, 27, 28);
        dying = gianaRegion.split(27, 28)[0][0];

        gianaRegion.setRegion(27, 59, 27 * 3, 28);
        gianaGrow = new Animation(0.1f, gianaRegion.split(27, 28)[0]);

        gianaDead = new Animation(0.2f, dying);

        gianaRight = new Animation(0.1f, gianaSmallRight[1], gianaSmallRight[2], gianaSmallRight[3], gianaSmallRight[4]);
        gianaLeft = new Animation(0.1f, gianaSmallLeft[1], gianaSmallLeft[2], gianaSmallLeft[3], gianaSmallLeft[4]);

        gianaJumpRight = new Animation(0.1f, gianaSmallRight[5]);
        gianaJumpLeft = new Animation(0.1f, gianaSmallLeft[5]);

        gianaIdleRight = new Animation(0.5f, gianaSmallRight[0]);
        gianaIdleLeft = new Animation(0.5f, gianaSmallLeft[0]);

        spawn = gianaSmallRight[0];

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        fontSize = (Gdx.graphics.getHeight() / 320) * 12;

        parameter.size = 10;
        font = generator.generateFont(parameter); // font size 12
        font.setColor(new Color(0.87f, 0.95f, 0.47f, 1));
        generator.dispose(); // don't forget to dispose to avoid memory leaks!
    }

    float stateTime = 0;
    Vector3 lerpTarget = new Vector3();

    public void render(float deltaTime) {

        float camX = map.giana.maxX;
        if (camX < 10) {
            camX = 10;
        }

        if (camX > 134) {
            camX = 134;
        }

        cam.position.lerp(lerpTarget.set(camX, map.tiles[0].length - SCENE_HEIGHT / 2 + 1, 0), 4f * deltaTime);
        cam.update();

        stateTime += deltaTime;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        batch.draw(endDoor, map.endDoor.bounds.x, map.endDoor.bounds.y, map.endDoor.bounds.width,
                map.endDoor.bounds.height);

        renderWaters();
        renderPiranhas();
        drawBlocks();

        renderSimpleImages();
        renderMovingSpikes();

        renderDiamonds();
        renderGroundMonsters();

        renderBees();
        renderTreats();

        renderTreatBoxeSmallDiamonds();
        renderTreatBoxes();
        renderQuickSand();

        renderGiana();

        batch.end();

        if (!map.demo) {
            renderUpperText();
        }

        // fps.log();
    }

    private void renderUpperText() {
        fontBatch.begin();
        //

        String formatted = String.format("%06d         %02d       %02d         %02d      %02d", map.score,
                map.diamondsCollected, map.lives, map.level, map.time);
        font.draw(fontBatch, "GIANA       BONUS     LIVES     STAGE    TIME", 10, 315);

        font.draw(fontBatch, formatted, 10, 305);

        fontBatch.end();
    }

    private void renderTreats() {
        for (Treat treat : map.treats) {
            if (treat.active) {

                switch (map.giana.power) {
                case NONE: {
                    if (treat.dir == Treat.RIGHT)
                        batch.draw(treatBallRightAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 1,
                                1);
                    else
                        batch.draw(treatBallLeftAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 1, 1);
                    break;
                }
                case BIG: {
                    batch.draw(lightningAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 0.5f, 0.8f);
                    break;
                }
                case SHOOT: {
                    batch.draw(doubleLightningAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 1,
                            0.8f);
                    break;
                }
                case STRAWBERRY: {
                    batch.draw(strawberry, treat.pos.x, treat.pos.y, 1, 0.8f);
                    break;
                }
                default:
                }

            }
        }
    }

    private void renderGiana() {
        Animation anim = null;

        if (GianaPower.isBig(map.giana.power)) {

            if (map.giana.bullet.active) {
                batch.draw(bullet, map.giana.bullet.pos.x, map.giana.bullet.pos.y, 0.3f, 0.3f);
            }

            if (map.giana.state == GianaState.RUN) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigLeft;
                else
                    anim = gianaBigRight;
            }
            if (map.giana.state == GianaState.IDLE) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigIdleLeft;
                else
                    anim = gianaBigIdleRight;
            }
            if (map.giana.state == GianaState.JUMP) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigJumpLeft;
                else
                    anim = gianaBigJumpRight;
            }
        } else {
            if (map.giana.state == GianaState.RUN) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaLeft;
                else
                    anim = gianaRight;
            }
            if (map.giana.state == GianaState.IDLE) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaIdleLeft;
                else
                    anim = gianaIdleRight;
            }
            if (map.giana.state == GianaState.JUMP) {
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaJumpLeft;
                else
                    anim = gianaJumpRight;
            }
        }

        if (map.giana.state == GianaState.GROW) {
            anim = gianaGrow;
        }

        if (map.giana.state == GianaState.SPAWN) {
            batch.draw(spawn, map.giana.pos.x, map.giana.pos.y, 1, 1);
            return;
        }
        if (map.giana.state == GianaState.DYING) {
            batch.draw(dying, map.giana.pos.x, map.giana.pos.y, 1, 1);
            return;
        }
        if (map.giana.active) {
            batch.draw(anim.getKeyFrame(map.giana.stateTime, true), map.giana.pos.x, map.giana.pos.y, 1, 1);
        }

        // batch.draw(tileTexture, map.giana.killerBounds.x,
        // map.giana.killerBounds.y, map.giana.killerBounds.width,
        // map.giana.killerBounds.height);

    }

    private void renderDiamonds() {
        for (Diamond currentDiamond : map.diamonds) {
            if (currentDiamond.active) {
                batch.draw(diamondAnim.getKeyFrame(currentDiamond.stateTime, true), currentDiamond.pos.x,
                        currentDiamond.pos.y, 0.8f, 0.8f);
            }
        }
    }

    private void renderTreatBoxeSmallDiamonds() {
        for (SmallDiamoind smallDiamoind : map.treatSmallDiamoinds) {
            if (smallDiamoind.active) {
                batch.draw(smallDiamondAnim.getKeyFrame(smallDiamoind.stateTime, true), smallDiamoind.pos.x,
                        smallDiamoind.pos.y, 0.4f, 0.4f);
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

    private void renderSimpleImages() {
        for (SimpleImage simpleImage : map.simpleImages) {
            TextureRegion region = simpleImageTextureRegions.get(simpleImage.type);
            if (region != null) {
                batch.draw(region, simpleImage.bounds.x, simpleImage.bounds.y, simpleImage.bounds.width,
                        simpleImage.bounds.height);
            }
        }
    }

    private void renderMovingSpikes() {
        for (int i = 0; i < map.movingSpikes.size; i++) {
            MovingSpikes spikes = map.movingSpikes.get(i);
            batch.draw(movingSpikesAnim.getKeyFrame(spikes.stateTime, true), spikes.pos.x, spikes.pos.y, 3, 1);
        }
    }

    private void renderWaters() {
        for (int i = 0; i < map.waters.size; i++) {
            Water water = map.waters.get(i);
            batch.draw(waterAnim.getKeyFrame(water.stateTime, true), water.pos.x, water.pos.y, 1, 1);
        }
    }

    private void renderGroundMonsters() {
        for (int i = 0; i < map.groundMonsters.size; i++) {
            GroundMonster monster = map.groundMonsters.get(i);
            Animation[] animations = groundMonsterAnimations.get(monster.type);
            if (animations.length == 0) {
                throw new IllegalStateException("ground monster type is not supported " + monster.type);
            } else {
                int index = 0;
                if (monster.type.needsMirror && monster.state == GroundMonster.BACKWARD) {
                    index = 1;
                }
                batch.draw(animations[index].getKeyFrame(monster.stateTime, true), monster.pos.x, monster.pos.y, 1,
                        monster.alive ? 1 : 0.2f);

            }
        }
    }

    private void renderPiranhas() {
        for (Fish fish : map.fishes) {
            if (fish.state == Fish.FORWARD)
                batch.draw(piranhaUpAnim.getKeyFrame(fish.stateTime, true), fish.pos.x, fish.pos.y, 1, 1);
            else
                batch.draw(piranhaDownAnim.getKeyFrame(fish.stateTime, true), fish.pos.x, fish.pos.y, 1, 1);

        }
    }

    private void renderQuickSand() {
        for (QuickSand sand : map.quickSandArray) {
            if (sand.active) {
                TextureRegion frame = quicksandAnim.getKeyFrame(sand.stateTime, false);

                batch.draw(frame, sand.pos.x + 0.0f, sand.pos.y, 0.33f, 1);
                batch.draw(frame, sand.pos.x + 0.33f, sand.pos.y, 0.33f, 1);
                batch.draw(frame, sand.pos.x + 0.66f, sand.pos.y, 0.33f, 1);
            }
        }
    }

    private void renderBees() {
        for (Bee bee : map.bees) {
            if (bee.alive) {
                if (bee.state == Bee.FORWARD)
                    batch.draw(waspRightAnim.getKeyFrame(bee.stateTime, true), bee.pos.x, bee.pos.y, 1, 1);
                else
                    batch.draw(waspLeftAnim.getKeyFrame(bee.stateTime, true), bee.pos.x, bee.pos.y, 1, 1);
            } else {
                if (bee.state == Bee.FORWARD)
                    batch.draw(waspRightDead, bee.pos.x, bee.pos.y, 1, 1);
                else
                    batch.draw(waspLeftDead, bee.pos.x, bee.pos.y, 1, 1);

            }

        }
    }

    private void drawBlocks() {

        for (Tile tile : map.tileArray) {
            if (tile.active) {
                switch (tile.state) {
                case NORMAL:
                    batch.draw(tileTexture, tile.pos.x, tile.pos.y, 1, 1);
                    break;
                case EXPLODING:
                    batch.draw(brickAnim.getKeyFrame(tile.stateTime, false), tile.pos.x, tile.pos.y, 1, 1);
                    break;
                case GONE:
                    break;
                }

            }
        }

    }

    public void dispose() {
        cache.dispose();
        batch.dispose();
        tileTexture.getTexture().dispose();
    }
}
