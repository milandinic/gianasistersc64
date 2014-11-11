package com.mdinic.game.giana;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class OnscreenControlRenderer {
    Map map;
    SpriteBatch batch;

    TextureRegion left;
    TextureRegion right;
    TextureRegion jump;

    public OnscreenControlRenderer(Map map) {
        this.map = map;
        loadAssets();
    }

    private void loadAssets() {
        Texture texture = new Texture(Gdx.files.internal("data/controls.png"));
        TextureRegion[] buttons = TextureRegion.split(texture, 64, 64)[0];
        left = buttons[0];
        right = buttons[1];
        jump = buttons[2];

        batch = new SpriteBatch();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 480, 320);
    }

    public void render() {
        if (Gdx.app.getType() != ApplicationType.Android && Gdx.app.getType() != ApplicationType.iOS)
            return;

        batch.begin();
        batch.draw(left, 0, 0);
        batch.draw(right, 70, 0);

        batch.draw(jump, 480 - 64, 0);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
