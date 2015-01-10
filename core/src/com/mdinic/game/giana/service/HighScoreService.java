package com.mdinic.game.giana.service;

import java.util.List;

public interface HighScoreService {

    boolean haveScoreUpdate();

    List<Score> getScoreUpdate();

    void fetchHighScores(final boolean saveLocalScoreToWeb);

    void saveHighScore(Score score);

    boolean internetAvailable();

    boolean goodForHighScores(int score);

    Score getMyBest();
}
