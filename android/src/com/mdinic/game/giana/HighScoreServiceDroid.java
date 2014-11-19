package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.Score;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class HighScoreServiceDroid implements HighScoreService {

    List<Score> scores = new ArrayList<Score>();

    final Object object = new Object();

    @Override
    public void fetchHighScores() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("GameScore");
        query.setLimit(10);
        query.addDescendingOrder("score");

        synchronized (object) {
            scores.clear();
        }

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    List<Score> newScores = new ArrayList<Score>();
                    for (ParseObject parseObject : scoreList) {
                        newScores.add(new Score(parseObject.getString("name"), parseObject.getInt("score")));
                    }
                    synchronized (object) {
                        scores = newScores;
                    }
                }
            }
        });
    }

    @Override
    public void saveHighScore(Score score) {
        ParseObject testObject = new ParseObject("GameScore");
        testObject.put(score.getName(), score.getScore());
        testObject.saveInBackground();
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

}
