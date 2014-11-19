package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreListener;
import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.Score;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class HighScoreServiceDroid implements HighScoreService {

    java.util.Map<String, Score> scoresMap = new HashMap<String, Score>();

    public HighScoreServiceDroid() {
        scoresMap.put("user", new Score("user", 123));
        scoresMap.put("Giana", new Score("Giana", 1260));
        scoresMap.put("Maria", new Score("Maria", 1230));
    }

    @Override
    public void getHighScores(final HighScoreListener listener) {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("GameScore");
        query.setLimit(10);
        query.addDescendingOrder("score");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    List<Score> scores = new ArrayList<Score>();
                    for (ParseObject parseObject : scoreList) {

                        scores.add(new Score(parseObject.getString("name"), parseObject.getInt("score")));
                    }

                    listener.receiveHighScore(scores);
                } else {
                    // Log.d("score", "Error: " + e.getMessage());
                    listener.receiveHighScore(Collections.EMPTY_LIST);
                }
            }
        });
    }

    @Override
    public void saveHighScore(Score score) {
        ParseObject testObject = new ParseObject("GameScore");
        testObject.put(score.getName(), score.getScore());
        testObject.saveInBackground();

        // scoresMap.put(score.getName(), score);
    }

}
