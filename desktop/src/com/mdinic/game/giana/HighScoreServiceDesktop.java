package com.mdinic.game.giana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mdinic.game.giana.service.HighScoreService;
import com.mdinic.game.giana.service.Score;

public class HighScoreServiceDesktop implements HighScoreService {

    List<Score> scores = new ArrayList<Score>();
    List<Score> todaysScores = new ArrayList<Score>();

    public HighScoreServiceDesktop() {

        scores.add(new Score("user", 123, 1));
        scores.add(new Score("Giana", 1260, 1));
        scores.add(new Score("Maria", 1630, 1));
        scores.add(new Score("Milan", 1530, 1));
        scores.add(new Score("Sanela", 1130, 1));

        todaysScores.add(new Score("Anna", 1220, 1));
        todaysScores.add(new Score("Pera", 1210, 1));
        todaysScores.add(new Score("Vera", 2210, 1));
        todaysScores.add(new Score("root", 4210, 1));
        todaysScores.add(new Score("Mile", 1910, 1));

        Collections.sort(scores, new Comparator<Score>() {

            @Override
            public int compare(Score o1, Score o2) {
                if (o1.getScore() < o2.getScore()) {
                    return 1;
                } else if (o1.getScore() > o2.getScore()) {
                    return -1;
                }
                return 0;
            }
        });
    }

    @Override
    public void fetchHighScores() {

    }

    @Override
    public void saveHighScore(Score score) {
        scores.add(score);
    }

    @Override
    public boolean haveScoreUpdate() {
        return !scores.isEmpty();
    }

    @Override
    public List<Score> getScoreUpdate() {
        List<Score> result = new ArrayList<Score>(scores.size() + 1);
        result.addAll(scores);
        scores.clear();

        return result;
    }

    @Override
    public boolean internetAvailable() {
        return true;
    }

    @Override
    public boolean goodForHighScores(int score) {
        return false;
    }

    @Override
    public List<Score> getTodaysScoreUpdate() {
        List<Score> result = new ArrayList<Score>(todaysScores.size() + 1);
        result.addAll(todaysScores);
        todaysScores.clear();

        return result;
    }

    @Override
    public void fetchTodaysHighScores(boolean saveLocalScoreToWeb) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean haveTodaysScoreUpdate() {
        return !todaysScores.isEmpty();
    }

    @Override
    public Score getMyBest() {
        return new Score("me", 1, 1);
    }
}
