package com.mdinic.game.giana.service;

import java.util.List;

public interface HighScoreService {

    List<Score> getHighScores();

    void saveHighScore(Score score);
}
