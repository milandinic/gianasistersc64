package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreListener;
import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.Score;

public class HighScoreServiceDesktop implements HighScoreService {

    java.util.Map<String, Score> scoresMap = new HashMap<String, Score>();

    public HighScoreServiceDesktop() {
        scoresMap.put("user", new Score("user", 123));
        scoresMap.put("Giana", new Score("Giana", 1260));
        scoresMap.put("Maria", new Score("Maria", 1630));
        scoresMap.put("Milan", new Score("Milan", 1530));
        scoresMap.put("Sanela", new Score("Sanela", 1130));
        scoresMap.put("Anna", new Score("Anna", 1220));
        scoresMap.put("Pera", new Score("Pera", 1210));

        scoresMap.put("Vera", new Score("Vera", 2210));
        scoresMap.put("root", new Score("root", 4210));
        scoresMap.put("Mile", new Score("Mile", 1910));
    }

    @Override
    public void getHighScores(HighScoreListener listener) {
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
        listener.receiveHighScore(scores);
    }

    @Override
    public void saveHighScore(Score score) {
        scoresMap.put(score.getName(), score);
    }

}
