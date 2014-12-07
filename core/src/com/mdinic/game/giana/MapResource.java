package com.mdinic.game.giana;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class MapResource {

    private final Music startLevelSfx;

    private final Sound endLevelSfx;

    private final Sound gianaJumpSfx;

    private final Sound gianaKillSfx;

    private final Sound gianaDyingSfx;

    private final Sound diamondSfx;

    private final Sound powerUpSfx;

    private final Sound brickDestroySfx;

    private final Sound treatBoxCoinSfx;

    private static MapResource INSTANCE = new MapResource();

    public static MapResource getInstance() {
        return INSTANCE;
    }

    public MapResource() {
        startLevelSfx = Gdx.audio.newMusic(Gdx.files.internal("data/sfx/startLevel.mp3"));
        endLevelSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/endLevel-bonus.mp3"));
        gianaJumpSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/jump.mp3"));
        gianaKillSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/kill.wav"));
        gianaDyingSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/dying.wav"));
        diamondSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/diamond-collect.mp3"));
        powerUpSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/powerup.wav"));
        brickDestroySfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/brick.wav"));
        treatBoxCoinSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/treatboxcoin.wav"));

    }

    public Music getStartLevelSfx() {
        return startLevelSfx;
    }

    public Sound getEndLevelSfx() {
        return endLevelSfx;
    }

    public Sound getGianaJumpSfx() {
        return gianaJumpSfx;
    }

    public Sound getGianaKillSfx() {
        return gianaKillSfx;
    }

    public Sound getGianaDyingSfx() {
        return gianaDyingSfx;
    }

    public Sound getDiamondSfx() {
        return diamondSfx;
    }

    public Sound getPowerUpSfx() {
        return powerUpSfx;
    }

    public Sound getBrickDestroySfx() {
        return brickDestroySfx;
    }

    public Sound getTreatBoxCoinSfx() {
        return treatBoxCoinSfx;
    }

}
