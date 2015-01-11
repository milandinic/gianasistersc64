package com.mdinic.game.giana;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class Sounds {

    private boolean mute = false;

    private final Music startLevelSfx;
    private final Music endLevelSfx;
    private final Sound gianaJumpSfx;
    private final Sound gianaKillSfx;
    private final Sound gianaDyingSfx;
    private final Sound diamondSfx;
    private final Sound powerUpSfx;
    private final Sound brickDestroySfx;
    private final Sound treatBoxCoinSfx;
    private final Music introSfx;

    private static Sounds INSTANCE = new Sounds();

    public static Sounds getInstance() {
        return INSTANCE;
    }

    public enum Sfx {
        START_LEVEL, END_LEVEL, JUMP, KILL, DYING, POWERUP, BRICK_DESTROY, TREAT_BOX_COIN, DIAMOND, INTRO
    };

    public Sounds() {
        introSfx = Gdx.audio.newMusic(Gdx.files.internal("data/sfx/intro.mp3"));
        startLevelSfx = Gdx.audio.newMusic(Gdx.files.internal("data/sfx/startLevel.mp3"));
        endLevelSfx = Gdx.audio.newMusic(Gdx.files.internal("data/sfx/endLevel-bonus.mp3"));
        gianaJumpSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/jump.mp3"));
        gianaKillSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/kill.mp3"));
        gianaDyingSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/dying.mp3"));
        diamondSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/diamond-collect.mp3"));
        powerUpSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/powerup.mp3"));
        brickDestroySfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/brick.mp3"));
        treatBoxCoinSfx = Gdx.audio.newSound(Gdx.files.internal("data/sfx/treatboxcoin.mp3"));
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public void play(Sfx sfx) {
        if (mute) {
            return;
        }
        switch (sfx) {
        case INTRO:
            introSfx.play();
            break;
        case BRICK_DESTROY:
            brickDestroySfx.play();
            break;
        case DYING:
            gianaDyingSfx.play();
            break;
        case END_LEVEL:
            endLevelSfx.play();
            break;
        case JUMP:
            gianaJumpSfx.play();
            break;
        case KILL:
            gianaKillSfx.play();
            break;
        case POWERUP:
            powerUpSfx.play();
            break;
        case START_LEVEL:
            startLevelSfx.play();
            break;
        case TREAT_BOX_COIN:
            treatBoxCoinSfx.play();
            break;
        case DIAMOND:
            diamondSfx.play();
            break;
        default:
            break;
        }
    }

    public void stop(Sfx sfx) {
        if (mute) {
            return;
        }
        switch (sfx) {
        case INTRO:
            introSfx.stop();
            break;
        case BRICK_DESTROY:
            brickDestroySfx.stop();
            break;
        case DYING:
            gianaDyingSfx.stop();
            break;
        case END_LEVEL:
            endLevelSfx.stop();
            break;
        case JUMP:
            gianaJumpSfx.stop();
            break;
        case KILL:
            gianaKillSfx.stop();
            break;
        case POWERUP:
            powerUpSfx.stop();
            break;
        case START_LEVEL:
            startLevelSfx.stop();
            break;
        case TREAT_BOX_COIN:
            treatBoxCoinSfx.stop();
            break;
        case DIAMOND:
            diamondSfx.stop();
            break;
        default:
            break;
        }
    }

}
