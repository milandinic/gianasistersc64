package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.mdinic.game.giana.GameMap;
import com.mdinic.game.giana.Giana;
import com.mdinic.game.giana.GianaState;
import com.mdinic.game.giana.MapRenderer;
import com.mdinic.game.giana.OnscreenControlRenderer;

public class BonusGameScreen extends GianaSistersScreen {
    GameMap map;
    GameMap oldMap;
    OnscreenControlRenderer controlRenderer;

    public BonusGameScreen(Game game, GameMap oldMap, MapRenderer renderer) {
        super(game, renderer);
        this.oldMap = oldMap;
        this.map = new GameMap(oldMap.level % 5, oldMap.sounds, true);
        this.map.level = oldMap.level;
        this.map.lives = oldMap.lives;
        this.map.score = oldMap.score;
        this.map.giana.stateTime = oldMap.giana.stateTime;
        this.map.time = oldMap.time;
        this.map.diamondsCollected = oldMap.diamondsCollected;
    }

    @Override
    public void show() {
        renderer.setMap(map);
        controlRenderer = new OnscreenControlRenderer(map, this);

        // map.sounds.play();
    }

    @Override
    public void render(float delta) {
        delta = Math.min(0.06f, Gdx.graphics.getDeltaTime());

        if (delta == 0) {
            return;
        }
        map.update(delta);

        if (map.giana.state == GianaState.DEAD) {
            map.level--;
            if (map.lives == 0) {
                if (getGame().getHighScoreService().goodForHighScores(map.score))
                    game.setScreen(new EnterYourNameScreen(game, map, renderer));
                else
                    game.setScreen(new HighScoreScreen(game, map.sounds, renderer));
            } else {
                game.setScreen(new LevelStartingScreen(game, map, renderer));
            }

            return;
        }

        if (map.giana.state != GianaState.DYING) {
            map.time = 99 - (int) map.giana.stateTime;
        }
        Gdx.gl.glClearColor(map.r, map.g, map.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(delta);

        controlRenderer.render();

        if (map.bonusLevelEndDoor != null) {
            if (map.giana.state != GianaState.RIDING && map.giana.bounds.overlaps(map.bonusLevelEndDoor.bounds)) {
                map.giana.state = GianaState.RIDING;
                map.giana.bounds.y = map.bonusLevelEndDoor.bounds.y;
            }
        }

        if (map.giana.state == GianaState.RIDING) {
            if (map.giana.pos.y >= 158) {

                oldMap.giana.pos.y += 2;
                oldMap.giana.bounds.y += 2;
                oldMap.giana.state = GianaState.JUMP;
                oldMap.giana.vel.y = Giana.JUMP_VELOCITY;
                oldMap.giana.grounded = false;
                oldMap.giana.stateTime = map.giana.stateTime;
                oldMap.lives = map.lives;
                oldMap.score = map.score;
                oldMap.time = map.time;
                oldMap.diamondsCollected = map.diamondsCollected;
                oldMap.turnBonusDoorIntoSand();
                game.setScreen(new GameScreen(game, oldMap, renderer, true));
                return;
            }
            map.bonusLevelEndDoor.bounds.y += 0.1f;
            map.giana.bounds.y += 0.1f;
            map.giana.pos.y = map.giana.bounds.y;

        }

    }

    @Override
    public void resume() {
        super.resume();
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.play();
        }
    }

    @Override
    public void pause() {
        super.pause();
        Music music = map.sounds.getCurrentMusic();
        if (music != null) {
            music.pause();
        }
    }

    @Override
    public void hide() {
        Gdx.app.debug("GianaSisters", "dispose game screen");
        // map.sounds.stop(LevelConf.values()[map.level].getMusic());
        controlRenderer.dispose();
    }

}
