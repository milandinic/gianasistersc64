package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.InternetConnectionChecker;
import com.mdinic.game.giana.service.Score;

/**
 * Local-only high score service. The Parse backend was removed (servers
 * shut down in 2017); online high scores are deferred to a later iteration.
 * Scores persist in SharedPreferences on-device only.
 */
public class HighScoreServiceDroid implements HighScoreService {

    private static final int LIMIT_SCORES = 5;
    private static final String SCORE = "score";
    private static final String LEVEL = "level";
    private static final String USERNAME = "username";

    private static final String NAMEMY = "usernamemy";
    private static final String LEVELMY = "levelmy";
    private static final String SCOREMY = "scoremy";
    private static final String TOTALSCORES = "totalscores";

    private List<Score> scores = new ArrayList<Score>();
    private List<Score> todaysScores = new ArrayList<Score>();

    private final Activity activity;
    final Object object = new Object();

    boolean haveUpdate = true;
    boolean haveTodaysUpdate = true;

    public HighScoreServiceDroid(Activity activity, InternetConnectionChecker checker) {
        super();
        this.activity = activity;
        fetchHighScores();
        fetchTodaysHighScores(true);
    }

    @Override
    public void fetchHighScores() {
        synchronized (object) {
            scores = getOfflineScores("");
            haveUpdate = true;
        }
    }

    @Override
    public void fetchTodaysHighScores(boolean saveLocalScoreToWeb) {
        synchronized (object) {
            todaysScores = getOfflineScores("todays");
            haveTodaysUpdate = true;
        }
    }

    @Override
    public void saveHighScore(Score score) {
        Score myBestScore = getMyBest();
        if (myBestScore == null || myBestScore.getScore() < score.getScore()) {
            saveMyOfflineHighScore(score);
        }
        fetchHighScores();
        fetchTodaysHighScores(false);
    }

    private void saveMyOfflineHighScore(Score score) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SCOREMY, score.getScore());
        editor.putInt(LEVELMY, score.getLevel());
        editor.putString(NAMEMY, score.getName());
        editor.commit();
    }

    @Override
    public Score getMyBest() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int score = sharedPref.getInt(SCOREMY, 0);
        if (score != 0) {
            return new Score(sharedPref.getString(NAMEMY, ""), score, sharedPref.getInt(LEVELMY, 0));
        }
        return null;
    }

    @Override
    public boolean goodForHighScores(int score) {
        if (todaysScores.isEmpty() || todaysScores.size() < LIMIT_SCORES) {
            return true;
        }
        for (Score topScore : todaysScores) {
            if (topScore.getScore() < score) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Score> getScoreUpdate() {
        List<Score> result = new ArrayList<Score>(scores.size() + 1);
        synchronized (object) {
            haveUpdate = false;
            result.addAll(scores);
        }
        return result;
    }

    @Override
    public List<Score> getTodaysScoreUpdate() {
        List<Score> result = new ArrayList<Score>(todaysScores.size() + 1);
        synchronized (object) {
            haveTodaysUpdate = false;
            result.addAll(todaysScores);
        }
        return result;
    }

    @Override
    public boolean haveScoreUpdate() {
        synchronized (object) {
            return haveUpdate;
        }
    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        synchronized (object) {
            return haveTodaysUpdate;
        }
    }

    @Override
    public boolean internetAvailable() {
        return false;
    }

    private List<Score> getOfflineScores(String prefix) {
        List<Score> localScores = new ArrayList<Score>();
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        int highScores = sharedPref.getInt(prefix + TOTALSCORES, 0);
        for (int i = 0; i < highScores; i++) {
            localScores.add(new Score(sharedPref.getString(prefix + USERNAME + i, ""),
                    sharedPref.getInt(prefix + SCORE + i, 0), sharedPref.getInt(prefix + LEVEL + i, 0)));
        }
        return localScores;
    }
}
