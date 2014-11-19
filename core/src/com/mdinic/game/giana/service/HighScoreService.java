package com.mdinic.game.giana.service;

import java.util.List;

public interface HighScoreService {

    boolean haveScoreUpdate();

    List<Score> getScoreUpdate();

    void fetchHighScores();

    void saveHighScore(Score score);
}
