package com.mdinic.game.giana.service;

import java.util.List;

public interface HighScoreService {

    boolean haveScoreUpdate();

    List<Score> getScoreUpdate();

    void fetchHighScores();

    void saveHighScore(Score score);

    boolean internetAvailable();

    boolean goodForHighScores(int score);

    // offline
    // List<Score> getOfflineScores();
    //
    // void persistOfflineScores(List<Score> scores);
    //
    // void saveMyScore(Score score);

    Score getMyBest();
}
