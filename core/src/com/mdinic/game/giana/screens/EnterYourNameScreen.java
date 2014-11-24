package com.mdinic.game.giana.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.mdinic.game.giana.Map;
import com.mdinic.game.giana.service.Score;

public class EnterYourNameScreen extends GianaSistersScreen {

    private static final String TYPE_YOUR_NAME = "Click to type name";
    private int fontSize;
    private BitmapFont yellowFont;

    private final Map oldMap;

    private Stage stage;
    private Color fontColor;

    public EnterYourNameScreen(Game game, Map oldMap) {
        super(game);
        this.oldMap = oldMap;
    }

    @Override
    public void show() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/Giana.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();

        fontSize = Gdx.graphics.getWidth() / SCREEN_WIDTH * 12; // font size 12

        parameter.size = fontSize;
        yellowFont = generator.generateFont(parameter);

        fontColor = new Color(0.87f, 0.95f, 0.47f, 1);
        yellowFont.setColor(fontColor);

        stage = new Stage();

        TextFieldStyle style = new TextFieldStyle();
        style.font = yellowFont;
        style.fontColor = fontColor;
        final TextField nameText = new TextField(TYPE_YOUR_NAME, style);
        nameText.setMaxLength(22);
        nameText.selectAll();

        nameText.setOnscreenKeyboard(new TextField.OnscreenKeyboard() {
            @Override
            public void show(boolean visible) {
                Gdx.input.getTextInput(new Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        if (!text.isEmpty()) {
                            Score score = new Score(text, oldMap.score, oldMap.level + 1);
                            getGame().getHighScoreService().saveHighScore(score);
                        }
                        game.setScreen(new HighScoreScreen(game));
                    }

                    @Override
                    public void canceled() {
                        game.setScreen(new IntroScreen(game));
                    }
                }, "", "");
            }
        });

        Table table = new Table();
        stage.addActor(table);

        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = yellowFont;
        labelStyle.fontColor = fontColor;
        table.add(new Label(String.format("YOUR SCORE %07d", oldMap.score), labelStyle))
                .width(Gdx.graphics.getWidth() / 2).height(fontSize * 2);
        table.row();

        table.add(nameText).width(Gdx.graphics.getWidth() / 2).height(fontSize * 2);
        stage.setKeyboardFocus(nameText);
        table.setPosition(Gdx.graphics.getWidth() / 3, Gdx.graphics.getHeight() / 2);

        Gdx.input.setInputProcessor(stage);
        generator.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    @Override
    public void hide() {
        stage.dispose();
        yellowFont.dispose();
    }

}
