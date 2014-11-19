package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
    public List<Score> getHighScores() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("GameScore");
        query.setLimit(10);
        query.addDescendingOrder("score");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    // Log.d("score", "Retrieved " + scoreList.size() +
                    // " scores");
                } else {
                    // Log.d("score", "Error: " + e.getMessage());
                }
            }
        });

        List<Score> scores = new ArrayList<Score>(scoresMap.values());
        Collections.sort(scores, new Comparator<Score>() {

            @Override
            public int compare(Score o1, Score o2) {
                if (o1.getScore() < o2.getScore()) {
                    return -1;
                } else if (o1.getScore() > o2.getScore()) {
                    return 1;
                }
                return 0;
            }
        });
        return scores;
    }

    @Override
    public void saveHighScore(Score score) {
        ParseObject testObject = new ParseObject("GameScore");
        testObject.put(score.getName(), score.getScore());
        testObject.saveInBackground();

        // scoresMap.put(score.getName(), score);
    }

}
