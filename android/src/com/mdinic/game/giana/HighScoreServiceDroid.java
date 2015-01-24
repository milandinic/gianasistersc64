package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.InternetConnectionChecker;
import com.mdinic.game.giana.service.Score;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class HighScoreServiceDroid implements HighScoreService {

    private static final String GAME_SCORE = "GameScore";
    private static final int LIMIT_SCORES = 5;
    private static final String DATE = "date";
    private static final String SCORE = "score";
    private static final String LEVEL = "level";
    private static final String USERNAME = "username";

    private static final String NAMEMY = "usernamemy";
    private static final String LEVELMY = "levelmy";
    private static final String SCOREMY = "scoremy";
    private static final String UNSAVED_SCORE = "unsavedscore";

    private static final String TOTALSCORES = "totalscores";

    private List<Score> scores = new ArrayList<Score>();
    private List<Score> todaysScores = new ArrayList<Score>();

    private final Activity activity;

    final Object object = new Object();

    boolean haveUpdate = true;
    boolean haveTodaysUpdate = true;
    boolean internetAvailable = false;

    public HighScoreServiceDroid(Activity activity, InternetConnectionChecker checker) {
        super();
        this.activity = activity;

        internetAvailable = checker != null && checker.isAvailableConnection();

        fetchHighScores();
        fetchTodaysHighScores(true);
    }

    @Override
    public void fetchHighScores() {
        if (internetAvailable()) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(GAME_SCORE);
            query.setLimit(LIMIT_SCORES);
            query.addDescendingOrder(SCORE);

            synchronized (object) {
                scores.clear();
            }

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> scoreList, ParseException e) {
                    if (e == null) {
                        List<Score> newScores = new ArrayList<Score>();
                        for (ParseObject parseObject : scoreList) {
                            newScores.add(new Score(parseObject.getString(USERNAME), parseObject.getInt(SCORE),
                                    parseObject.getInt(LEVEL)));
                        }
                        persistScores(newScores, "");
                        synchronized (object) {
                            scores = newScores;
                            haveUpdate = true;
                        }
                    }
                }

            });
        } else {
            synchronized (object) {
                scores = getOfflineScores("");
                haveUpdate = true;
            }
        }

    }

    @Override
    public void saveHighScore(Score score) {

        Score myBestScore = getMyBest();

        if (internetAvailable()) {
            persistObject(score);
            setUnsavedHighscoreFlag(false);
        }

        if (myBestScore == null) {
            saveMyOfflineHighScore(score);

        } else if (myBestScore.getScore() < score.getScore()) {
            saveMyOfflineHighScore(score);
        }

        fetchHighScores();
        fetchTodaysHighScores(false);
    }

    private void persistObject(Score myBestScore) {
        ParseObject object = new ParseObject(GAME_SCORE);
        object.put(USERNAME, myBestScore.getName());
        object.put(SCORE, myBestScore.getScore());
        object.put(LEVEL, myBestScore.getLevel());
        object.put(DATE, myBestScore.getDate().getTime());
        object.saveInBackground();
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

    public void saveMyOfflineHighScore(Score score) {

        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(SCOREMY, score.getScore());
        editor.putInt(LEVELMY, score.getLevel());
        editor.putString(NAMEMY, score.getName());

        editor.commit();

        if (!internetAvailable()) {
            setUnsavedHighscoreFlag(true);
        }
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
    public List<Score> getTodaysScoreUpdate() {
        List<Score> result = new ArrayList<Score>(todaysScores.size() + 1);
        synchronized (object) {
            haveTodaysUpdate = false;
            result.addAll(todaysScores);
        }
        return result;
    }

    @Override
    public void fetchTodaysHighScores(final boolean saveLocalScoreToWeb) {
        if (internetAvailable()) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(GAME_SCORE);
            query.setLimit(LIMIT_SCORES);
            query.addDescendingOrder(SCORE);
            Date from = new Date();
            from.setSeconds(0);
            from.setMinutes(0);
            from.setHours(0);

            query.whereGreaterThan("createdAt", from);

            Date to = new Date();
            to.setSeconds(59);
            to.setMinutes(59);
            to.setHours(23);

            query.whereLessThan("createdAt", to);

            synchronized (object) {
                todaysScores.clear();
            }

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> scoreList, ParseException e) {
                    if (e == null) {
                        List<Score> newScores = new ArrayList<Score>();
                        for (ParseObject parseObject : scoreList) {
                            newScores.add(new Score(parseObject.getString(USERNAME), parseObject.getInt(SCORE),
                                    parseObject.getInt(LEVEL)));
                        }
                        persistScores(newScores, "todays");
                        synchronized (object) {
                            todaysScores = newScores;
                            haveTodaysUpdate = true;
                        }
                        if (saveLocalScoreToWeb) {

                            Score myBestScore = getMyBest();

                            if (myBestScore != null && getUnsavedHighscoreFlag()) {
                                if (goodForHighScores(myBestScore.getScore())) {
                                    persistObject(myBestScore);
                                }
                            }
                            setUnsavedHighscoreFlag(false);
                        }
                    }
                }

            });
        } else {
            synchronized (object) {
                todaysScores = getOfflineScores("todays");
                haveTodaysUpdate = true;
            }
        }
    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        synchronized (object) {
            return haveTodaysUpdate;
        }
    }

    @Override
    public boolean haveScoreUpdate() {
        synchronized (object) {
            return haveUpdate;
        }
    }

    @Override
    public boolean internetAvailable() {
        return internetAvailable;
    }

    // offline
    public List<Score> getOfflineScores(String prefix) {
        List<Score> localScores = new ArrayList<Score>();

        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        int highScores = sharedPref.getInt(prefix + TOTALSCORES, 0);

        for (int i = 0; i < highScores; i++) {
            localScores.add(new Score(sharedPref.getString(prefix + USERNAME + i, ""), sharedPref.getInt(prefix + SCORE
                    + i, 0), sharedPref.getInt(prefix + LEVEL + i, 0)));
        }

        return localScores;
    }

    public void persistScores(List<Score> scores, String prefix) {

        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(prefix + TOTALSCORES, scores.size());

        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);

            editor.putInt(prefix + SCORE + i, score.getScore());
            editor.putInt(prefix + LEVEL + i, score.getLevel());
            editor.putString(prefix + USERNAME + i, score.getName());
        }

        editor.commit();
    }

    // offline flag

    private void setUnsavedHighscoreFlag(boolean flag) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(UNSAVED_SCORE, flag);
        editor.commit();
    }

    private boolean getUnsavedHighscoreFlag() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(UNSAVED_SCORE, false);
    }
}
