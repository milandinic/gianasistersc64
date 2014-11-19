package com.mdinic.game.giana.service;


public interface HighScoreService {

    void getHighScores(HighScoreListener listener);

    void saveHighScore(Score score);
}
