package com.mdinic.game.giana.service;

import java.util.List;

public interface HighScoreService {

    boolean haveScoreUpdate();

    boolean haveTodaysScoreUpdate();

    List<Score> getScoreUpdate();

    List<Score> getTodaysScoreUpdate();

    void fetchHighScores();

    void fetchTodaysHighScores(final boolean saveLocalScoreToWeb);

    void saveHighScore(Score score);

    boolean internetAvailable();

    boolean goodForHighScores(int score);

    Score getMyBest();
}
