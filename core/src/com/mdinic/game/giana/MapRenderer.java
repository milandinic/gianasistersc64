package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Vector3;
import com.mdinic.game.giana.Bullet.BulletState;
import com.mdinic.game.giana.LevelConf.BrickColor;

public class MapRenderer {

    public static final int SCENE_HEIGHT = 16;
    GameMap map;
    public OrthographicCamera cam;

    float stateTime = 0;
    Vector3 lerpTarget = new Vector3();

    public SpriteBatch batch = new SpriteBatch(5460);
    public final SpriteBatch fontBatch;

    Animation spiderAnim;

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
    java.util.Map<BrickColor, Animation> treatBoxAnim = new HashMap<BrickColor, Animation>();
    Animation whiteCristalAnim;

    java.util.Map<BrickColor, TextureRegion> tileTexture = new HashMap<BrickColor, TextureRegion>();

    java.util.Map<BrickColor, TextureRegion> usedTreatBox = new HashMap<BrickColor, TextureRegion>();
    TextureRegion spawn;
    TextureRegion dying;
    TextureRegion endDoor;
    TextureRegion bullet;
    TextureRegion bulletExplode;
    TextureRegion waspLeftDead;
    TextureRegion waspRightDead;
    TextureRegion strawberry;

    java.util.Map<GoundMonsterType, Animation[]> groundMonsterAnimations = new HashMap<GoundMonsterType, Animation[]>();

    public final BitmapFont yellowFont10;
    public final BitmapFont yellowFont12;
    public BitmapFont whiteFont10;
    public BitmapFont redFont10;

    java.util.Map<SimpleImageType, TextureRegion> simpleImageTextureRegions = new HashMap<SimpleImageType, TextureRegion>();
    java.util.Map<FixedTrapType, Animation> fixedTrapTypeAnim = new HashMap<FixedTrapType, Animation>();

    private final Animation treatBallRightAnim;
    private final Animation treatBallLeftAnim;
    private final Animation piranhaUpAnim;
    private final Animation piranhaDownAnim;

    private final Animation ballDownAnim;
    private final Animation ballUpAnim;

    private final Animation waspRightAnim;
    private final Animation waspLeftAnim;
    public final Animation yellowCristalAnim;
    java.util.Map<BrickColor, Animation> quicksandAnim = new HashMap<LevelConf.BrickColor, Animation>();

    java.util.Map<BrickColor, Animation> brickAnim = new HashMap<LevelConf.BrickColor, Animation>();

    private final Animation lightningAnim;
    private final Animation doubleLightningAnim;
    private float camY;

    public MapRenderer() {

        this.cam = new OrthographicCamera(20, SCENE_HEIGHT);

        fontBatch = new SpriteBatch();
        fontBatch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);

        yellowCristalAnim = new Animation(0.3f, new TextureRegion(new Texture(
                Gdx.files.internal("data/yellow-cristal.png"))).split(11, 11)[0]);

        Texture sprites = new Texture(Gdx.files.internal("data/sprites.png"));

        for (BrickColor color : BrickColor.values()) {
            Texture brick = new Texture(Gdx.files.internal("data/bricks-" + color.getName() + ".png"));

            TextureRegion[] brickRegion = new TextureRegion(brick).split(24, 16)[0];
            brickAnim.put(color, new Animation(0.1f, brickRegion[1], brickRegion[2], brickRegion[3], brickRegion[4]));

            tileTexture.put(color, brickRegion[0]);

            quicksandAnim.put(
                    color,
                    new Animation(0.1f, new TextureRegion(new Texture(Gdx.files.internal("data/quicksand-"
                            + color.getName() + ".png"))).split(20, 20)[0]));

            // treat box

            Texture treatboxTexture = new Texture(Gdx.files.internal("data/treatbox-" + color.getName() + ".png"));

            TextureRegion[] treatboxRegion = new TextureRegion(treatboxTexture).split(30, 20)[0];

            usedTreatBox.put(color, new TextureRegion(sprites, 213 + color.ordinal() * 30, 377, 30, 20));

            List<TextureRegion> tbArray = new ArrayList<TextureRegion>();
            for (int i = 0; i < treatboxRegion.length; i++) {
                tbArray.add(treatboxRegion[i]);
            }
            for (int i = treatboxRegion.length - 1; i >= 0; i--) {
                tbArray.add(treatboxRegion[i]);
            }
            treatBoxAnim.put(color, new Animation(0.2f, tbArray.toArray(new TextureRegion[10])));
        }

        bullet = new TextureRegion(new Texture(Gdx.files.internal("data/bullet.png")));
        bulletExplode = new TextureRegion(new Texture(Gdx.files.internal("data/bulletexplode.png")));

        this.endDoor = new TextureRegion(sprites, 16, 196, 32, 32);

        simpleImageTextureRegions.put(SimpleImageType.BIG_CLOUD, new TextureRegion(sprites, 13, 10, 42, 17));
        simpleImageTextureRegions.put(SimpleImageType.SMALL_CLOUD, new TextureRegion(sprites, 18, 46, 34, 17));
        simpleImageTextureRegions.put(SimpleImageType.MUSHROOM, new TextureRegion(sprites, 69, 44, 32, 24));
        simpleImageTextureRegions.put(SimpleImageType.ROUND_BUSH, new TextureRegion(sprites, 66, 7, 32, 22));
        simpleImageTextureRegions.put(SimpleImageType.WIDE_BUSH, new TextureRegion(sprites, 20, 83, 80, 20));
        simpleImageTextureRegions.put(SimpleImageType.COLUMN, new TextureRegion(sprites, 161, 259, 48, 25));
        simpleImageTextureRegions.put(SimpleImageType.FLOATING_COLUMN_UP, new TextureRegion(sprites, 164, 160, 48, 16));
        simpleImageTextureRegions.put(SimpleImageType.BLUE_WIRE, new TextureRegion(sprites, 384, 221, 16, 16));
        simpleImageTextureRegions.put(SimpleImageType.SMALL_COLUMN, new TextureRegion(sprites, 201, 44, 25, 25));
        simpleImageTextureRegions.put(SimpleImageType.STATIC_ALIEN, new TextureRegion(sprites, 381, 177, 24, 32));
        simpleImageTextureRegions.put(SimpleImageType.SPIRAL_WAGON, new TextureRegion(sprites, 15, 397, 72, 7));
        simpleImageTextureRegions.put(SimpleImageType.SPIRAL, new TextureRegion(sprites, 40, 374, 24, 23));
        simpleImageTextureRegions.put(SimpleImageType.MAGICWATER, new TextureRegion(sprites, 227, 270, 28, 3));

        smallDiamondAnim = new Animation(0.1f, new TextureRegion(new Texture(
                Gdx.files.internal("data/smalldiamond.png"))).split(8, 8)[0]);

        whiteCristalAnim = new Animation(0.3f, new TextureRegion(new Texture(
                Gdx.files.internal("data/white-cristal.png"))).split(11, 11)[0]);

        Texture gianaTexture = new Texture(Gdx.files.internal("data/giana.png"));
        Texture diamondTexture = new Texture(Gdx.files.internal("data/diamond.png"));

        Texture movingSpikesTexture = new Texture(Gdx.files.internal("data/movingspikes.png"));
        Texture fireTexture = new Texture(Gdx.files.internal("data/fire.png"));
        Texture waterTexture = new Texture(Gdx.files.internal("data/water.png"));

        Animation waterAnim = new Animation(0.1f, new TextureRegion(waterTexture).split(24, 12)[0]);
        Animation fireAnim = new Animation(0.1f, new TextureRegion(fireTexture).split(27, 16)[0]);
        Animation movingSpikesAnim = new Animation(0.3f, new TextureRegion(movingSpikesTexture).split(48, 16)[0]);
        Animation triangleAnim = new Animation(1f, new TextureRegion(sprites, 381, 140, 24, 24));

        fixedTrapTypeAnim.put(FixedTrapType.FIRE, fireAnim);
        fixedTrapTypeAnim.put(FixedTrapType.MOVING_SPIKES, movingSpikesAnim);
        fixedTrapTypeAnim.put(FixedTrapType.WATER, waterAnim);
        fixedTrapTypeAnim.put(FixedTrapType.TRIANGLE, triangleAnim);

        loadMonster("purple-alien", 20, 20, GoundMonsterType.PURPLE_ALIEN, 0.3f, false);
        loadMonster("yellow-alien", 20, 20, GoundMonsterType.YELLOW_ALIEN, 0.3f, false);
        loadMonster("bug", 22, 9, GoundMonsterType.BUG, 0.3f, false);
        loadMonster("eye", 24, 17, GoundMonsterType.EYE, 0.3f, true);
        loadMonster("dagger", 24, 16, GoundMonsterType.DAGGER, 0.3f, true);
        loadMonster("lobster", 24, 20, GoundMonsterType.LOBSTER, 0.2f, true);
        loadMonster("worm", 25, 21, GoundMonsterType.WORM, 0.1f, true);

        Texture groundMonstersTexture = new Texture(Gdx.files.internal("data/groundmonsters.png"));

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

        TextureRegion[] ballRegions = new TextureRegion(new Texture(Gdx.files.internal("data/ball.png"))).split(24, 20)[0];

        ballUpAnim = new Animation(0.2f, ballRegions[0], ballRegions[1], ballRegions[2]);
        ballDownAnim = new Animation(1f, ballRegions[3]);

        piranhaUpAnim = new Animation(0.2f,
                new TextureRegion(new Texture(Gdx.files.internal("data/piranha.png"))).split(20, 20)[0]);

        TextureRegion[] piranhaDownRegion = new TextureRegion(new Texture(Gdx.files.internal("data/piranha.png")))
                .split(20, 20)[0];
        for (TextureRegion textureRegion : piranhaDownRegion) {
            textureRegion.flip(false, true);
        }
        piranhaDownAnim = new Animation(0.2f, piranhaDownRegion);

        TextureRegion groundMonstersRegion = new TextureRegion(groundMonstersTexture);
        groundMonstersRegion.setRegion(0, 0, 240, 20);

        groundMonsterAnimations.put(GoundMonsterType.OWL,
                new Animation[] { new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]) });

        groundMonstersRegion.setRegion(240, 0, 240, 20);

        groundMonsterAnimations.put(GoundMonsterType.JELLY,
                new Animation[] { new Animation(0.2f, groundMonstersRegion.split(24, 20)[0]) });

        diamondAnim = new Animation(0.3f, new TextureRegion(diamondTexture).split(16, 16)[0]);

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

        spiderAnim = new Animation(0.1f, new TextureRegion(new Texture(Gdx.files.internal("data/spider.png"))).split(
                46, 20)[0]);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        parameter.size = 10;
        yellowFont10 = generator.generateFont(parameter); // font size 10
        yellowFont10.setColor(new Color(0.87f, 0.95f, 0.47f, 1));

        whiteFont10 = generator.generateFont(parameter);
        whiteFont10.setColor(new Color(1, 1, 1, 1));

        redFont10 = generator.generateFont(parameter);
        redFont10.setColor(new Color(0.66f, 0.21f, 0.14f, 1));

        parameter.size = 12;
        yellowFont12 = generator.generateFont(parameter); // font size 12
        yellowFont12.setColor(new Color(0.87f, 0.95f, 0.47f, 1));

        generator.dispose(); // don't forget to dispose to avoid memory leaks!
    }

    public void setMap(GameMap map, boolean moveCamToStart) {
        this.map = map;
        camY = map.tiles[0].length - SCENE_HEIGHT / 2 + 1;
        int x = (int) map.giana.pos.x;

        if (moveCamToStart) {
            x = 10;
        }

        if (x < 10) {
            x = 10;
        }
        lerpTarget.set(x, camY, 0);
        this.cam.position.set(x, camY, 0);
        cam.update();
    }

    public void render(float deltaTime) {

        float camX = map.giana.maxX;
        if (camX < 10) {
            camX = 10;
        }

        if (camX > 134) {
            camX = 134;
        }

        cam.position.lerp(lerpTarget.set(camX, camY, 0), 4f * deltaTime);
        if (!map.bonus)
            cam.update();

        stateTime += deltaTime;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        if (map.endDoor != null)
            batch.draw(endDoor, map.endDoor.bounds.x, map.endDoor.bounds.y, map.endDoor.bounds.width,
                    map.endDoor.bounds.height);

        renderFixedTraps();
        renderPiranhas();
        renderBalls();
        drawBlocks();

        renderSimpleImages();

        renderDiamonds();
        renderGroundMonsters();

        renderBees();
        renderTreats();

        renderTreatBoxeSmallDiamonds();
        renderTreatBoxes();
        renderQuickSand();

        renderGiana();

        renderBoss();

        batch.end();

        if (!map.demo) {
            renderUpperText();
        }
    }

    private void renderBoss() {
        if (map.boss != null) {
            batch.draw(spiderAnim.getKeyFrame(map.boss.stateTime, true), map.boss.pos.x, map.boss.pos.y,
                    map.boss.bounds.width, map.boss.bounds.height);
        }
    }

    private void renderUpperText() {
        fontBatch.begin();
        //

        String formatted = String.format("%06d         %02d       %02d         %02d      %02d", map.score,
                map.diamondsCollected, map.lives, map.level, map.time);
        yellowFont10.draw(fontBatch, "GIANA       BONUS     LIVES     STAGE    TIME", 10, 315);

        yellowFont10.draw(fontBatch, formatted, 10, 305);

        fontBatch.draw(whiteCristalAnim.getKeyFrame(stateTime, true), 130, 295);

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
                case BREAK_BRICK: {
                    batch.draw(lightningAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 0.5f, 0.8f);
                    break;
                }
                case SHOOT: {
                    batch.draw(doubleLightningAnim.getKeyFrame(treat.stateTime, true), treat.pos.x, treat.pos.y, 1,
                            0.8f);
                    break;
                }
                case HOMING: {
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
                if (map.giana.bullet.state == BulletState.EXPLODE) {
                    batch.draw(bulletExplode, map.giana.bullet.pos.x, map.giana.bullet.pos.y, 0.6f, 0.6f);
                } else {
                    batch.draw(bullet, map.giana.bullet.pos.x, map.giana.bullet.pos.y, 0.3f, 0.3f);
                }
            }

            switch (map.giana.state) {
            case RUN:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigLeft;
                else
                    anim = gianaBigRight;
                break;
            case IDLE:
            case RIDING:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigIdleLeft;
                else
                    anim = gianaBigIdleRight;
                break;
            case JUMP:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaBigJumpLeft;
                else
                    anim = gianaBigJumpRight;
                break;
            default:
                break;
            }
        } else {
            switch (map.giana.state) {
            case RUN:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaLeft;
                else
                    anim = gianaRight;
                break;
            case RIDING:
            case IDLE:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaIdleLeft;
                else
                    anim = gianaIdleRight;
                break;
            case JUMP:
                if (map.giana.dir == Giana.LEFT)
                    anim = gianaJumpLeft;
                else
                    anim = gianaJumpRight;
                break;
            default:
                break;
            }

        }

        if (map.giana.state == GianaState.GROW) {
            anim = gianaGrow;
        }

        if (map.giana.state == GianaState.DYING) {
            batch.draw(dying, map.giana.pos.x, map.giana.pos.y, 1, 1);
            return;
        }

        if (map.giana.active) {
            batch.draw(anim.getKeyFrame(map.giana.stateTime, true), map.giana.pos.x, map.giana.pos.y, 1, 1);
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

    private void renderTreatBoxeSmallDiamonds() {
        for (SmallDiamoind smallDiamoind : map.treatSmallDiamoinds) {
            if (smallDiamoind.active) {
                batch.draw(smallDiamondAnim.getKeyFrame(smallDiamoind.stateTime, true), smallDiamoind.pos.x,
                        smallDiamoind.pos.y, 0.4f, 0.4f);
            }
        }
    }

    private void renderTreatBoxes() {

        BrickColor color;
        if (map.bonus) {
            color = BonusLevelConf.BONUS.getBrickColor();
        } else {
            color = LevelConf.values()[map.level].getBrickColor();
        }

        for (TreatBox box : map.treatBoxes) {
            if (box.active) {
                batch.draw(treatBoxAnim.get(color).getKeyFrame(box.stateTime, true), box.pos.x, box.pos.y, 1, 1);
            } else {
                batch.draw(usedTreatBox.get(color), box.pos.x, box.pos.y, 1, 1);
            }
        }
    }

    private void renderSimpleImages() {
        SimpleImageType[] values = SimpleImageType.values();
        for (SimpleImageType simpleImageType : values) {
            TextureRegion region = simpleImageTextureRegions.get(simpleImageType);
            if (region != null) {
                if (map.simpleImages.containsKey(simpleImageType)) {
                    List<SimpleImage> list = map.simpleImages.get(simpleImageType);
                    for (SimpleImage simpleImage : list) {

                        if (simpleImageType == SimpleImageType.SPIRAL_WAGON) {
                            batch.draw(region, simpleImage.bounds.x, simpleImage.bounds.y, simpleImage.bounds.width,
                                    -simpleImage.bounds.height);
                        } else {
                            batch.draw(region, simpleImage.bounds.x, simpleImage.bounds.y, simpleImage.bounds.width,
                                    simpleImage.bounds.height);
                        }

                    }
                }
            }
        }

    }

    private void renderFixedTraps() {
        for (int i = 0; i < map.fixedTraps.size; i++) {
            FixedTrap trap = map.fixedTraps.get(i);
            Animation anim = fixedTrapTypeAnim.get(trap.type);

            batch.draw(anim.getKeyFrame(trap.stateTime, true), trap.pos.x, trap.pos.y, trap.bounds.width,
                    trap.bounds.height);
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
                batch.draw(animations[index].getKeyFrame(monster.stateTime, true), monster.pos.x, monster.pos.y,
                        monster.bounds.width, monster.alive ? monster.bounds.height : 0.2f);

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

    private void renderBalls() {
        for (Ball ball : map.balls) {
            if (ball.state == Ball.FORWARD)
                batch.draw(ballUpAnim.getKeyFrame(ball.stateTime, true), ball.pos.x, ball.pos.y, 1, 1);
            else
                batch.draw(ballDownAnim.getKeyFrame(ball.stateTime, true), ball.pos.x, ball.pos.y, 1, 1);

        }
    }

    private void renderQuickSand() {
        BrickColor color;
        if (map.bonus) {
            color = BonusLevelConf.BONUS.getBrickColor();
        } else {
            color = LevelConf.values()[map.level].getBrickColor();
        }

        for (QuickSand sand : map.quickSandArray) {
            if (sand.active) {
                TextureRegion frame = quicksandAnim.get(color).getKeyFrame(sand.stateTime, false);

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
        BrickColor color;
        if (map.bonus) {
            color = BonusLevelConf.BONUS.getBrickColor();
        } else {
            color = LevelConf.values()[map.level].getBrickColor();
        }

        for (Tile tile : map.tileArray) {
            if (tile.active) {
                switch (tile.state) {
                case NORMAL:
                    batch.draw(tileTexture.get(color), tile.pos.x, tile.pos.y, 1, 1);
                    break;
                case EXPLODING:
                    batch.draw(brickAnim.get(color).getKeyFrame(tile.stateTime, false), tile.pos.x, tile.pos.y, 1, 1);
                    break;
                case GONE:
                    break;
                }

            }
        }
    }

    void loadMonster(String textureName, int width, int height, GoundMonsterType type, float speed, boolean swap) {

        TextureRegion[] rightRegion = new TextureRegion(new Texture(Gdx.files.internal("data/" + textureName + ".png")))
                .split(width, height)[0];

        TextureRegion[] leftRegion = new TextureRegion(new Texture(Gdx.files.internal("data/" + textureName + ".png")))
                .split(width, height)[0];

        for (TextureRegion textureRegion : leftRegion) {
            textureRegion.flip(true, false);
        }

        Animation animLeft = new Animation(speed, leftRegion);
        Animation animRight = new Animation(speed, rightRegion);

        if (swap)
            groundMonsterAnimations.put(type, new Animation[] { animLeft, animRight });
        else

            groundMonsterAnimations.put(type, new Animation[] { animRight, animLeft });
    }

    public void dispose() {
        yellowFont10.dispose();
        yellowFont12.dispose();
        whiteFont10.dispose();
        redFont10.dispose();

        spawn.getTexture().dispose();
        dying.getTexture().dispose();
        endDoor.getTexture().dispose();
        bullet.getTexture().dispose();
        bulletExplode.getTexture().dispose();
        waspLeftDead.getTexture().dispose();
        waspRightDead.getTexture().dispose();
        strawberry.getTexture().dispose();

        for (Entry<BrickColor, TextureRegion> entry : tileTexture.entrySet()) {
            entry.getValue().getTexture().dispose();
        }

        for (Entry<BrickColor, TextureRegion> entry : usedTreatBox.entrySet()) {
            entry.getValue().getTexture().dispose();
        }

        for (Entry<FixedTrapType, Animation> entry : fixedTrapTypeAnim.entrySet()) {
            disposeAnimation(entry.getValue());
        }

        for (Entry<SimpleImageType, TextureRegion> entry : simpleImageTextureRegions.entrySet()) {
            entry.getValue().getTexture().dispose();
        }

        for (Entry<GoundMonsterType, Animation[]> entry : groundMonsterAnimations.entrySet()) {
            for (Animation animation : entry.getValue()) {
                disposeAnimation(animation);
            }
        }

        for (Entry<BrickColor, Animation> entry : treatBoxAnim.entrySet()) {
            disposeAnimation(entry.getValue());
        }

        for (Entry<BrickColor, Animation> entry : quicksandAnim.entrySet()) {
            disposeAnimation(entry.getValue());
        }

        for (Entry<BrickColor, Animation> entry : brickAnim.entrySet()) {
            disposeAnimation(entry.getValue());
        }

        disposeAnimation(gianaLeft);
        disposeAnimation(gianaRight);
        disposeAnimation(gianaJumpLeft);
        disposeAnimation(gianaJumpRight);
        disposeAnimation(gianaIdleLeft);
        disposeAnimation(gianaIdleRight);
        disposeAnimation(gianaBigLeft);
        disposeAnimation(gianaBigRight);
        disposeAnimation(gianaBigJumpLeft);
        disposeAnimation(gianaBigJumpRight);
        disposeAnimation(gianaBigIdleLeft);
        disposeAnimation(gianaBigIdleRight);
        disposeAnimation(gianaDead);
        disposeAnimation(gianaGrow);
        disposeAnimation(smallDiamondAnim);
        disposeAnimation(diamondAnim);

        disposeAnimation(treatBallRightAnim);
        disposeAnimation(treatBallLeftAnim);
        disposeAnimation(piranhaUpAnim);
        disposeAnimation(piranhaDownAnim);
        disposeAnimation(waspRightAnim);
        disposeAnimation(waspLeftAnim);
        disposeAnimation(lightningAnim);
        disposeAnimation(doubleLightningAnim);
        disposeAnimation(whiteCristalAnim);
        disposeAnimation(yellowCristalAnim);

        disposeAnimation(ballUpAnim);
        disposeAnimation(ballDownAnim);
        disposeAnimation(spiderAnim);

        batch.dispose();
    }

    void disposeAnimation(Animation anim) {
        TextureRegion[] keyFrames = anim.getKeyFrames();
        for (TextureRegion textureRegion : keyFrames) {
            textureRegion.getTexture().dispose();
        }
    }
}
