package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.Vector3;
import com.mdinic.game.giana.Giana.GianaState;

public class MapRenderer {
    Map map;
    OrthographicCamera cam;
    OrthographicCamera scoreCam;
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
    Animation spawn;
    TextureRegion dying;
    TextureRegion endDoor;
    FPSLogger fps = new FPSLogger();
    private Animation movingSpikesAnim;

    private Animation owlAnim;
    private Animation jellyAnim;
    private Animation lobsterAnim;
    private BitmapFont font;

    int fontSize;

    java.util.Map<SimpleImageType, TextureRegion> simpleImageTextureRegions = new HashMap<SimpleImageType, TextureRegion>();

    public MapRenderer(Map map) {
        this.map = map;
        this.cam = new OrthographicCamera(20, 16);
        scoreCam = new OrthographicCamera();
        this.scoreCam.setToOrtho(false);
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

        simpleImageTextureRegions.put(SimpleImageType.BIG_CLOUD, new TextureRegion(sprites, 13, 10, 42, 17));
        simpleImageTextureRegions.put(SimpleImageType.SMALL_CLOUD, new TextureRegion(sprites, 18, 46, 34, 17));
        simpleImageTextureRegions.put(SimpleImageType.MUSHROOM, new TextureRegion(sprites, 69, 44, 32, 24));
        simpleImageTextureRegions.put(SimpleImageType.ROUND_BUSH, new TextureRegion(sprites, 66, 7, 32, 22));
        simpleImageTextureRegions.put(SimpleImageType.WIDE_BUSH, new TextureRegion(sprites, 20, 83, 80, 20));
        simpleImageTextureRegions.put(SimpleImageType.COLUMN, new TextureRegion(sprites, 161, 259, 48, 25));

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

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        System.out.println(Gdx.graphics.getWidth());

        fontSize = Gdx.graphics.getWidth() / 640 * 12;

        parameter.size = fontSize;
        font = generator.generateFont(parameter); // font size 12
        font.setColor(new Color(0xe0ef99));
        generator.dispose(); // don't forget to dispose to avoid memory leaks!
    }

    float stateTime = 0;
    Vector3 lerpTarget = new Vector3();

    public void render(float deltaTime) {

        float camX = map.giana.maxX;
        if (camX < 10) {
            camX = 10;
        }

        if (camX > 135) {
            camX = 135;
        }

        cam.position.lerp(lerpTarget.set(camX, 153, 0), 4f * deltaTime);
        cam.update();

        scoreCam.update();

        cache.setProjectionMatrix(cam.combined);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        cache.begin();
        for (int blockY = 0; blockY < 4; blockY++) {
            for (int blockX = 0; blockX < 6; blockX++) {
                cache.draw(blocks[blockX][blockY]);
            }
        }
        cache.end();
        stateTime += deltaTime;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        if (map.endDoor != null)
            batch.draw(endDoor, map.endDoor.bounds.x, map.endDoor.bounds.y, 2, 2);

        renderSimpleImages();

        renderMovingSpikes();
        renderGroundMonsters();

        renderBob();
        renderDiamonds();
        renderTreatBoxes();
        batch.end();

        this.scoreCam.position.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
        scoreCam.update();
        batch.setProjectionMatrix(scoreCam.combined);

        renderUpperText();

        // fps.log();
    }

    private void renderUpperText() {
        batch.begin();
        //

        String formatted = String.format("%06d         %02d       %02d         %02d      %02d", 123,
                map.diamondsCollected, map.lives, map.level, map.time);
        font.draw(batch, "GIANA      BONUS     LIVES     STAGE    TIME", 20, Gdx.graphics.getHeight() - fontSize);

        font.draw(batch, formatted, 20, Gdx.graphics.getHeight() - fontSize * 2.1f);

        batch.end();
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
