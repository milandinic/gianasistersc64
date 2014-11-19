package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.Score;

public class HighScoreServiceDesktop implements HighScoreService {

    java.util.Map<String, Score> scoresMap = new HashMap<String, Score>();

    public HighScoreServiceDesktop() {
        scoresMap.put("user", new Score("user", 123));
        scoresMap.put("Giana", new Score("Giana", 1260));
        scoresMap.put("Maria", new Score("Maria", 1230));
    }

    @Override
    public List<Score> getHighScores() {
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
        scoresMap.put(score.getName(), score);
    }

}
