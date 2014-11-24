package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.InternetConnectionChecker;
import com.mdinic.game.giana.service.Score;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class HighScoreServiceDroid implements HighScoreService {

    private static final String SCORE = "score";
    private static final String LEVEL = "level";
    private static final String USERNAME = "username";

    List<Score> scores = new ArrayList<Score>();

    final Object object = new Object();

    InternetConnectionChecker checker = null;

    @Override
    public void fetchHighScores() {
        if (internetAvailable()) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("GameScore");
            query.setLimit(10);
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
                        synchronized (object) {
                            scores = newScores;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void saveHighScore(Score score) {
        if (internetAvailable()) {
            ParseObject object = new ParseObject("GameScore");
            object.put(USERNAME, score.getName());
            object.put(SCORE, score.getScore());
            object.put(LEVEL, score.getLevel());
            object.put("date", score.getDate().getTime());
            object.saveInBackground();
        } else {
            // TODO save locally
        }
    }

    @Override
    public boolean haveScoreUpdate() {
        synchronized (object) {
            return !scores.isEmpty();
        }
    }

    @Override
    public List<Score> getScoreUpdate() {
        List<Score> result = new ArrayList<Score>(scores.size() + 1);
        synchronized (object) {
            result.addAll(scores);
            scores.clear();
        }
        return result;
    }

    @Override
    public boolean internetAvailable() {
        return checker != null && checker.isAvailableConnection();
    }

    @Override
    public void setIneternetConnectionChecker(InternetConnectionChecker checker) {

        this.checker = checker;
    }
}
